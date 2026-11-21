package ca.jarvis.bridge;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * One-time, READ-ONLY Android accessibility snapshot just after Degrouper.
 * No performAction, dispatchGesture, capture, settings, or machine commands.
 *
 * "Selected" is Android's isSelected() state. xTool may render the drawing
 * inside a custom view without exposing the selected shapes as nodes.
 */
final class XtoolSelectionInspector {
    private static final int MAX_NODES = 500;
    private static final int MAX_DEPTH = 18;
    private static final int MAX_ROWS = 22;
    private static final int MAX_CHILDREN = 50;
    private static final String HEADER = "DIAGNOSTIC XTOOL — SÉLECTION\n";

    private XtoolSelectionInspector() { }

    static String inspect(AccessibilityNodeInfo root) {
        if (root == null) {
            return HEADER + "Fenêtre Android non disponible. Ouvrir xTool / Conception.";
        }
        ArrayDeque<NodeDepth> queue = new ArrayDeque<>();
        queue.addLast(new NodeDepth(root, 0));
        try {
            if (!isXtool(root)) {
                return HEADER + "Fenêtre active différente de xTool Creative Space.";
            }
            int visited = 0;
            int selected = 0;
            int withClearSelection = 0;
            int withSelect = 0;
            int canvasCandidates = 0;
            int shown = 0;
            List<String> rows = new ArrayList<>();

            while (!queue.isEmpty() && visited < MAX_NODES) {
                NodeDepth item = queue.removeFirst();
                AccessibilityNodeInfo node = item.node;
                try {
                    if (!isXtool(node)) continue;
                    visited++;
                    if (node.isVisibleToUser()) {
                        boolean isSelected = node.isSelected();
                        boolean supportsClear = hasAction(node,
                                AccessibilityNodeInfo.ACTION_CLEAR_SELECTION);
                        boolean supportsSelect = hasAction(node,
                                AccessibilityNodeInfo.ACTION_SELECT);
                        boolean looksLikeCanvas = isCanvasCandidate(node);

                        if (isSelected) selected++;
                        if (supportsClear) withClearSelection++;
                        if (supportsSelect) withSelect++;
                        if (looksLikeCanvas) canvasCandidates++;
                        if ((isSelected || supportsClear || looksLikeCanvas)
                                && shown < MAX_ROWS) {
                            rows.add(row(node, item.depth, isSelected,
                                    supportsClear, supportsSelect));
                            shown++;
                        }
                    }
                    if (item.depth >= MAX_DEPTH) continue;
                    int count = Math.min(MAX_CHILDREN, node.getChildCount());
                    for (int i = 0; i < count && queue.size() < MAX_NODES; i++) {
                        AccessibilityNodeInfo child = node.getChild(i);
                        if (child != null) queue.addLast(
                                new NodeDepth(child, item.depth + 1));
                    }
                } finally {
                    if (node != root) node.recycle();
                }
            }

            StringBuilder report = new StringBuilder(1000);
            report.append(HEADER)
                    .append("Instantané après Dégrouper — LECTURE SEULE\n")
                    .append("Nœuds examinés : ").append(visited).append("\n")
                    .append("Éléments marqués selected=true : ").append(selected).append("\n")
                    .append("ACTION_CLEAR_SELECTION exposée : ")
                    .append(withClearSelection).append("\n")
                    .append("ACTION_SELECT exposée : ").append(withSelect).append("\n")
                    .append("Candidats canevas/WebView : ")
                    .append(canvasCandidates).append("\n\n");
            if (rows.isEmpty()) {
                report.append("Aucun élément sélectionné ou canevas identifiable ")
                        .append("dans l'arbre d'accessibilité.\n");
            } else {
                report.append("Éléments pertinents (")
                        .append(rows.size()).append(" premiers) :\n");
                for (int i = 0; i < rows.size(); i++) {
                    report.append("\n").append(i + 1).append(". ")
                            .append(rows.get(i)).append("\n");
                }
            }
            report.append("\nIMPORTANT : selected=true peut désigner l'onglet ")
                    .append("Conception et non les formes. Si aucune action n'est ")
                    .append("exposée, cela ne prouve pas que le dessin est vide.\n")
                    .append("Aucun clic ni réglage effectué après Dégrouper.");
            return report.toString();
        } catch (RuntimeException failure) {
            return HEADER + "Lecture Android impossible : "
                    + failure.getClass().getSimpleName()
                    + "\nAucun clic de désélection effectué.";
        } finally {
            while (!queue.isEmpty()) {
                AccessibilityNodeInfo node = queue.removeFirst().node;
                if (node != root) node.recycle();
            }
            root.recycle();
        }
    }

