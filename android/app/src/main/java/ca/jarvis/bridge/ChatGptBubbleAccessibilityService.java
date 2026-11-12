package ca.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional, explicitly enabled accessibility service.
 * Optionally observes UI labels in the foreground xTool accessibility tree,
 * ONLY while the named xTool app is active. It never performs gestures or
 * changes an editable value. Read label snapshots are never persisted.
 * The existing Android screen capture requires separate consent.
 */
public final class ChatGptBubbleAccessibilityService extends AccessibilityService {
    private static final String CHATGPT_PACKAGE = "com.openai.chatgpt";
    // These diagnostics contain ONLY connection state, timings and counts,
    // never window text, numbers, screenshots or user-specific app contents.
    private static volatile boolean accessibilityConnected;
    private static volatile long lastXtoolEventAt;
    private static volatile int lastXtoolEventType;
    private static volatile String lastXtoolTreeStatus = "Aucune inspection";
    private static volatile int lastXtoolNodes;
    private static volatile int lastEasySetHeadings;
    // Only short EasySet headings are retained in memory for diagnostics,
    // preserving duplicates/source: never collect numeric control values.
    private static volatile String lastEasySetExactLabels = "Aucun libellé relevé";
    private static volatile long lastXtoolTreeAt;

    static String easySetConnectionDiagnostic() {
        long now = SystemClock.elapsedRealtime();
        String event = lastXtoolEventAt == 0L ? "aucun"
                : ((now - lastXtoolEventAt) / 1000L) + " s"
                + " (" + AccessibilityEvent.eventTypeToString(lastXtoolEventType) + ")";
        String tree = lastXtoolTreeAt == 0L ? "non inspectée"
                : lastXtoolTreeStatus + " (" + (now - lastXtoolTreeAt) / 1000L
                + " s, " + lastXtoolNodes + " nœuds, "
                + lastEasySetHeadings + " libellés)";
        return "DIAGNOSTIC EASYSET"
                + "\nAccessibilité Jarvis : " + (accessibilityConnected ? "active" : "inactive")
                + "\nCapture autorisée : " + (EasySetSessionService.isActive() ? "oui" : "non")
                + "\nDernier événement xTool : " + event
                + "\nFenêtre inspectée : " + tree
                + "\nLibellés exacts (avec doublons) :\n" + lastEasySetExactLabels
                + "\n" + EasySetSessionService.panelFlowDiagnostic()
                + "\nAucun paramètre modifié.";
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        accessibilityConnected = true;
        lastXtoolEventAt = 0L;
        lastXtoolTreeAt = 0L;
        lastXtoolTreeStatus = "Aucune inspection";
        lastEasySetExactLabels = "Aucun libellé relevé";
    }
    private long lastOpenRequestMillis;
    private long lastXtoolScanRequestMillis = -EasySetForegroundScanPolicy.MIN_INTERVAL_MS;
    private long lastNamedPanelTapMs = -EasySetPanelTapPolicy.NAMED_TAP_COOLDOWN_MS;
    private long lastUnnamedPanelTapMs = -EasySetPanelTapPolicy.UNNAMED_TAP_COOLDOWN_MS;
    private final Handler panelHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingPanelRead;
    private boolean scannedCurrentAccessibleOpenPanel;

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
            lastXtoolEventAt = SystemClock.elapsedRealtime();
            lastXtoolEventType = type;
            // xTool may implement "Volet EasySet" as a custom view whose tap
            // never produces a new Android window. Click events are observed
            // WITHOUT reading the window tree or performing any gestures.
            // The primary path now reads xTool's *accessible labels*
            // directly, rather than guessing from one Android click.
            // The snapshot is deferred until the UI has settled.
            scheduleAccessiblePanelInspection();
            if (clicked) {
                // Retain the existing narrow fallback for xTool WebViews
                // that expose no accessibility nodes for their EasySet row.
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

    /**
     * Content-only inspector for one foreground xTool window. Never walks
     * ChatGPT, Gmail, Jarvis overlays, or any other Android app.
     * No ACTION_CLICK, ACTION_SET_TEXT, dispatchGesture, or laser controls.
     */
    private void scheduleAccessiblePanelInspection() {
        if (!EasySetSessionService.isActive()
                || !JarvisBubbleService.isBubbleActive()
                || !Settings.canDrawOverlays(this)) return;
        // Coalesce event storms without postponing forever during a redraw.
        if (pendingPanelRead != null) return;
        pendingPanelRead = () -> {
            pendingPanelRead = null;
            inspectAccessiblePanelOnce();
        };
        panelHandler.postDelayed(pendingPanelRead, 350L);
    }

    private void inspectAccessiblePanelOnce() {
        lastXtoolTreeAt = SystemClock.elapsedRealtime();
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) {
            lastXtoolTreeStatus = "Fenêtre Android non disponible";
            lastXtoolNodes = 0;
            lastEasySetHeadings = 0;
            lastEasySetExactLabels = "Aucun libellé relevé";
            return;
        }
        if (!EasySetForegroundScanPolicy.XCS_PACKAGE
                .contentEquals(root.getPackageName())) {
            lastXtoolTreeStatus = "Fenêtre active différente de xTool";
            lastXtoolNodes = 0;
            lastEasySetHeadings = 0;
            lastEasySetExactLabels = "Aucun libellé relevé";
            return;
        }

        final int maxNodes = 240;
        final int maxDepth = 12;
        ArrayDeque<NodeAtDepth> queue = new ArrayDeque<>();
        List<String> labels = new ArrayList<>(12);
        List<String> exactLabels = new ArrayList<>(12);
        queue.add(new NodeAtDepth(root, 0));
        int examined = 0;

        while (!queue.isEmpty() && examined++ < maxNodes) {
            NodeAtDepth item = queue.removeFirst();
            AccessibilityNodeInfo node = item.node;
            if (node == null) continue;
            CharSequence pkg = node.getPackageName();
            if (pkg != null && !EasySetForegroundScanPolicy.XCS_PACKAGE
                    .contentEquals(pkg)) continue;

            collectEasySetLabel(node.getText(), labels, exactLabels, "Texte");
            collectEasySetLabel(node.getContentDescription(), labels,
                    exactLabels, "Description");

            if (item.depth >= maxDepth) continue;
            int count = Math.min(40, node.getChildCount());
            for (int i = 0; i < count && queue.size() < maxNodes; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.addLast(
                        new NodeAtDepth(child, item.depth + 1));
            }
        }

        EasySetAccessiblePanel.State state =
                EasySetAccessiblePanel.classify(labels);
        lastXtoolNodes = examined;
        lastEasySetHeadings = labels.size();
        StringBuilder exact = new StringBuilder();
        for (int i = 0; i < exactLabels.size(); i++) {
            if (i > 0) exact.append('\n');
            exact.append(i + 1).append(". ").append(exactLabels.get(i));
        }
        lastEasySetExactLabels = exact.length() == 0
                ? "Aucun libellé relevé" : exact.toString();
        lastXtoolTreeStatus = state == EasySetAccessiblePanel.State.OPEN
                ? "EasySet ouvert visible" : state == EasySetAccessiblePanel.State.CLOSED
                ? "Volet EasySet fermé visible" : "Champs EasySet non identifiés";
        if (state == EasySetAccessiblePanel.State.CLOSED) {
            // A new opening is eligible after the human closes the panel.
            scannedCurrentAccessibleOpenPanel = false;
        } else if (state == EasySetAccessiblePanel.State.OPEN
                && !scannedCurrentAccessibleOpenPanel
                && EasySetSessionService.isActive()) {
            scannedCurrentAccessibleOpenPanel = true;
            // Reuses the one-shot read-only capture route; a successful
            // accessibility label reading is not a numeric OCR result.
            EasySetSessionService.requestPanelTapScan(this);
        }
        // UNKNOWN is intentionally quiet: never infer panel state from
        // unrelated numbers or initiate repeated screenshot polling.
    }

