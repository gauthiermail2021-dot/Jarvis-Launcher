package ca.jarvis.bridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * On-device, read-only EasySet OCR. Screenshots and raw OCR are kept in memory.
 * The user-visible numeric summary is ephemeral; SharedPreferences persist
 * only the fact that a diagnostic was performed, never its numeric values.
 */
public final class EasySetVisionAnalyzer {
    static final String PREFS = "easyset_vision_local";
    static final String LAST = "last_diagnostic";
    private EasySetVisionAnalyzer() { }
    private static final ExecutorService PASS_OCR_WORKER = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onDone(String summary);
    }

    public static String last(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(LAST, "Aucune analyse visuelle effectuée.");
    }

    public static void save(Context context, String result) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(LAST, result != null
                        && result.contains("Valeurs OCR indicatives")
                        ? "EasySet analysé. Les valeurs numériques ont été affichées "
                        + "temporairement et ne sont pas conservées. "
                        + "Relance une analyse pour les revoir."
                        : result).apply();
    }

    public static void analyze(Context context, Bitmap bitmap, Callback callback) {
        if (bitmap == null) {
            finish(context, "Échec : capture d'écran vide.", callback);
            return;
        }
        final TextRecognizer recognizer;
        try {
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        } catch (RuntimeException error) {
            bitmap.recycle();
            finish(context, "Reconnaissance indisponible : "
                    + error.getClass().getSimpleName(), callback);
            return;
        }
        try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener(vision -> {
                        List<EasySetValues.Line> base = EasySetTextExtractor.extract(vision);
                        String baseline = EasySetValues.diagnose(base);
                        EasySetPassesDiagnostic debug = new EasySetPassesDiagnostic(base);
                        // The full-screen OCR often misses a tiny isolated '1'
                        // or sees the chevron beside '114' as part of a line.
                        // Only when a parameter is missing, run a second OCR
                        // on an enlarged right strip of the SAME consented
                        // screenshot, without taking another screen capture.
                        if (baseline.startsWith("EasySet ")
                                && (baseline.contains("non lue")
                                        || baseline.contains("non lus"))) {
                            readRightColumn(context, bitmap, base, baseline, debug, callback);
                        } else {
                            if (baseline.startsWith("EasySet ")) {
                                debug.columnSkipped("aucune valeur manquante");
                                debug.cropSkipped("valeur Passer déjà disponible");
                            }
                            finish(context, debug.append(baseline), callback);
                        }
                    })
                    .addOnFailureListener(error -> finish(context,
                            "Lecture impossible : " + error.getClass().getSimpleName(),
                            callback))
                    .addOnCompleteListener(task -> {
                        recognizer.close();
                        if (!bitmap.isRecycled()) bitmap.recycle();
                    });
        } catch (RuntimeException error) {
            recognizer.close();
            if (!bitmap.isRecycled()) bitmap.recycle();
            finish(context, "Analyse non disponible : "
                    + error.getClass().getSimpleName(), callback);
        }
    }

    private static void readRightColumn(Context context, Bitmap original,
                                        List<EasySetValues.Line> base,
                                        String baseline,
                                        EasySetPassesDiagnostic debug,
                                        Callback callback) {
        Bitmap enlarged = null;
        TextRecognizer recognizer = null;
        FocusedPasses focused = null;
        Bitmap powerNumeric = null;
        Bitmap speedNumeric = null;
        try {
            // Capture the isolated Speed number before Android recycles
            // the original screen bitmap after ML Kit finishes.
            if (baseline.contains("Vitesse : non lue")) {
                EasySetSpeedRegion sr = EasySetSpeedRegion.find(
                        base, original.getWidth(), original.getHeight());
                if (sr != null) {
                    speedNumeric = Bitmap.createBitmap(original,
                            sr.left, sr.top, sr.width(), sr.height());
                }
            }
            // Clone only the power number (not the slider or the chevron),
            // while the original consented screenshot is still available.
            if (baseline.contains("Puissance : non lue")) {
                EasySetPowerRegion powerRegion = EasySetPowerRegion.find(
                        base, original.getWidth(), original.getHeight());
                if (powerRegion != null) {
                    powerNumeric = Bitmap.createBitmap(original,
                            powerRegion.left, powerRegion.top,
                            powerRegion.width(), powerRegion.height());
                }
            }
            // Build a third-pass crop NOW, before the first recognizer's
            // completion listener recycles the source screen bitmap.
            // It is used only if the enlarged right-column OCR still fails.
            if (baseline.contains("Passages : non lus")) {
                focused = prepareFocusedPasses(original, base);
            }

            // Existing 1.2.29 workflow: read the right column at larger size.
            int left = Math.round(original.getWidth() * 0.56f);
            int width = original.getWidth() - left;
            int height = original.getHeight();
            if (width < 48 || height < 48) {
                debug.columnSkipped("capture trop petite");
                readPassesIfMissing(context, base, new ArrayList<>(),
                        baseline, focused, powerNumeric, speedNumeric, debug, callback);
                return;
            }
            Bitmap cropped = Bitmap.createBitmap(original, left, 0, width, height);
            float scale = Math.max(1f, Math.min(2f, 3000f / height));
            enlarged = Bitmap.createScaledBitmap(cropped,
                    Math.max(1, Math.round(width * scale)),
                    Math.max(1, Math.round(height * scale)), true);
            if (enlarged != cropped) cropped.recycle();

            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            final TextRecognizer detail = recognizer;
            final Bitmap enlargedImage = enlarged;
            final FocusedPasses passesCrop = focused;
            final Bitmap powerCrop = powerNumeric;
            final Bitmap speedCrop = speedNumeric;
            final float resizeFactor = scale;
            final int offsetX = left;
            detail.process(InputImage.fromBitmap(enlargedImage, 0))
                    .addOnSuccessListener(vision -> {
                        List<EasySetValues.Line> rightCandidates = new ArrayList<>();
                        for (EasySetValues.Line line : EasySetTextExtractor.extract(vision)) {
                            if (line.right <= line.left || line.bottom <= line.top)
                                continue;
                            rightCandidates.add(new EasySetValues.Line(line.text,
                                    offsetX + Math.round(line.left / resizeFactor),
                                    Math.round(line.top / resizeFactor),
                                    offsetX + Math.round(line.right / resizeFactor),
                                    Math.round(line.bottom / resizeFactor)));
                        }
                        debug.readColumn(rightCandidates);
                        String interim = EasySetValues.diagnose(base, rightCandidates);
                        readPassesIfMissing(context, base, rightCandidates, interim,
                                passesCrop, powerCrop, speedCrop, debug, callback);
                    })
                    .addOnFailureListener(error -> {
                        debug.columnError();
                        readPassesIfMissing(context, base, new ArrayList<>(), baseline,
                                passesCrop, powerCrop, speedCrop, debug, callback);
                    })
                    .addOnCompleteListener(task -> {
                        detail.close();
                        if (!enlargedImage.isRecycled()) enlargedImage.recycle();
                    });
        } catch (RuntimeException error) {
            if (recognizer != null) recognizer.close();
            if (enlarged != null && !enlarged.isRecycled()) enlarged.recycle();
            debug.columnError();
            readPassesIfMissing(context, base, new ArrayList<>(),
                    baseline, focused, powerNumeric, speedNumeric, debug, callback);
        }
    }

    /**
     * A targeted OCR pass for the single digit beside "Passer" only.
     * It uses an enlarged, contrast-boosted Passer ROW, including its
     * field label for textual context, and does not require new permission.
     */
    private static FocusedPasses prepareFocusedPasses(Bitmap source,
                                                       List<EasySetValues.Line> base) {
        EasySetPassesRegion region = EasySetPassesRegion.find(
                base, source.getWidth(), source.getHeight());
        if (region == null) return null;
        Bitmap slice = null;
        Bitmap resized = null;
        Bitmap numericOnly = null;
        try {
            // Clone the numeric field before the full screenshot is recycled
            // by the asynchronous screen OCR completion callback.
            EasySetPassesRegion box = region.numberField(
                    source.getWidth(), source.getHeight());
            if (box != null) {
                numericOnly = Bitmap.createBitmap(source,
                        box.left, box.top, box.width(), box.height());
            }
            slice = Bitmap.createBitmap(source,
                    region.left, region.top, region.width(), region.height());
            float factor = Math.min(3.5f, 2900f / region.width());
            factor = Math.max(1f, factor);
            resized = Bitmap.createScaledBitmap(slice,
                    Math.round(slice.getWidth() * factor),
                    Math.round(slice.getHeight() * factor), true);
            if (resized != slice) slice.recycle();
            slice = null;

            // Brightens light-gray UI elements while retaining black digit strokes.
            android.graphics.ColorMatrix adjust = new android.graphics.ColorMatrix(
                    new float[] {
                            1.35f,0,0,0,-32f,
                            0,1.35f,0,0,-32f,
                            0,0,1.35f,0,-32f,
                            0,0,0,1,0
                    });
            Bitmap enhanced = Bitmap.createBitmap(
                    resized.getWidth(), resized.getHeight(), Bitmap.Config.ARGB_8888);
            android.graphics.Paint paint = new android.graphics.Paint(
                    android.graphics.Paint.ANTI_ALIAS_FLAG
                            | android.graphics.Paint.FILTER_BITMAP_FLAG);
            paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(adjust));
            new android.graphics.Canvas(enhanced).drawBitmap(resized, 0, 0, paint);
            resized.recycle();
            return new FocusedPasses(enhanced, region, factor, numericOnly);
        } catch (RuntimeException ignored) {
            if (slice != null && !slice.isRecycled()) slice.recycle();
            if (resized != null && !resized.isRecycled()) resized.recycle();
            if (numericOnly != null && !numericOnly.isRecycled()) numericOnly.recycle();
            return null;
        }
    }

    private static final class FocusedPasses {
        final Bitmap image;
        final EasySetPassesRegion region;
        final float scale;
        final Bitmap numericOnly;

        FocusedPasses(Bitmap image, EasySetPassesRegion region, float scale,
                      Bitmap numericOnly) {
            this.image = image;
            this.region = region;
            this.scale = scale;
            this.numericOnly = numericOnly;
        }

        void dispose() {
            if (!image.isRecycled()) image.recycle();
            if (numericOnly != null && !numericOnly.isRecycled()) numericOnly.recycle();
        }
    }

    private static void readPassesIfMissing(
            Context context, List<EasySetValues.Line> base,
            List<EasySetValues.Line> rightCandidates, String interim,
            FocusedPasses focused, Bitmap powerNumeric, Bitmap speedNumeric,
            EasySetPassesDiagnostic debug, Callback callback) {
        if (focused == null || !interim.startsWith("EasySet ")
                || !interim.contains("Passages : non lus")) {
            debug.cropSkipped(focused == null
                    ? "zone Passer introuvable"
                    : "Passer déjà lu ou EasySet absent");
            // A Passer crop may be unavailable or unnecessary, but power
            // still needs its own targeted numeric fallback.
            finishWithNumericFallback(context, interim, focused, powerNumeric, speedNumeric,
                    debug, callback);
            return;
        }
        TextRecognizer recognizer;
        try {
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        } catch (RuntimeException problem) {
            debug.cropError();
            finishWithNumericFallback(context, interim, focused, powerNumeric, speedNumeric,
                    debug, callback);
            return;
        }
        try {
            recognizer.process(InputImage.fromBitmap(focused.image, 0))
                    .addOnSuccessListener(vision -> {
                        List<EasySetValues.Line> supplement = new ArrayList<>(rightCandidates);
                        for (EasySetValues.Line line : EasySetTextExtractor.extract(vision)) {
                            if (line.right <= line.left || line.bottom <= line.top)
                                continue;
                            supplement.add(new EasySetValues.Line(line.text,
                                    focused.region.left + Math.round(line.left / focused.scale),
                                    focused.region.top + Math.round(line.top / focused.scale),
                                    focused.region.left + Math.round(line.right / focused.scale),
                                    focused.region.top + Math.round(line.bottom / focused.scale)));
                        }
                        List<EasySetValues.Line> focusedCandidates =
                                new ArrayList<>(supplement.subList(
                                        rightCandidates.size(), supplement.size()));
                        debug.readCrop(focusedCandidates);
                        finishWithNumericFallback(context,
                                EasySetValues.diagnose(base, supplement),
                                focused, powerNumeric, speedNumeric, debug, callback);
                    })
                    .addOnFailureListener(error -> {
                        debug.cropError();
                        finishWithNumericFallback(context, interim, focused, powerNumeric, speedNumeric,
                    debug, callback);
                    })
                    .addOnCompleteListener(task -> {
                        recognizer.close();
                        // Do NOT recycle focused.numericOnly before background Tesseract
                        // has finished; finishWithNumericFallback owns this bitmap.
                    });
        } catch (RuntimeException error) {
            debug.cropError();
            recognizer.close();
            finishWithNumericFallback(context, interim, focused, powerNumeric, speedNumeric,
                    debug, callback);
        }
    }

    /**
     * Bounded numeric-only OCR for Power, Speed and Passer. Each region is tied to
     * its detected field label; no number is borrowed from a neighboring row.
     * Never overwrite a value already read by ML Kit.
     *
     * This is a passive, indicative display. No xTool controls are changed.
     * All bitmaps are freed on both success and failure.
     */
    private static void finishWithNumericFallback(
            Context context, String summary, FocusedPasses focused,
            Bitmap powerNumeric, Bitmap speedNumeric, EasySetPassesDiagnostic debug,
            Callback callback) {
        boolean needsPower = summary.startsWith("EasySet ")
                && summary.contains("Puissance : non lue")
                && powerNumeric != null;
        boolean needsSpeed = summary.startsWith("EasySet ")
                && summary.contains("Vitesse : non lue")
                && speedNumeric != null;
        boolean needsPasses = summary.startsWith("EasySet ")
                && summary.contains("Passages : non lus")
                && focused != null && focused.numericOnly != null;
        if (!needsPower && !needsSpeed && !needsPasses) {
            if (focused != null) focused.dispose();
            if (powerNumeric != null && !powerNumeric.isRecycled())
                powerNumeric.recycle();
            if (speedNumeric != null && !speedNumeric.isRecycled())
                speedNumeric.recycle();
            finish(context, debug.append(summary), callback);
            return;
        }
        try {
            PASS_OCR_WORKER.execute(() -> {
                String corrected = summary;
                String powerDiagnostic = "";
                String speedDiagnostic = "";
                String passesDiagnostic = "";
                EasySetPassesTesseract.Result powerReading = null;
                EasySetPassesTesseract.Result speedReading = null;
                EasySetPassesTesseract.Result passesReading = null;
                try {
                    if (needsPower) {
                        try {
                            powerReading = EasySetPassesTesseract.readPower(
                                    context, powerNumeric,
                                    new Rect(0, 0, powerNumeric.getWidth(),
                                            powerNumeric.getHeight()));
                            if (powerReading.number != null
                                    && powerReading.number >= 0
                                    && powerReading.number <= 100) {
                                corrected = corrected.replace("Puissance : non lue",
                                        "Puissance : " + powerReading.number + " %");
                            }
                            powerDiagnostic = "\nOCR chiffre seul — "
                                    + powerReading.diagnostic;
                        } catch (Exception e) {
                            powerDiagnostic = "\nOCR Puissance indisponible ("
                                    + e.getClass().getSimpleName() + ")";
                        }
                    }
                    if (needsSpeed) {
                        try {
                            speedReading = EasySetPassesTesseract.readSpeed(
                                    context, speedNumeric,
                                    new Rect(0, 0, speedNumeric.getWidth(),
                                            speedNumeric.getHeight()));
                            if (speedReading.number != null
                                    && speedReading.number >= 1
                                    && speedReading.number <= 3000) {
                                corrected = corrected.replace("Vitesse : non lue",
                                        "Vitesse : " + speedReading.number + " mm/s");
                            }
                            speedDiagnostic = "\nOCR chiffre seul — "
                                    + speedReading.diagnostic;
                        } catch (Exception error) {
                            speedDiagnostic = "\nOCR Vitesse indisponible ("
                                    + error.getClass().getSimpleName() + ")";
                        }
                    }
                    if (needsPasses) {
                        try {
                            passesReading = EasySetPassesTesseract.read(
                                    context, focused.numericOnly,
                                    new Rect(0, 0, focused.numericOnly.getWidth(),
                                            focused.numericOnly.getHeight()));
                            if (passesReading.passages != null
                                    && passesReading.passages >= 1
                                    && passesReading.passages <= 99) {
                                corrected = corrected.replace("Passages : non lus",
                                        "Passages : " + passesReading.passages);
                            }
                            passesDiagnostic = "\nOCR chiffre seul — "
                                    + passesReading.diagnostic;
                        } catch (Exception e) {
                            passesDiagnostic = "\nOCR Passer indisponible ("
                                    + e.getClass().getSimpleName() + ")";
                        }
                    }
                } finally {
                    if (powerReading != null && powerReading.preview != null
                            && !powerReading.preview.isRecycled())
                        powerReading.preview.recycle();
                    if (speedReading != null && speedReading.preview != null
                            && !speedReading.preview.isRecycled())
                        speedReading.preview.recycle();
                    if (passesReading != null && passesReading.preview != null
                            && !passesReading.preview.isRecycled())
                        passesReading.preview.recycle();
                    if (focused != null) focused.dispose();
                    if (powerNumeric != null && !powerNumeric.isRecycled())
                        powerNumeric.recycle();
                    if (speedNumeric != null && !speedNumeric.isRecycled())
                        speedNumeric.recycle();
                }
                finish(context, debug.append(corrected)
                        + powerDiagnostic + speedDiagnostic + passesDiagnostic, callback);
            });
        } catch (RuntimeException error) {
            if (focused != null) focused.dispose();
            if (powerNumeric != null && !powerNumeric.isRecycled())
                powerNumeric.recycle();
            if (speedNumeric != null && !speedNumeric.isRecycled())
                speedNumeric.recycle();
            finish(context, debug.append(summary)
                    + "\nOCR chiffre seul : indisponible", callback);
        }
    }

    private static void finish(Context context, String summary, Callback callback) {
        save(context, summary);
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) callback.onDone(summary);
        });
    }
}