    private static String row(AccessibilityNodeInfo node, int depth,
                              boolean selected, boolean clear, boolean select) {
        Rect b = new Rect();
        node.getBoundsInScreen(b);
        String type = safe(node.getClassName());
        String label = safe(node.getText());
        String description = safe(node.getContentDescription());
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" | profondeur=").append(depth)
                .append("\n   texte=«").append(label).append("»")
                .append(" | description=«").append(description).append("»")
                .append("\n   bounds=[").append(b.left).append(",").append(b.top)
                .append(" – ").append(b.right).append(",").append(b.bottom).append("]")
                .append("\n   selected=").append(selected)
                .append(" | clickable=").append(node.isClickable())
                .append(" | enabled=").append(node.isEnabled())
                .append("\n   SELECT=").append(select)
                .append(" | CLEAR_SELECTION=").append(clear)
                .append(" | actions=").append(actionNames(node));
        return sb.toString();
    }

    private static String actionNames(AccessibilityNodeInfo node) {
        StringBuilder s = new StringBuilder();
        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
            if (s.length() > 0) s.append(", ");
            int id = action.getId();
            if (id == AccessibilityNodeInfo.ACTION_CLEAR_SELECTION) s.append("CLEAR_SELECTION");
            else if (id == AccessibilityNodeInfo.ACTION_SELECT) s.append("SELECT");
            else if (id == AccessibilityNodeInfo.ACTION_CLICK) s.append("CLICK");
            else if (id == AccessibilityNodeInfo.ACTION_LONG_CLICK) s.append("LONG_CLICK");
            else if (id == AccessibilityNodeInfo.ACTION_FOCUS) s.append("FOCUS");
            else if (id == AccessibilityNodeInfo.ACTION_CLEAR_FOCUS) s.append("CLEAR_FOCUS");
            else s.append(id);
            if (s.length() > 180) {
                s.append("…");
                break;
            }
        }
        return s.length() > 0 ? s.toString() : "(aucune)";
    }

    private static boolean hasAction(AccessibilityNodeInfo node, int target) {
        for (AccessibilityNodeInfo.AccessibilityAction action : node.getActionList()) {
            if (action.getId() == target) return true;
        }
        return false;
    }

    private static boolean isCanvasCandidate(AccessibilityNodeInfo node) {
        String className = safe(node.getClassName()).toLowerCase(Locale.ROOT);
        String description = safe(node.getContentDescription()).toLowerCase(Locale.ROOT);
        String text = safe(node.getText()).toLowerCase(Locale.ROOT);
        return className.contains("webview") || className.contains("surfaceview")
                || className.contains("textureview")
                || description.contains("canvas") || description.contains("canevas")
                || text.equals("canvas") || text.equals("canevas");
    }

    private static boolean isXtool(AccessibilityNodeInfo node) {
        return node != null && node.getPackageName() != null
                && EasySetForegroundScanPolicy.XCS_PACKAGE.contentEquals(
                        node.getPackageName());
    }

    private static String safe(CharSequence raw) {
        if (raw == null) return "";
        String clean = raw.toString().trim().replaceAll("\\s+", " ");
        if (clean.length() > 85) clean = clean.substring(0, 85) + "…";
        return clean;
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
