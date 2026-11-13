package ca.jarvis.bridge;

import java.util.List;

/**
 * Pure read-only diagnostic helpers for EasySet accessibility nodes.
 * They never invoke UI actions, write values or keep object references.
 */
final class EasySetControlProperties {
    static final int MAX_MATCHES = 16;
    static final int MAX_TEXT_LENGTH = 70;

    private EasySetControlProperties() { }

    static boolean matchesHeading(String text) {
        if (text == null) return false;
        String name = EasySetLabels.normalize(text);
        if (name.length() == 0 || name.length() > MAX_TEXT_LENGTH) return false;
        return name.equals("volet easyset") || name.startsWith("volet easyset ")
                || name.equals("puissance") || name.startsWith("puissance ")
                || name.equals("power") || name.startsWith("power ")
                || name.equals("vitesse") || name.startsWith("vitesse ")
                || name.equals("speed") || name.startsWith("speed ")
                || name.equals("passer") || name.startsWith("passer ")
                || name.equals("passages") || name.startsWith("passages ")
                || name.equals("passes") || name.startsWith("passes ");
    }

    static String line(String className, boolean clickable, boolean editable,
                       boolean focusable, String bounds,
                       List<String> actionNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("Type : ").append(className == null || className.isEmpty()
                ? "non exposé" : className).append('\n');
        sb.append("Cliquable : ").append(clickable ? "oui" : "non")
                .append(" | Modifiable : ").append(editable ? "oui" : "non")
                .append(" | Focus : ").append(focusable ? "oui" : "non").append('\n');
        sb.append("Position écran : ")
                .append(bounds == null ? "inconnue" : bounds).append('\n');
        sb.append("Actions Android : ");
        if (actionNames == null || actionNames.isEmpty()) {
            sb.append("aucune");
        } else {
            int cap = Math.min(actionNames.size(), 8);
            for (int i = 0; i < cap; i++) {
                if (i > 0) sb.append(", ");
                sb.append(actionNames.get(i));
            }
            if (actionNames.size() > cap) sb.append(", …");
        }
        return sb.toString();
    }

    static String report(List<String> records, int inspectedNodes) {
        if (records == null || records.isEmpty())
            return "Aucun champ EasySet reconnu dans l'arbre Android. "
                    + inspectedNodes + " nœuds examinés.";
        StringBuilder sb = new StringBuilder();
        sb.append(records.size()).append(" entrées EasySet relevées")
                .append(" sur ").append(inspectedNodes)
                .append(" nœuds examinés.\n");
        sb.append("Un libellé peut être non cliquable même si son parent l'est.\n");
        for (int i = 0; i < records.size(); i++) {
            sb.append("\n").append(i + 1).append(". ").append(records.get(i))
                    .append("\n");
        }
        return sb.toString();
    }
}