    private static void collectEasySetLabel(CharSequence raw,
                                            List<String> result,
                                            List<String> exactResult, String source) {
        if (raw == null || result.size() >= 20) return;
        String original = raw.toString().trim().replaceAll("\\s+", " ");
        if (original.length() > 48 || original.length() == 0) return;
        String normalized = EasySetLabels.normalize(original);
        // Collect *only* the few recognized parameter headings.
        // Other xTool contents must not leave this method.
        if (normalized.length() > 0 && normalized.length() <= 48
                && (normalized.startsWith("volet easyset")
                    || normalized.startsWith("puissance")
                    || normalized.startsWith("power")
                    || normalized.startsWith("vitesse")
                    || normalized.startsWith("speed")
                    || normalized.startsWith("passer")
                    || normalized.startsWith("passages")
                    || normalized.startsWith("passes"))) {
            result.add(normalized);
            // Prevent numeric setting values appearing in diagnostics.
            // Headings normally contain letters, symbols and measurement
            // units, not numeric values.
            if (!original.matches(".*[0-9].*"))
                exactResult.add(source + " : «" + original + "»");
            else
                exactResult.add(source + " : [libellé avec nombre masqué]");
        }
    }

    private static final class NodeAtDepth {
        final AccessibilityNodeInfo node;
        final int depth;
        NodeAtDepth(AccessibilityNodeInfo node, int depth) {
            this.node = node;
            this.depth = depth;
        }
    }

    @Override
    public void onDestroy() {
        accessibilityConnected = false;
        if (pendingPanelRead != null)
            panelHandler.removeCallbacks(pendingPanelRead);
        pendingPanelRead = null;
        super.onDestroy();
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
