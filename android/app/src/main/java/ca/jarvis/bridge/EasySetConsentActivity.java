package ca.jarvis.bridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Small, transparent one-shot permission broker launched from the Jarvis bubble.
 *
 * No diagnostic screen is drawn. This Activity runs in an independent,
 * temporary task so that Jarvis MainActivity does not rise in front of xTool.
 * The operating system still shows its own mandatory screen-capture consent.
 */
public final class EasySetConsentActivity extends Activity {
    private static final int REQUEST_CAPTURE_CONSENT = 3810;
    private boolean pendingConsent = false;
    private boolean processedResult = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        if (savedInstanceState != null) {
            pendingConsent = savedInstanceState.getBoolean("pending_consent", false);
            processedResult = savedInstanceState.getBoolean("processed_result", false);
        }
        if (!pendingConsent && !processedResult) {
            askCaptureConsent();
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("pending_consent", pendingConsent);
        state.putBoolean("processed_result", processedResult);
        super.onSaveInstanceState(state);
    }

    private void askCaptureConsent() {
        MediaProjectionManager manager = (MediaProjectionManager)
                getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (manager == null) {
            explainAndReturn("Capture d'écran indisponible.");
            return;
        }
        try {
            pendingConsent = true;
            startActivityForResult(manager.createScreenCaptureIntent(),
                    REQUEST_CAPTURE_CONSENT);
        } catch (RuntimeException e) {
            pendingConsent = false;
            explainAndReturn("Autorisation Android indisponible.");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_CAPTURE_CONSENT || processedResult) return;
        processedResult = true;
        pendingConsent = false;
        if (resultCode == RESULT_OK && data != null) {
            Intent capture = new Intent(this, EasySetCaptureService.class);
            capture.putExtra(EasySetCaptureService.RESULT_CODE, resultCode);
            capture.putExtra(EasySetCaptureService.RESULT_INTENT, data);
            try {
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(capture);
                else startService(capture);
                JarvisBubbleService.prepareEasySetCapture();
            } catch (RuntimeException error) {
                explainAndReturn("Capture Android refusée : "
                        + error.getClass().getSimpleName());
                return;
            }
        } else {
            Toast.makeText(this, "Capture annulée.", Toast.LENGTH_SHORT).show();
        }
        returnToPriorApp();
    }

    private void explainAndReturn(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        returnToPriorApp();
    }

    private void returnToPriorApp() {
        // The dedicated consent task, not the Jarvis settings task, goes away.
        // The previously foregrounded xTool window should be revealed.
        try { moveTaskToBack(true); } catch (RuntimeException ignored) { }
        finish();
        overridePendingTransition(0, 0);
    }
}
