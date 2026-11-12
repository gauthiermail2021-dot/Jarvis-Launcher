package ca.jarvis.bridge;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagnostic ponctuel en lecture seule : repere les libelles de calques,
 * leurs rectangles Android et les parents qui exposent un clic.
 * Ne commande ni XCS ni le laser. Aucun stockage ni televersement.
 */
final class XtoolLayerInspector {
    private static final int MAX_NODES = 300;
    private static final int MAX_DEPTH = 14;
    private static final int MAX_MATCHES = 30;
    private static final int MAX_WORDS = 65;

    private XtoolLayerInspector() { }

    static String inspect(AccessibilityNodeInfo root) {
        String prefix = "DIAGNOSTIC XTOOL — CALQUES\n";
        if (root == null) return prefix + "Fenetre Android non disponible.\n"
                + "Ouvrir XCS et le panneau Calque, puis recommencer.";
        if (root.getPackageName() == null
                || !EasySetForegroundScanPolicy.XCS_PACKAGE
                .contentEquals(root.getPackageName())) {
            return prefix + "La fenetre active n'est pas xTool Creative Space.\n"
                    + "Ouvrir le panneau Calque dans XCS avant la lecture.";
        }

        ArrayDeque<NodeDepth> queue = new ArrayDeque<>();
        queue.add(new NodeDepth(root, 0));
        List<String> words = new ArrayList<>();
        StringBuilder matches = new StringBuilder();
        int examined = 0;
        int matched = 0;
        boolean red = false, green = false, blue = false, layer = false;

        while (!queue.isEmpty() && examined < MAX_NODES) {
            NodeDepth item = queue.removeFirst();
            AccessibilityNodeInfo node = item.node;
            examined++;
            if (node.getPackageName() != null
                    && !EasySetForegroundScanPolicy.XCS_PACKAGE
                    .contentEquals(node.getPackageName())) continue;

            CharSequence[] labels = {node.getText(), node.getContentDescription()};
            String[] sources = {"Texte", "Description"};
            for (int i = 0; i < labels.length; i++) {
                String word = clean(labels[i]);
                if (word.isEmpty()) continue;
                if (words.size() < MAX_WORDS)
                    words.add(sources[i] + " : «" + word + "»");

                String kind = XtoolLayerNames.classify(word);
                if ("AUCUN".equals(kind)) continue;
                if ("ROUGE".equals(kind)) red = true;
                if ("VERT".equals(kind)) green = true;
                if ("BLEU".equals(kind)) blue = true;
                if ("CALQUE".equals(kind)) layer = true;
                if (matched++ >= MAX_MATCHES) continue;

                Rect area = new Rect();
                node.getBoundsInScreen(area);
                matches.append("\n").append(matched).append(". ")
                        .append(kind).append(" / ").append(sources[i])
                        .append(" : «").append(word).append("»")
                        .append("\n   rectangle ").append(rectText(area))
                        .append("; cliquable=").append(node.isClickable());
                if (node.getClassName() != null)
                    matches.append("; type=").append(node.getClassName());
                AccessibilityNodeInfo parent = node.getParent();
                for (int depth = 0; parent != null && depth < 3; depth++) {
                    if (parent.isClickable()) {
                        Rect parentArea = new Rect();
                        parent.getBoundsInScreen(parentArea);
                        matches.append("\n   parent cliquable : ")
                                .append(rectText(parentArea));
                        break;
                    }
                    parent = parent.getParent();
                }
            }

            if (item.depth >= MAX_DEPTH) continue;
            int children = Math.min(node.getChildCount(), 40);
            for (int i = 0; i < children && queue.size() < MAX_NODES; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null)
                    queue.addLast(new NodeDepth(child, item.depth + 1));
            }
        }

        StringBuilder result = new StringBuilder(prefix);
        result.append("Lecture locale : ").append(examined).append(" noeuds Android.\n")
                .append("Libelles presents : Calque=").append(layer ? "oui" : "non")
                .append(", Rouge=").append(red ? "oui" : "non")
                .append(", Vert=").append(green ? "oui" : "non")
                .append(", Bleu=").append(blue ? "oui" : "non")
                .append(".\n");
        if (matched == 0) {
            result.append("Aucun libelle de calque/couleur expose. Les pastilles colorees ")
                    .append("peuvent etre des dessins sans description Android.\n");
        } else {
            result.append("Libelles candidats et zones :").append(matches);
            if (matched > MAX_MATCHES)
                result.append("\n(Affichage limite a ")
                        .append(MAX_MATCHES).append(" correspondances.)");
        }
        result.append("\n\nAutres textes accessibles (pour reperer les entrees voisines) :");
        if (words.isEmpty()) result.append("\nAucun texte expose.");
        for (int i = 0; i < words.size(); i++)
            result.append("\n").append(i + 1).append(". ").append(words.get(i));
        if (words.size() >= MAX_WORDS)
            result.append("\n(liste limitee a ").append(MAX_WORDS).append(" entrees)");
        result.append("\n\nDégrouper : ")
                .append(XtoolUngroupReadiness.inspect(root));
        result.append("\n\nUne etiquette de couleur ne prouve pas le type de traitement. ")
                .append("Lecture seule, aucun reglage modifie.");
        return result.toString();
    }

    private static String clean(CharSequence raw) {
        if (raw == null) return "";
        String result = raw.toString().trim().replaceAll("\\s+", " ");
        if (result.length() > 100) result = result.substring(0, 100) + "…";
        return result;
    }

    private static String rectText(Rect rect) {
        return "[" + rect.left + "," + rect.top + "–"
                + rect.right + "," + rect.bottom + "]";
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
