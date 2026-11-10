package ca.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

/**
 * Optional, explicitly enabled accessibility service.
 * Observes foreground WINDOW TRANSITIONS only from ChatGPT and xTool XCS.
 * Does not retrieve window content, listen to text, or perform gestures.
 *
 * For xTool XCS: requests ONE passive EasySet analysis when an already-open
 * drawing comes to the foreground, provided Android screen projection was
 * explicitly granted for this Jarvis session. Incoming SVG handoffs continue
 * to use the existing SVG-triggered path; avoid duplicate analyses.
 */
public final class ChatGptBubbleAccessibilityService extends AccessibilityService {
    private static final String CHATGPT_PACKAGE = "com.openai.chatgpt";
    private static final String XTOOL_XCS_PACKAGE = "com.makeblock.xcs";
    private static final long XTOOL_WINDOW_COOLDOWN_MS = 15_000L;
    private long lastOpenRequestMillis;
    private long lastXtoolScanRequestMillis;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return;

        CharSequence pkg = event.getPackageName();
        if (CHATGPT_PACKAGE.contentEquals(pkg)) {
            openChatGptBubbleIfNeeded();
        } else if (XTOOL_XCS_PACKAGE.contentEquals(pkg)) {
            inspectExistingXtoolDrawing();
        }
    }

    private void openChatGptBubbleIfNeeded() {
        if (!Settings.canDrawOverlays(this) || JarvisBubbleService.isBubbleActive()) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastOpenRequestMillis < 2500L) return;
        lastOpenRequestMillis = now;
        try {
            Intent bubble = new Intent(this, JarvisBubbleService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(bubble);
            else startService(bubble);
        } catch (RuntimeException ignored) {
            // Keep the manual button available on Android versions that
            // prohibit background service launches.
        }
    }

    private void inspectExistingXtoolDrawing() {
        if (!Settings.canDrawOverlays(this) || !JarvisBubbleService.isBubbleActive()
                || !EasySetSessionService.isActive()) return;

        // The capture service de-duplicates an existing SVG-triggered scan
        // and accepts xTool projects that were already open.
        long now = SystemClock.elapsedRealtime();
        if (now - lastXtoolScanRequestMillis < XTOOL_WINDOW_COOLDOWN_MS) return;
        lastXtoolScanRequestMillis = now;

        // No screenshot is taken here. The projection service waits briefly
        // for xTool to settle, then analyzes one explicitly authorized frame.
        EasySetSessionService.requestExistingXtoolScan(this);
    }

    @Override
    public void onInterrupt() {
        // No continuous monitoring or screenshot process to interrupt.
    }
}
