package ca.jarvis.bridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Looper;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OCR PASSIF pour la case numérique "Passer" dans xTool EasySet.
 *
 * Le rectangle doit désigner exclusivement la case du nombre de passages
 * (pas toute la rangée, sinon un autre chiffre pourrait être pris à tort).
 * Ne modifie aucun réglage et n'envoie aucune commande à xTool.
 * Appeler depuis un thread d'arrière-plan : Tesseract est bloquant.
 *
 * Pré-requis : dépendance Tesseract4Android 4.9.0 et
 * app/src/main/assets/tessdata/eng.traineddata livré dans l'APK.
 */
public final class EasySetPassesTesseract {
    private EasySetPassesTesseract() { }

    private static final int SCALE = 6;
    private static final int PAD = 72; // 12 original pixels at 6x zoom
    private static final int MAX_WIDTH = 1024;
    private static final int MAX_HEIGHT = 512;
    private static final int MIN_CONFIDENCE = 50;

    public static final class Result {
        /** Valeur indicative uniquement, null si absente / ambiguë. */
        public final Integer passages;
        /** Numeric result for either Passer or Puissance; null if absent. */
        public final Integer number;
        /** Confiance OCR de 0 à 100 ; -1 si aucun résultat. */
        public final int confidence;
        /** Explication destinée à l'affichage de diagnostic. */
        public final String diagnostic;
        /** Zoom du champ réellement analysé ; l'appelant doit recycler ce Bitmap. */
        public final Bitmap preview;

        private Result(Integer passages, int confidence, String diagnostic, Bitmap preview) {
            this.passages = passages;
            this.number = passages;
            this.confidence = confidence;
            this.diagnostic = diagnostic;
            this.preview = preview;
        }
    }

    private static final class Candidate {
        final Integer value;
        final int confidence;
        final String raw;
        final Bitmap preview;

        Candidate(Integer value, int confidence, String raw, Bitmap preview) {
            this.value = value;
            this.confidence = confidence;
            this.raw = raw;
            this.preview = preview;
        }
    }

    /**
     * @param screenshot capture en coordonnées pixels (ne sera pas modifiée)
     * @param numberBox coordonnées EXACTES de la zone numérique (pas du libellé)
     */
    public static Result read(Context context, Bitmap screenshot, Rect numberBox)
            throws IOException {
        return readNumber(context, screenshot, numberBox, 0);
    }

    /**
     * Separate read-only Power field recognition. Uses the same verified
     * Tesseract preprocessing but accepts 0..100 and does not reuse the
     * Passer value or overwrite power detected by ML Kit.
     */
    public static Result readPower(Context context, Bitmap screenshot, Rect numberBox)
            throws IOException {
        return readNumber(context, screenshot, numberBox, 1);
    }

    /**
     * Only the isolated speed numeric field; never a slider, canvas
     * dimension or unrelated value. Valid speed range: 1..3000.
     */
    public static Result readSpeed(Context context, Bitmap screenshot, Rect numberBox)
            throws IOException {
        return readNumber(context, screenshot, numberBox, 2);
    }

    // Mode 0 Passer, 1 Puissance, 2 Vitesse; all read-only.
    private static Result readNumber(Context context, Bitmap screenshot,
                                     Rect numberBox, int fieldMode)
            throws IOException {
        final String field = fieldMode == 1 ? "Puissance"
                : fieldMode == 2 ? "Vitesse" : "Passer";
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("OCR Tesseract à lancer hors du thread UI");
        }
        if (context == null || screenshot == null || screenshot.isRecycled() || numberBox == null) {
            throw new IllegalArgumentException("Capture et rectangle requis");
        }
        Rect bounds = new Rect(0, 0, screenshot.getWidth(), screenshot.getHeight());
        Rect roi = new Rect(numberBox);
        if (!roi.intersect(bounds) || roi.width() < 3 || roi.height() < 3) {
            return new Result(null, -1, "Zone " + field + " vide ou hors écran", null);
        }
        // Called from Jarvis with a cloned *numeric-only* screenshot; its
        // ROI legitimately covers 100 % of that small Bitmap. Reject a broad
        // screen crop, but do not reject the verified small numeric field.
        if (!acceptsRegion(screenshot.getWidth(), screenshot.getHeight(),
                roi.left, roi.top, roi.right, roi.bottom)) {
            return new Result(null, -1, "Zone trop large : cadrer seulement le chiffre " + field, null);
        }

