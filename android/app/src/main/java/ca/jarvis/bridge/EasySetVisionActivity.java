package ca.jarvis.bridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.view.Gravity;
import android.view.ViewGroup;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.List;

/** Explicit permission-gated UI for a single screenshot, with manual photo fallback. */
public final class EasySetVisionActivity extends Activity {
    private static final int REQUEST_SCREEN = 3801;
    private static final int REQUEST_IMAGE = 3802;
    private TextView result;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(12, 25, 44));
        getWindow().setNavigationBarColor(Color.rgb(12, 25, 44));
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(10, 23, 40));
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(22), dp(26), dp(22), dp(24));
        scroll.addView(body);

        TextView title = add(body, "JARVIS 1.2.23 • EASYSET", 24, 0xff52dbf8);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        add(body, "ANALYSE VISUELLE — LECTURE SEULE", 15, 0xffc2dce5);
        add(body, "Jarvis peut rechercher les libellés EasySet dans une capture, "
                + "sans autorisation d'accessibilité. "
                + "Les chiffres ne sont pas extraits : vérifie toujours les réglages dans xTool.",
                15, 0xffe5eefa);

        Button capture = new Button(this);
        capture.setText("1 • ANALYSER EASYSET À L'ÉCRAN");
        body.addView(capture);
        capture.setOnClickListener(v -> {
            result.setText("Autorise une capture unique, puis Jarvis reviendra "
                    + "à l'application précédente et analysera l'écran.");
            MediaProjectionManager mgr = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (mgr == null) {
                result.setText("La capture d'écran n'est pas disponible sur cet appareil.");
                return;
            }
            startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_SCREEN);
        });
        add(body, "Conseil : ouvre d'abord xTool sur EasySet, puis utilise le menu "
                + "de la bulle Jarvis pour lancer ce diagnostic. "
                + "Android demandera ton autorisation à chaque capture.",
                13, 0xffb8c9d4);

        Button importButton = new Button(this);
        importButton.setText("2 • ANALYSER UNE CAPTURE DÉJÀ PRISE");
        body.addView(importButton);
        importButton.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.setType("image/*");
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(pick, REQUEST_IMAGE);
        });

        Button refresh = new Button(this);
        refresh.setText("AFFICHER LE DERNIER RÉSULTAT");
        body.addView(refresh);
        refresh.setOnClickListener(v ->
                result.setText(EasySetVisionAnalyzer.last(this)));

        result = add(body, EasySetVisionAnalyzer.last(this), 17, 0xff99f4de);
        result.setPadding(0, dp(12), 0, dp(20));
        add(body, "Aucun clic, aucune saisie et aucune commande de laser. "
                + "Le lancement de fabrication reste entièrement manuel.",
                14, 0xfff5cb9b);
        setContentView(scroll);
    }

    private TextView add(LinearLayout parent, String text, int sizeSp, int color) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextColor(color);
        label.setTextSize(sizeSp);
        label.setPadding(0, dp(10), 0, dp(10));
        parent.addView(label,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        return label;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override protected void onResume() {
        super.onResume();
        if (result != null) result.setText(EasySetVisionAnalyzer.last(this));
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCREEN) {
            if (resultCode != RESULT_OK || data == null) {
                result.setText("Capture annulée. Aucun accès à l'écran accordé.");
                return;
            }
            EasySetVisionAnalyzer.save(this, "Capture autorisée. Retour à xTool pour analyse...");
            Intent service = new Intent(this, EasySetCaptureService.class);
            service.putExtra(EasySetCaptureService.RESULT_CODE, resultCode);
            service.putExtra(EasySetCaptureService.RESULT_INTENT, data);
            try {
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
                else startService(service);
                moveTaskToBack(true);
            } catch (RuntimeException error) {
                result.setText("Capture refusée par Android : " + error.getClass().getSimpleName());
            }
        } else if (requestCode == REQUEST_IMAGE && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            result.setText("Analyse de la capture sélectionnée...");
            Uri uri = data.getData();
            try {
                // Let ML Kit decode the URI; text is processed only in memory.
                var recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                recognizer.process(InputImage.fromFilePath(this, uri))
                        .addOnSuccessListener(vision -> {
                            List<String> lines = new ArrayList<>();
                            for (Text.TextBlock block : vision.getTextBlocks())
                                for (Text.Line line : block.getLines())
                                    lines.add(line.getText());
                            String summary = EasySetLabels.diagnose(lines);
                            EasySetVisionAnalyzer.save(this, summary);
                            result.setText(summary);
                        })
                        .addOnFailureListener(e -> result.setText(
                                "Impossible de lire l'image : " + e.getClass().getSimpleName()))
                        .addOnCompleteListener(task -> recognizer.close());
            } catch (Exception exception) {
                result.setText("Image non prise en charge : "
                        + exception.getClass().getSimpleName());
            }
        }
    }
}
