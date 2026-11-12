package ca.jarvis.bridge;

import java.util.List;

/** Diagnostic-only formatting of EasySet heading labels, not field values. */
final class EasySetDiagnosticHeadings {
    private EasySetDiagnosticHeadings() { }

    static String one(String source, String original) {
        if (original == null) return source + " : [indisponible]";
        // Never expose numeric machine settings in the connection diagnostic.
        if (original.matches(".*[0-9].*"))
            return source + " : [libellé avec nombre masqué]";
        return source + " : «" + original + "»";
    }

    static String enumerate(List<String> headings) {
        if (headings == null || headings.isEmpty())
            return "Aucun libellé relevé";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < headings.size(); i++) {
            if (i > 0) result.append('\n');
            result.append(i + 1).append(". ").append(headings.get(i));
        }
        return result.toString();
    }
}