        File tessRoot = ensureLanguageModel(context.getApplicationContext());
        Bitmap cropped = Bitmap.createBitmap(screenshot, roi.left, roi.top, roi.width(), roi.height());
        List<Candidate> candidates = new ArrayList<>();
        TessBaseAPI api = new TessBaseAPI();
        try {
            if (!api.init(tessRoot.getAbsolutePath() + File.separator, "eng")) {
                throw new IOException("Tesseract : modèle eng non initialisé");
            }
            api.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");
            api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_WORD);
            // xTool may draw a 1 as a thin stroke with NO lower serif.
            // Tesseract single-character PSM10 often discards that glyph.
            // PSM8 (single word) on the *isolated ink* was verified locally
            // on the supplied EasySet screenshot; Android remains unverified.
            int background = (Color.red(cropped.getPixel(0, 0))
                    + Color.green(cropped.getPixel(0, 0))
                    + Color.blue(cropped.getPixel(0, 0))) / 3;
            boolean whiteText = background < 128;
            for (int threshold : new int[]{120, 165}) {
                Rect inkBounds = findInkBounds(cropped, threshold, whiteText);
                if (inkBounds == null) continue;
                Bitmap ink = Bitmap.createBitmap(cropped, inkBounds.left, inkBounds.top,
                        inkBounds.width(), inkBounds.height());
                try {
                    Bitmap prepared = prepare(ink, threshold, whiteText);
                    try {
                        api.setImage(prepared);
                        String raw = api.getUTF8Text();
                        int confidence = api.meanConfidence();
                        String normalized = raw == null ? "" : raw.trim().replaceAll("\\s+", "");
                        Integer value = null;
                        value = acceptedNumber(normalized, confidence, fieldMode);
                        candidates.add(new Candidate(value, confidence, normalized, prepared));
                        api.clear();
                    } catch (RuntimeException e) {
                        prepared.recycle();
                        throw e;
                    }
                } finally {
                    ink.recycle();
                }
            }
        } finally {
            cropped.recycle();
            api.recycle();
        }

        Candidate best = null;
        boolean disagreement = false;
        for (Candidate c : candidates) {
            if (c.value == null) continue;
            if (best != null && !c.value.equals(best.value)) disagreement = true;
            if (best == null || c.confidence > best.confidence) best = c;
        }
        Bitmap preview = best == null ? (candidates.isEmpty() ? null : candidates.get(0).preview)
                                      : best.preview;
        for (Candidate c : candidates) {
            if (c.preview != preview) c.preview.recycle();
        }
        if (disagreement) {
            return new Result(null, -1, field + " ambigu : deux lectures contradictoires", preview);
        }
        if (best == null) {
            StringBuilder raw = new StringBuilder("Aucun chiffre fiable : ");
            for (Candidate c : candidates) {
                raw.append('[').append(c.raw).append(" ; ").append(c.confidence).append("%] ");
            }
            return new Result(null, -1, raw.toString().trim(), preview);
        }
        return new Result(best.value, best.confidence,
                String.format(Locale.CANADA_FRENCH,
                        "%s : %d (Tesseract, confiance %d %%, indicatif)",
                        field, best.value, best.confidence), preview);
    }

    /**
     * JVM-testable strict numeric validation for the three separate cells.
     * Rejects leading zeros and impossible values instead of guessing.
     */
    static Integer acceptedNumber(String raw, int confidence, int mode) {
        if (raw == null || confidence < MIN_CONFIDENCE) return null;
        final int max = mode == 1 ? 100 : mode == 2 ? 3000 : 99;
        final int min = mode == 1 ? 0 : 1;
        if (!raw.matches(mode == 1 ? "(?:0|[1-9][0-9]{0,2})"
                : mode == 2 ? "[1-9][0-9]{0,3}"
                : "[1-9][0-9]?")) return null;
        int parsed;
        try { parsed = Integer.parseInt(raw); }
        catch (NumberFormatException ignored) { return null; }
        return parsed >= min && parsed <= max ? parsed : null;
    }

    /**
     * Exclude xTool's light-gray spinner arrow and white margins. A single
     * narrow digit must be kept, not discarded as OCR noise.
     */
    /**
     * JVM-testable ROI guard. It prevents an arbitrary full-screen OCR but
     * accepts a small isolated field copied from an already located label.
     */
    static boolean acceptsRegion(int screenWidth, int screenHeight,
                                 int left, int top, int right, int bottom) {
        int w = right - left, h = bottom - top;
        if (screenWidth < 3 || screenHeight < 3 || left < 0 || top < 0
                || right > screenWidth || bottom > screenHeight
                || w < 3 || h < 3) return false;
        boolean smallIsolatedField = left == 0 && top == 0
                && w == screenWidth && h == screenHeight
                && screenWidth <= 180 && screenHeight <= 128;
        return smallIsolatedField
                || (w <= screenWidth / 2 && h <= screenHeight / 4);
    }

    private static Rect findInkBounds(Bitmap source, int threshold, boolean whiteText) {
        int w = source.getWidth(), h = source.getHeight();
        int[] pixels = new int[w * h];
        source.getPixels(pixels, 0, w, 0, 0, w, h);
        int left = w, top = h, right = -1, bottom = -1, count = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = pixels[y * w + x];
                int lum = (299 * Color.red(rgb) + 587 * Color.green(rgb)
                        + 114 * Color.blue(rgb)) / 1000;
                boolean ink = whiteText ? lum >= 255 - threshold : lum <= threshold;
                if (!ink) continue;
                count++;
                if (x < left) left = x;
                if (x > right) right = x;
                if (y < top) top = y;
                if (y > bottom) bottom = y;
            }
        }
        if (count < 12 || right - left + 1 < 2 || bottom - top + 1 < 8)
            return null;
        // An entire dark or white rectangle means no isolated character.
        if ((right - left + 1) >= w * 0.94f
                && (bottom - top + 1) >= h * 0.94f) return null;
        return new Rect(left, top, right + 1, bottom + 1);
    }

    private static Bitmap prepare(Bitmap source, int threshold, boolean lightCharacters) {
        int w = Math.min(MAX_WIDTH, source.getWidth() * SCALE);
        int h = Math.min(MAX_HEIGHT, source.getHeight() * SCALE);
        Bitmap result = Bitmap.createBitmap(w + PAD * 2, h + PAD * 2,
                Bitmap.Config.ARGB_8888);
        result.eraseColor(Color.WHITE);
        int sw = source.getWidth();
        int sh = source.getHeight();
        int[] px = new int[sw * sh];
        source.getPixels(px, 0, sw, 0, 0, sw, sh);
        int[] row = new int[w];
        for (int y = 0; y < h; y++) {
            int sy = Math.min(sh - 1, (int)((long)y * sh / h));
            for (int x = 0; x < w; x++) {
                int sx = Math.min(sw - 1, (int)((long)x * sw / w));
                int rgb = px[sy * sw + sx];
                int luma = (299 * Color.red(rgb) + 587 * Color.green(rgb)
                        + 114 * Color.blue(rgb)) / 1000;
                boolean foreground = lightCharacters ? luma >= threshold : luma <= threshold;
                row[x] = foreground ? Color.BLACK : Color.WHITE;
            }
            result.setPixels(row, 0, w, PAD, PAD + y, w, 1);
        }
        return result;
    }

    private static int otsuThreshold(Bitmap source) {
        int w = source.getWidth(), h = source.getHeight();
        int[] px = new int[w * h];
        source.getPixels(px, 0, w, 0, 0, w, h);
        int[] hist = new int[256];
        long sum = 0;
        for (int rgb : px) {
            int l = (299 * Color.red(rgb) + 587 * Color.green(rgb)
                    + 114 * Color.blue(rgb)) / 1000;
            hist[l]++;
            sum += l;
        }
        long sumBack = 0;
        int weightBack = 0;
        double bestVariance = -1;
        int best = 127;
        for (int t = 0; t < 256; t++) {
            weightBack += hist[t];
            if (weightBack == 0) continue;
            int weightFront = px.length - weightBack;
            if (weightFront == 0) break;
            sumBack += (long)t * hist[t];
            double meanBack = (double)sumBack / weightBack;
            double meanFront = (double)(sum - sumBack) / weightFront;
            double variance = (double)weightBack * weightFront
                    * (meanBack - meanFront) * (meanBack - meanFront);
            if (variance > bestVariance) {
                bestVariance = variance;
                best = t;
            }
        }
        return best;
    }

    private static File ensureLanguageModel(Context context) throws IOException {
        File root = new File(context.getFilesDir(), "tesseract");
        File tess = new File(root, "tessdata");
        if (!tess.exists() && !tess.mkdirs()) {
            throw new IOException("Impossible de créer le dossier tessdata");
        }
        File target = new File(tess, "eng.traineddata");
        if (target.length() > 100_000) return root;
        File temporary = new File(tess, "eng.traineddata.tmp");
        try (InputStream in = context.getAssets().open("tessdata/eng.traineddata");
             OutputStream out = new FileOutputStream(temporary)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) >= 0) {
                out.write(buf, 0, n);
            }
        } catch (IOException e) {
            temporary.delete();
            throw new IOException("Modèle tessdata/eng.traineddata absent de l'APK", e);
        }
        if (temporary.length() <= 100_000 || !temporary.renameTo(target)) {
            temporary.delete();
            throw new IOException("Modèle Tesseract corrompu ou impossible à installer");
        }
        return root;
    }
}
