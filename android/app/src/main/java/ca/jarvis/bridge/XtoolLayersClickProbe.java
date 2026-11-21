package ca.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic and explicit one-shot ACTION_CLICK on an unambiguous, named xTool
 * Calques control. Never clicks coordinates, taps canvases, dismisses menus,
 * changes EasySet, or operates the laser.
 */
final class XtoolLayersClickProbe {
    private static final int MAX_DEPTH = 18;
    private static final int MAX_NODES = 250;
    private static final int MAX_CHILDREN = 50;
    private static final String HEADER = "DIAGNOSTIC XTOOL — CIBLE CALQUES\\n";

    private XtoolLayersClickProbe() { }

    static String inspect(AccessibilityService service) {
        return execute(service, false);
    }

    static String clickOnce(AccessibilityService service) {
        return execute(service, true);
    }

    private static String execute(AccessibilityService service, boolean click) {
        AccessibilityNodeInfo root = null;
        Scan scan = new Scan();
        try {
            root = service.getRootInActiveWindow();
            if (!isXtool(root)) {
                return HEADER + "Fenêtre active différente de xTool. Aucun clic.";
            }
            Point display = screen(service);
            if (display == null) {
                return HEADER + "Dimensions écran indisponibles. Aucun clic.";
            }
            inspectTree(root, display, 0, scan);
            StringBuilder report = new StringBuilder(512);
            report.append(HEADER)
                    .append("Mode : ").append(click
                            ? "TEST ACTION_CLICK unique" : "LECTURE SEULE")
                    .append("\\nNœuds examinés : ").append(scan.visited)
                    .append("\\nLibellés exacts Calques : ").append(scan.named)
                    .append("\\nLibellés visibles en bas : ").append(scan.bounded)
                    .append("\\nContrôles cliquables uniques : ")
                    .append(scan.targets.size()).append("\\n");
            if (!scan.summaries.isEmpty()) {
                report.append("\\nCandidats :\\n");
                for (String line : scan.summaries) report.append(line).append("\\n");
            }
            if (!click) {
                report.append("\\nAucun toucher. Test direct possible uniquement ")
                        .append("si un seul contrôle cliquable est identifié.");
                return report.toString();
            }
            if (!XtoolSessionManager.isActive(service)) {
                return report.append("\\nACTION_CLICK NON TENTÉE : session xTool inactive.")
                        .toString();
            }
            if (scan.targets.size() != 1) {
                return report.append("\\nACTION_CLICK NON TENTÉE : contrôle ")
                        .append(scan.targets.isEmpty()
                                ? "introuvable ou non cliquable."
                                : "ambigu (plusieurs cibles).")
                        .toString();
            }
            // Never click through a still-exposed contextual Dégrouper menu.
            if (XtoolLayersOneShot.ungroupPopupVisible(service)) {
                return report.append("\\nACTION_CLICK NON TENTÉE : ")
                        .append("menu Dégrouper encore visible. Aucun geste.")
                        .toString();
            }
            AccessibilityNodeInfo target = scan.targets.get(0);
            Rect area = new Rect();
            target.getBoundsInScreen(area);
            if (!target.isVisibleToUser() || !target.isEnabled()
                    || !target.isClickable()
                    || !XtoolLayersPolicy.validBottomButton(area.left,
                    area.top, area.right, area.bottom, display.x, display.y)) {
                return report.append("\\nACTION_CLICK NON TENTÉE : cible ")
                        .append("non valide au moment du clic.").toString();
            }
            boolean accepted = target.performAction(
                    AccessibilityNodeInfo.ACTION_CLICK);
            if (accepted) JarvisBubbleService.showXtoolTouchMarker(
                    area.centerX(), area.centerY());
            report.append("\\nACTION_CLICK : ")
                    .append(accepted ? "ACCEPTÉE" : "REFUSÉE")
                    .append("\\nOuverture du panneau : NON CONFIRMÉE.")
                    .append("\\nAucun toucher de coordonnées; aucune autre action.");
            return report.toString();
        } catch (RuntimeException error) {
            return HEADER + "Inspection/clic interrompu : "
                    + error.getClass().getSimpleName()
                    + ". Aucun geste de secours.";
        } finally {
            scan.recycle();
            if (root != null) root.recycle();
        }
    }

    private static void inspectTree(AccessibilityNodeInfo node, Point display,
                                    int depth, Scan scan) {
        if (node == null || !isXtool(node) || depth > MAX_DEPTH
                || scan.visited >= MAX_NODES) return;
        scan.visited++;
        if (XtoolLayersPolicy.exactLayersLabel(node.getText())
                || XtoolLayersPolicy.exactLayersLabel(node.getContentDescription())) {
            scan.named++;
            Rect labelArea = new Rect();
            node.getBoundsInScreen(labelArea);
            boolean bounded = node.isVisibleToUser() && node.isEnabled()
                    && XtoolLayersPolicy.validBottomButton(
                    labelArea.left, labelArea.top, labelArea.right,
                    labelArea.bottom, display.x, display.y);
            if (bounded) {
                scan.bounded++;
                AccessibilityNodeInfo target = findClickable(node, display);
                if (target != null) {
                    Rect targetArea = new Rect();
                    target.getBoundsInScreen(targetArea);
                    boolean duplicate = false;
                    for (AccessibilityNodeInfo existing : scan.targets) {
                        Rect existingArea = new Rect();
                        existing.getBoundsInScreen(existingArea);
                        if (existingArea.equals(targetArea)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (duplicate) target.recycle();
                    else scan.targets.add(target);
                }
                if (scan.summaries.size() < 8) {
                    scan.summaries.add("Calques [" + labelArea.flattenToString()
                            + "] / cible ACTION_CLICK "
                            + (target != null ? "candidate" : "non exposée"));
                }
            }
        }
        int count = Math.min(node.getChildCount(), MAX_CHILDREN);
        for (int i = 0; i < count && scan.visited < MAX_NODES; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                inspectTree(child, display, depth + 1, scan);
            } finally {
                child.recycle();
            }
        }
    }

    private static AccessibilityNodeInfo findClickable(
            AccessibilityNodeInfo label, Point display) {
        AccessibilityNodeInfo current = AccessibilityNodeInfo.obtain(label);
        for (int level = 0; current != null && level <= 4; level++) {
            if (!isXtool(current) || !current.isVisibleToUser()
                    || !current.isEnabled()) break;
            Rect box = new Rect();
            current.getBoundsInScreen(box);
            if (current.isClickable()
                    && XtoolLayersPolicy.validBottomButton(
                    box.left, box.top, box.right, box.bottom,
                    display.x, display.y)) {
                return current;
            }
            AccessibilityNodeInfo next = current.getParent();
            current.recycle();
            current = next;
        }
        if (current != null) current.recycle();
        return null;
    }

    private static boolean isXtool(AccessibilityNodeInfo node) {
        return node != null && node.getPackageName() != null
                && EasySetForegroundScanPolicy.XCS_PACKAGE.contentEquals(
                node.getPackageName());
    }

    private static Point screen(AccessibilityService service) {
        WindowManager wm = (WindowManager)
                service.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) return null;
        Point size = new Point();
        wm.getDefaultDisplay().getRealSize(size);
        return size.x > 0 && size.y > 0 ? size : null;
    }

    private static final class Scan {
        int visited;
        int named;
        int bounded;
        final List<AccessibilityNodeInfo> targets = new ArrayList<>();
        final List<String> summaries = new ArrayList<>();

        void recycle() {
            for (AccessibilityNodeInfo target : targets) target.recycle();
            targets.clear();
        }
    }
}
