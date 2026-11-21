package ca.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;

/**
 * Read-only post-action verification of xTool layer labels. It does not
 * change selection, read machine settings or interact with the laser.
 *
 * A live UI exposing at least two distinct layer color names provides
 * positive evidence of a displayed layer list, not proof of saved settings.
 */
final class XtoolLayerPanelEvidence {
    private static final int MAX_NODES = 250;
    private static final int MAX_DEPTH = 16;

    private XtoolLayerPanelEvidence() { }

    static String inspect(AccessibilityService service) {
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) return "Panneau Calques : non confirmé (fenêtre absente).";
        ArrayDeque<NodeDepth> pending = new ArrayDeque<>();
        pending.add(new NodeDepth(root, 0));
        boolean red = false, green = false, blue = false;
        int checked = 0;
        try {
            if (!isXtool(root)) {
                return "Panneau Calques : non confirmé (xTool hors premier plan).";
            }
            while (!pending.isEmpty() && checked < MAX_NODES) {
                NodeDepth current = pending.removeFirst();
                AccessibilityNodeInfo node = current.node;
                try {
                    checked++;
                    if (!isXtool(node)) continue;
                    if (node.isVisibleToUser()) {
                        String[] labels = {
                                XtoolLayerNames.classify(toStringSafe(node.getText())),
                                XtoolLayerNames.classify(toStringSafe(
                                        node.getContentDescription()))
                        };
                        for (String label : labels) {
                            if ("ROUGE".equals(label)) red = true;
                            else if ("VERT".equals(label)) green = true;
                            else if ("BLEU".equals(label)) blue = true;
                        }
                    }
                    if (current.depth >= MAX_DEPTH) continue;
                    int childCount = Math.min(40, node.getChildCount());
                    for (int i = 0; i < childCount
                            && pending.size() < MAX_NODES; i++) {
                        AccessibilityNodeInfo child = node.getChild(i);
                        if (child != null) pending.addLast(
                                new NodeDepth(child, current.depth + 1));
                    }
                } finally {
                    if (node != root) node.recycle();
                }
            }
            if (XtoolLayerPanelEvidencePolicy.hasMultipleColors(red, green, blue)) {
                return "Panneau Calques : indices positifs (plusieurs couleurs "
                        + "de calques visibles). Vérifier visuellement l'ouverture.";
            }
            return "Panneau Calques : non confirmé (moins de deux couleurs "
                    + "nommées visibles; cela n'exclut pas un panneau à un seul calque).";
        } catch (RuntimeException error) {
            return "Panneau Calques : contrôle interrompu ("
                    + error.getClass().getSimpleName() + ").";
        } finally {
            while (!pending.isEmpty()) {
                AccessibilityNodeInfo node = pending.removeFirst().node;
                if (node != root) node.recycle();
            }
            root.recycle();
        }
    }

    private static String toStringSafe(CharSequence value) {
        return value == null ? "" : value.toString();
    }

    private static boolean isXtool(AccessibilityNodeInfo node) {
        return node != null && node.getPackageName() != null
                && EasySetForegroundScanPolicy.XCS_PACKAGE.contentEquals(
                        node.getPackageName());
    }

    private static final class NodeDepth {
        final AccessibilityNodeInfo node;
        final int depth;
        NodeDepth(AccessibilityNodeInfo node, int depth) {
            this.node = node;
            this.depth = depth;
        }
    }
}
