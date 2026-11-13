package ca.jarvis.bridge;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

/** Read-only XCS Ungroup status inside the existing layer diagnostic. */
final class XtoolUngroupReadiness {
    private XtoolUngroupReadiness() { }

    static String inspect(AccessibilityNodeInfo root) {
        if (root == null || root.getPackageName() == null
                || !EasySetForegroundScanPolicy.XCS_PACKAGE
                    .contentEquals(root.getPackageName())) {
            return "fenêtre xTool absente.";
        }
        State state = new State();
        walk(root, 0, state);
        if (!state.found) {
            return "commande non exposée. Le groupe doit peut-être être sélectionné.";
        }
        return "libellé trouvé; visible=" + yes(state.visible)
                + ", actif=" + yes(state.enabled)
                + ", cliquable=" + yes(state.clickable)
                + ", sélection Android=" + yes(state.selected)
                + ", rectangle=" + state.bounds
                + ". Le bouton ne prouve pas que les objets sont séparés."
                + "\nDernière tentative : " + XtoolUngroupOneShot.lastAttempt();
    }

    private static String yes(boolean b) { return b ? "oui" : "non"; }

    private static void walk(AccessibilityNodeInfo node, int depth, State state) {
        if (node == null || state.found || depth > 14
                || node.getPackageName() == null
                || !EasySetForegroundScanPolicy.XCS_PACKAGE
                        .contentEquals(node.getPackageName())) return;
        if (XtoolUngroupPolicy.isUngroupLabel(node.getText())
                || XtoolUngroupPolicy.isUngroupLabel(node.getContentDescription())) {
            Rect rect = new Rect();
            node.getBoundsInScreen(rect);
            state.found = true;
            state.visible = node.isVisibleToUser();
            state.enabled = node.isEnabled();
            state.clickable = node.isClickable();
            state.selected = node.isSelected();
            state.bounds = "[" + rect.left + "," + rect.top + "–"
                    + rect.right + "," + rect.bottom + "]";
            return;
        }
        int children = Math.min(40, node.getChildCount());
        for (int i = 0; i < children; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try { walk(child, depth + 1, state); }
            finally { child.recycle(); }
            if (state.found) return;
        }
    }

    private static final class State {
        boolean found, visible, enabled, clickable, selected;
        String bounds = "inconnu";
    }
}
