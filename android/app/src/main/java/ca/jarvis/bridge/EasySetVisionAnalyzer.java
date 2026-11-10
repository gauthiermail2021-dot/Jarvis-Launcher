package ca.jarvis.bridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.mlkit.vision.text.TextRecognizer;

import java.util.ArrayList;
import java.util.List;

/** One-shot, on-device OCR: never stores the image or raw recognized screen text. */
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
        final TextRecognizer recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS);
        try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener(vision -> {
                        List<EasySetValues.Line> lines = new ArrayList<>();
                        for (Text.TextBlock block : vision.getTextBlocks()) {
                            for (Text.Line line : block.getLines()) {
                                Rect rect = line.getBoundingBox();
                                if (rect == null) {
                                    lines.add(new EasySetValues.Line(line.getText(), 0, 0, 0, 0));
                                } else {
                                    lines.add(new EasySetValues.Line(line.getText(),
                                            rect.left, rect.top, rect.right, rect.bottom));
                                }
                            }
                        }
                        // Numeric values exist only in the temporary result callback.
                        finish(context, EasySetValues.diagnose(lines), callback);
                    })
                    .addOnFailureListener(e ->
                            finish(context, "Lecture impossible : " + e.getClass().getSimpleName(),
                                    callback))
                    .addOnCompleteListener(task -> {
                        recognizer.close();
                        if (!bitmap.isRecycled()) bitmap.recycle();
                    });
        } catch (RuntimeException problem) {
            recognizer.close();
            if (!bitmap.isRecycled()) bitmap.recycle();
            finish(context, "Analyse non disponible : "
                    + problem.getClass().getSimpleName(), callback);
        }
    }

    private static void finish(Context context, String summary, Callback callback) {
        save(context, summary);
        new Handler(Looper.getMainLooper()).post(() -> {
            if (callback != null) callback.onDone(summary);
        });
    }
}
