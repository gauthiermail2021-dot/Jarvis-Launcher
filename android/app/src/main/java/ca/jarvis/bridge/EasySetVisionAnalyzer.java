package ca.jarvis.bridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * On-device, read-only EasySet OCR. Screenshots and raw OCR are kept in memory.
 * The user-visible numeric summary is ephemeral; SharedPreferences persist
 * only the fact that a diagnostic was performed, never its numeric values.
 */
public final class EasySetVisionAnalyzer {
    static final String PREFS = "easyset_vision_local";
    static final String LAST = "last_diagnostic";
    private EasySetVisionAnalyzer() { }

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
                        // The full-screen OCR often misses a tiny isolated '1'
                        // or sees the chevron beside '114' as part of a line.
                        // Only when a parameter is missing, run a second OCR
                        // on an enlarged right strip of the SAME consented
                        // screenshot, without taking another screen capture.
                        if (baseline.startsWith("EasySet ")
                                && (baseline.contains("non lue")
                                        || baseline.contains("non lus"))) {
                            readRightColumn(context, bitmap, base, baseline, callback);
                        } else {
                            finish(context, baseline, callback);
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
                                        String baseline, Callback callback) {
        Bitmap enlarged = null;
        TextRecognizer recognizer = null;
        try {
            // The xTool controls put all editable numbers in the far right
            // of the screen. Re-reading only this area at higher resolution
            // improves small-font OCR without including chat or canvas data.
            int left = Math.round(original.getWidth() * 0.56f);
            int width = original.getWidth() - left;
            int height = original.getHeight();
            if (width < 48 || height < 48) {
                finish(context, baseline, callback);
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
            final float resizeFactor = scale;
            final int offsetX = left;
            detail.process(InputImage.fromBitmap(enlargedImage, 0))
                    .addOnSuccessListener(vision -> {
                        List<EasySetValues.Line> mapped = new ArrayList<>();
                        for (EasySetValues.Line line : EasySetTextExtractor.extract(vision)) {
                            if (line.right <= line.left || line.bottom <= line.top)
                                continue;
                            mapped.add(new EasySetValues.Line(line.text,
                                    offsetX + Math.round(line.left / resizeFactor),
                                    Math.round(line.top / resizeFactor),
                                    offsetX + Math.round(line.right / resizeFactor),
                                    Math.round(line.bottom / resizeFactor)));
                        }
                        finish(context, EasySetValues.diagnose(base, mapped), callback);
                    })
                    .addOnFailureListener(error -> finish(context, baseline, callback))
                    .addOnCompleteListener(task -> {
                        detail.close();
                        if (!enlargedImage.isRecycled()) enlargedImage.recycle();
                    });
        } catch (RuntimeException error) {
            if (recognizer != null) recognizer.close();
            if (enlarged != null && !enlarged.isRecycled()) enlarged.recycle();
            // Enhanced OCR is a best-effort fallback. The baseline is still
            // useful and must not be replaced by a fabricated numeric value.
            finish(context, baseline, callback);
        }
    }

    private static void finish(Context context, String summary, Callback callback) {
        save(context, summary);
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) callback.onDone(summary);
        });
    }
}
