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
    private long lastOpenRequestMillis;
    private long lastXtoolScanRequestMillis = -EasySetForegroundScanPolicy.MIN_INTERVAL_MS;
    private long lastNamedPanelTapMs = -EasySetPanelTapPolicy.NAMED_TAP_COOLDOWN_MS;
    private long lastUnnamedPanelTapMs = -EasySetPanelTapPolicy.UNNAMED_TAP_COOLDOWN_MS;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        int type = event.getEventType();
        boolean foregroundTransition = type
                == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED;
        boolean contentChanged = type
                == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
        boolean clicked = type == AccessibilityEvent.TYPE_VIEW_CLICKED;
        if (!foregroundTransition && !contentChanged && !clicked) return;

        CharSequence pkg = event.getPackageName();
        if (CHATGPT_PACKAGE.contentEquals(pkg)) {
            if (foregroundTransition) openChatGptBubbleIfNeeded();
        } else if (EasySetForegroundScanPolicy.XCS_PACKAGE.contentEquals(pkg)) {
            // xTool may implement "Volet EasySet" as a custom view whose tap
            // never produces a new Android window. Click events are observed
            // WITHOUT reading the window tree or performing any gestures.
            if (clicked) {
                inspectXtoolPanelTap(event);
                return;
            }
            if (foregroundTransition) {
                // Opening a modal may produce a window-state event rather
                // than a content-change event. While awaiting the panel,
                // accept either event with the shorter one-shot debounce.
                inspectExistingXtoolDrawing(
                        EasySetSessionService.isAwaitingPanelOpen());
            } else if (EasySetSessionService.isAwaitingPanelOpen()) {
                inspectExistingXtoolDrawing(true);
            }
        }
    }

    private void inspectXtoolPanelTap(AccessibilityEvent event) {
        boolean named = EasySetPanelTapPolicy.namesEasySetRow(
                event.getText(), event.getContentDescription());
        boolean hasText = EasySetPanelTapPolicy.hasAnyText(
                event.getText(), event.getContentDescription());
        long now = SystemClock.elapsedRealtime();
        boolean allowed = EasySetPanelTapPolicy.shouldRequest(
                EasySetForegroundScanPolicy.XCS_PACKAGE,
                EasySetSessionService.isActive(),
                JarvisBubbleService.isBubbleActive() && Settings.canDrawOverlays(this),
                named, !hasText,
                EasySetSessionService.needsPanelInspection(),
                now, lastNamedPanelTapMs, lastUnnamedPanelTapMs);
        if (!allowed) return;
        if (named) lastNamedPanelTapMs = now;
        else lastUnnamedPanelTapMs = now;

        // One passive screenshot shortly after the user opens EasySet.
        // This bypasses the older "a previous screenshot must be CLOSED"
        // prerequisite and the prior-success block, but not Android consent.
        EasySetSessionService.requestPanelTapScan(this);
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

    private void inspectExistingXtoolDrawing(boolean panelContentChanged) {
        // The capture service de-duplicates an existing SVG-triggered scan
        // and accepts xTool projects that were already open.
        long now = SystemClock.elapsedRealtime();
        boolean projectionGranted = EasySetSessionService.isActive();
        boolean bubbleVisible = JarvisBubbleService.isBubbleActive()
                && Settings.canDrawOverlays(this);
        boolean allowed = panelContentChanged
                ? EasySetForegroundScanPolicy.shouldRequestAfterPanelChange(
                        EasySetForegroundScanPolicy.XCS_PACKAGE,
                        projectionGranted, bubbleVisible,
                        EasySetSessionService.isAwaitingPanelOpen(),
                        now, lastXtoolScanRequestMillis)
                : EasySetForegroundScanPolicy.shouldRequest(
                        EasySetForegroundScanPolicy.XCS_PACKAGE,
                        projectionGranted, bubbleVisible,
                        now, lastXtoolScanRequestMillis);
        if (!allowed) return;
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
