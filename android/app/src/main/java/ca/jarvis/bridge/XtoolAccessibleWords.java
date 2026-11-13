package ca.jarvis.bridge;

import java.util.List;

/**
 * Read-only, bounded preview of text and accessibility descriptions from xTool.
 * The output lives in memory only, visible solely through a user-selected
 * Jarvis diagnostic. No screen images, file writes, network or gestures.
 */
final class XtoolAccessibleWords {
    static final int MAX_ENTRIES = 100;
    static final int MAX_CHARS_PER_ENTRY = 100;

    private XtoolAccessibleWords() { }

    static void add(List<String> entries, CharSequence raw, String source) {
        if (entries == null || raw == null || entries.size() >= MAX_ENTRIES)
            return;
        String actual = raw.toString().trim().replaceAll("\\s+", " ");
        if (actual.isEmpty()) return;
        if (actual.length() > MAX_CHARS_PER_ENTRY)
            actual = actual.substring(0, MAX_CHARS_PER_ENTRY) + "…";
        entries.add(source + " : «" + actual + "»");
    }

    static String format(List<String> entries, int nodeCount) {
        if (entries == null || entries.isEmpty())
            return "Aucun mot ou libellé exposé par Android.\n"
                    + "Nœuds examinés : " + nodeCount + ".";
        StringBuilder result = new StringBuilder();
        result.append(entries.size()).append(" textes/descriptions relevés")
                .append(" sur ").append(nodeCount).append(" nœuds.\n")
                .append("Les doublons sont conservés.\n");
        if (entries.size() >= MAX_ENTRIES)
            result.append("Limite d'affichage : premiers ")
                    .append(MAX_ENTRIES).append(" éléments.\n");
        for (int i = 0; i < entries.size(); i++) {
            result.append('\n').append(i + 1).append(". ")
                    .append(entries.get(i));
        }
        return result.toString();
    }
}
