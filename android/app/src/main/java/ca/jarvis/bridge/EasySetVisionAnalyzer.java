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
        try {
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
                        baseline, focused, debug, callback);
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
                                passesCrop, debug, callback);
                    })
                    .addOnFailureListener(error -> {
                        debug.columnError();
                        readPassesIfMissing(context, base, new ArrayList<>(), baseline,
                                passesCrop, debug, callback);
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
                    baseline, focused, debug, callback);
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
            FocusedPasses focused,
            EasySetPassesDiagnostic debug, Callback callback) {
        if (focused == null || !interim.startsWith("EasySet ")
                || !interim.contains("Passages : non lus")) {
            debug.cropSkipped(focused == null
                    ? "zone Passer introuvable"
                    : "Passer déjà lu ou EasySet absent");
            if (focused != null) focused.dispose();
            finish(context, debug.append(interim), callback);
            return;
        }
        TextRecognizer recognizer;
        try {
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        } catch (RuntimeException problem) {
            debug.cropError();
            finishWithNumericFallback(context, interim, focused, debug, callback);
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
                                focused, debug, callback);
                    })
                    .addOnFailureListener(error -> {
                        debug.cropError();
                        finishWithNumericFallback(context, interim, focused, debug, callback);
                    })
                    .addOnCompleteListener(task -> {
                        recognizer.close();
                        // Do NOT recycle focused.numericOnly before background Tesseract
                        // has finished; finishWithNumericFallback owns this bitmap.
                    });
        } catch (RuntimeException error) {
            debug.cropError();
            recognizer.close();
            finishWithNumericFallback(context, interim, focused, debug, callback);
        }
    }

    /**
     * Fourth and final OCR stage. It runs only if all ML Kit passes failed.
     * The original screenshot has already been released: the small numeric
     * ROI was cloned ahead of time. Tesseract runs off the main/UI thread.
     *
     * The resulting number is indicative only. No xTool setting is changed,
     * and any failure leaves "Passages : non lus" unchanged.
     */
    private static void finishWithNumericFallback(
            Context context, String summary, FocusedPasses focused,
            EasySetPassesDiagnostic debug, Callback callback) {
        if (focused == null || focused.numericOnly == null
                || !summary.startsWith("EasySet ")
                || !summary.contains("Passages : non lus")) {
            if (focused != null) focused.dispose();
            finish(context, debug.append(summary), callback);
            return;
        }
        try {
            PASS_OCR_WORKER.execute(() -> {
                String corrected = summary;
                String diagnostic = "OCR chiffre seul : non lu";
                EasySetPassesTesseract.Result reading = null;
                try {
                    reading = EasySetPassesTesseract.read(context,
                            focused.numericOnly,
                            new Rect(0, 0, focused.numericOnly.getWidth(),
                                    focused.numericOnly.getHeight()));
                    if (reading.passages != null
                            && reading.passages >= 1 && reading.passages <= 99) {
                        corrected = summary.replace("Passages : non lus",
                                "Passages : " + reading.passages);
                    }
                    diagnostic = "OCR chiffre seul — " + reading.diagnostic;
                } catch (Exception e) {
                    // The installed OCR model might not be available on every device.
                    diagnostic = "OCR chiffre seul indisponible ("
                            + e.getClass().getSimpleName() + ")";
                } finally {
                    if (reading != null && reading.preview != null
                            && !reading.preview.isRecycled()) reading.preview.recycle();
                    focused.dispose();
                }
                finish(context, debug.append(corrected) + "\n" + diagnostic, callback);
            });
        } catch (RuntimeException error) {
            focused.dispose();
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
