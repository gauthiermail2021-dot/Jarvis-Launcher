package ca.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Rect;
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
    private static volatile ChatGptBubbleAccessibilityService clickTestService;
    private boolean clickTestPending;

    /** User-requested test, never triggered by an Android accessibility event. */
    static void requestEasySetPanelClickTest() {
        ChatGptBubbleAccessibilityService svc = clickTestService;
        if (svc == null) {
            JarvisBubbleService.showEasySetResult(
                    "TEST CLIC : activer le service « Jarvis Bridge — ChatGPT et xTool » dans Accessibilité.");
            return;
        }
        if (svc.clickTestPending) return;
        svc.clickTestPending = true;
        svc.panelHandler.postDelayed(() ->
                EasySetOneShotClickTest.run(svc, svc.panelHandler,
                        () -> svc.clickTestPending = false), 650L);
    }

    private static volatile long lastXtoolEventAt;
    private static volatile int lastXtoolEventType;
    private static volatile String lastXtoolTreeStatus = "Aucune inspection";
    private static volatile int lastXtoolNodes;
    private static volatile int lastEasySetHeadings;
    // Only short EasySet headings are retained in memory for diagnostics,
    // preserving duplicates/source: never collect numeric control values.
    private static volatile String lastEasySetExactLabels = "Aucun libellé relevé";
    private static volatile long lastXtoolTreeAt;
    // Last on-device xTool snapshot only; never persisted or uploaded.
    // Includes all text/content descriptions exposed by Android, not just
    // the 5 heading matches. Available only from a user-opened diagnostic.
    private static volatile String lastDiscoveredXtoolWords =
            "Aucune inspection xTool disponible.";
    private static volatile long lastDiscoveredWordsAt;
    // Targeted EasySet control properties, retained in RAM until next
    // xTool tree inspection; no AccessibilityNodeInfo handles are retained.
    private static volatile String lastEasySetControlProperties =
            "Aucune inspection des commandes disponible.";
    private static volatile long lastControlInspectionAt;

    /**
     * Compact read-only status used by the main menu.
     * Does not request screenshots, click controls, or change parameters.
     */
    static String easySetShortStatus() {
        if (!accessibilityConnected)
            return "EasySet\nAccessibilité Jarvis inactive.\n"
                    + "Consulter Réglages et diagnostics pour vérifier la connexion.";
        if (lastXtoolTreeAt == 0L)
            return "EasySet\nAucun état xTool relevé.\n"
                    + "Ouvrir xTool pour observer les libellés accessibles.";
        return "EasySet\n" + lastXtoolTreeStatus
                + "\nLecture Android uniquement : aucun réglage modifié.";
    }

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

    static String xToolDiscoveredWordsDiagnostic() {
        long since = lastDiscoveredWordsAt == 0L ? -1L
                : Math.max(0L, (SystemClock.elapsedRealtime()
                - lastDiscoveredWordsAt) / 1000L);
        return "DIAGNOSTIC EASYSET — MOTS xTool\n"
                + "Source : service d'accessibilité Android (xTool uniquement)\n"
                + (since < 0 ? "Aucune lecture effectuée.\n"
                : "Dernière lecture : " + since + " s.\n")
                + "« Texte » = texte accessible ; « Description » = libellé "
                + "d'accessibilité, parfois invisible à l'écran.\n\n"
                + lastDiscoveredXtoolWords
                + "\n\nLecture locale uniquement. Aucun paramètre modifié.";
    }

    static String xToolControlsDiagnostic() {
        long age = lastControlInspectionAt == 0L ? -1L
                : Math.max(0L, (SystemClock.elapsedRealtime()
                        - lastControlInspectionAt) / 1000L);
        return "DIAGNOSTIC EASYSET — COMMANDES xTool\n"
                + (age < 0L ? "Aucun relevé disponible.\n"
                : "Dernière inspection : " + age + " s.\n")
                + "Source : propriétés accessibles Android dans xTool.\n"
                + "Les contrôles parents sont inclus quand disponibles.\n"
                + "Lecture seule : aucune action exécutée.\n\n"
                + lastEasySetControlProperties
                + "\nAucun réglage ou commande laser modifié.";
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        accessibilityConnected = true;
        clickTestService = this;
        lastXtoolEventAt = 0L;
        lastXtoolTreeAt = 0L;
        lastXtoolTreeStatus = "Aucune inspection";
        lastEasySetExactLabels = "Aucun libellé relevé";
        lastDiscoveredXtoolWords = "Aucune inspection xTool disponible.";
        lastDiscoveredWordsAt = 0L;
        lastEasySetControlProperties = "Aucune inspection des commandes disponible.";
        lastControlInspectionAt = 0L;
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
            lastDiscoveredXtoolWords = "Fenêtre Android indisponible.";
            lastDiscoveredWordsAt = SystemClock.elapsedRealtime();
            lastEasySetControlProperties = "Fenêtre Android indisponible.";
            lastControlInspectionAt = SystemClock.elapsedRealtime();
            return;
        }
        if (!EasySetForegroundScanPolicy.XCS_PACKAGE
                .contentEquals(root.getPackageName())) {
            lastXtoolTreeStatus = "Fenêtre active différente de xTool";
            lastXtoolNodes = 0;
            lastEasySetHeadings = 0;
            lastEasySetExactLabels = "Aucun libellé relevé";
            lastDiscoveredXtoolWords = "Fenêtre active différente de xTool.";
            lastDiscoveredWordsAt = SystemClock.elapsedRealtime();
            lastEasySetControlProperties = "Fenêtre active différente de xTool.";
            lastControlInspectionAt = SystemClock.elapsedRealtime();
            return;
        }

        final int maxNodes = 240;
        final int maxDepth = 12;
        ArrayDeque<NodeAtDepth> queue = new ArrayDeque<>();
        List<String> labels = new ArrayList<>(12);
        List<String> exactLabels = new ArrayList<>(12);
        List<String> allAccessibleWords = new ArrayList<>(40);
        List<String> easySetControls = new ArrayList<>(8);
        queue.add(new NodeAtDepth(root, 0));
        int examined = 0;

        while (!queue.isEmpty() && examined++ < maxNodes) {
            NodeAtDepth item = queue.removeFirst();
            AccessibilityNodeInfo node = item.node;
            if (node == null) continue;
            CharSequence pkg = node.getPackageName();
            if (pkg != null && !EasySetForegroundScanPolicy.XCS_PACKAGE
                    .contentEquals(pkg)) continue;

            XtoolAccessibleWords.add(allAccessibleWords, node.getText(), "Texte");
            XtoolAccessibleWords.add(allAccessibleWords,
                    node.getContentDescription(), "Description");
            collectEasySetLabel(node.getText(), labels, exactLabels, "Texte");
            collectEasySetLabel(node.getContentDescription(), labels,
                    exactLabels, "Description");
            // No gestures, clicks or value changes: inspect only named
            // EasySet nodes and the immediately associated parent.
            appendEasySetControl(node, easySetControls);

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
        lastDiscoveredXtoolWords =
                XtoolAccessibleWords.format(allAccessibleWords, examined);
        lastDiscoveredWordsAt = SystemClock.elapsedRealtime();
        lastEasySetExactLabels = EasySetDiagnosticHeadings.enumerate(exactLabels);
        lastEasySetControlProperties =
                EasySetControlProperties.report(easySetControls, examined);
        lastControlInspectionAt = SystemClock.elapsedRealtime();
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

    private static void appendEasySetControl(AccessibilityNodeInfo node,
                                            List<String> controls) {
        if (controls.size() >= EasySetControlProperties.MAX_MATCHES)
            return;
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        String heading = null;
        String source = null;
        if (text != null && EasySetControlProperties.matchesHeading(text.toString())) {
            heading = text.toString();
            source = "Texte";
        } else if (description != null
                && EasySetControlProperties.matchesHeading(description.toString())) {
            heading = description.toString();
            source = "Description";
        }
        if (heading == null) return;

        StringBuilder item = new StringBuilder();
        item.append(source).append(" : «").append(heading).append("»\n");
        item.append("Élément :\n").append(describeNode(node));
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null && parent.getPackageName() != null
                && EasySetForegroundScanPolicy.XCS_PACKAGE
                        .contentEquals(parent.getPackageName())) {
            item.append("\nParent :\n").append(describeNode(parent));
        }
        controls.add(item.toString());
    }

    private static String describeNode(AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        String boundsText = bounds.left + "," + bounds.top
                + " — " + bounds.right + "," + bounds.bottom;
        List<String> actions = new ArrayList<>(6);
        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
            int actionId = action.getId();
            String actionName;
            if (actionId == AccessibilityNodeInfo.AccessibilityAction
                    .ACTION_SET_PROGRESS.getId()) {
                actionName = "SET_PROGRESS";
                actions.add(actionName);
                continue;
            }
            switch (actionId) {
                case AccessibilityNodeInfo.ACTION_CLICK: actionName = "CLICK"; break;
                case AccessibilityNodeInfo.ACTION_LONG_CLICK: actionName = "LONG_CLICK"; break;
                case AccessibilityNodeInfo.ACTION_FOCUS: actionName = "FOCUS"; break;
                case AccessibilityNodeInfo.ACTION_SET_TEXT: actionName = "SET_TEXT"; break;
                case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD:
                    actionName = "SCROLL_FORWARD"; break;
                case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:
                    actionName = "SCROLL_BACKWARD"; break;
                default: actionName = "action-" + actionId; break;
            }
            actions.add(actionName);
        }
        return EasySetControlProperties.line(
                node.getClassName() == null ? null : node.getClassName().toString(),
                node.isClickable(), node.isEditable(), node.isFocusable(),
                boundsText, actions);
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
            exactResult.add(EasySetDiagnosticHeadings.one(source, original));
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
        if (clickTestService == this) clickTestService = null;
        clickTestPending = false;
        lastDiscoveredXtoolWords = "Aucune inspection xTool disponible.";
        lastDiscoveredWordsAt = 0L;
        lastEasySetControlProperties = "Aucune inspection des commandes disponible.";
        lastControlInspectionAt = 0L;
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
