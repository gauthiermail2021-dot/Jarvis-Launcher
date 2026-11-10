package ca.jarvis.bridge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Ephemeral, minimal explanation of what the OCR really saw in Passer's row.
 * No logs, network, image writes or persistent recognition history.
 * Do not "fix" OCR by assuming a 1 from an I/l glyph.
 */
final class EasySetPassesDiagnostic {
    private static final Pattern LABEL = Pattern.compile(
            "(?iu)^\\s*(?:passer|passage|passages|passes|pass count)\\b");
    private static final Pattern SMALL_TOKEN = Pattern.compile(
            "(?iu)^[0-9Il|>›▶▸►▷.,?\\- ]{1,12}$");

    private final List<EasySetValues.Line> base;
    private final EasySetValues.Line label;
    private final List<EasySetValues.Line> extras = new ArrayList<>();
    private final String screen;
    private String column = "non exécutée";
    private String crop = "non exécuté";
    private String exceptional = "";

    EasySetPassesDiagnostic(Collection<EasySetValues.Line> baseLines) {
        base = new ArrayList<>(baseLines);
        EasySetValues.Line found = null;
        for (EasySetValues.Line line : base) {
            if (LABEL.matcher(line.text).find()
                    && line.bottom > line.top && line.right > line.left) {
                if (found == null || line.top > found.top) found = line;
            }
        }
        label = found;
        screen = stage(base, EasySetValues.diagnose(base));
    }

    void readColumn(Collection<EasySetValues.Line> candidates) {
        List<EasySetValues.Line> stage = new ArrayList<>(candidates);
        extras.addAll(stage);
        column = stage(stage, EasySetValues.diagnose(base, extras));
    }

    void readCrop(Collection<EasySetValues.Line> candidates) {
        List<EasySetValues.Line> stage = new ArrayList<>(candidates);
        extras.addAll(stage);
        crop = stage(stage, EasySetValues.diagnose(base, extras));
    }

    void columnSkipped(String reason) {
        column = "non exécutée (" + reason + ")";
    }

    void cropSkipped(String reason) {
        crop = "non exécuté (" + reason + ")";
    }

    void columnError() {
        column = "erreur OCR (aucune donnée ajoutée)";
    }

    void cropError() {
        crop = "erreur OCR (aucune donnée ajoutée)";
    }

    String append(String summary) {
        if (summary == null || !summary.startsWith("EasySet ")) return summary;
        // Only 3 compact, row-restricted sets of snippets, not the entire
        // user's screen or the image. Never saved by EasySetVisionAnalyzer.
        String association = summary.contains("Passages : non lus")
                ? (label == null
                        ? "refusée : libellé Passer absent"
                        : "refusée : chiffre absent, illisible ou hors zone")
                : "acceptée : nombre entier dans la zone Passer";
        return summary + "\n\nDIAGNOSTIC PASSER (temporaire)"
                + "\nLibellé : " + (label == null ? "non repéré" : "repéré")
                + "\nÉcran : " + screen
                + "\nColonne droite : " + column
                + "\nZone agrandie : " + crop
                + "\nAssociation : " + association;
    }

    private String stage(Collection<EasySetValues.Line> stage,
                         String fullResult) {
        if (label == null) return "libellé absent, comparaison impossible";
        String seen = shortTokens(stage);
        boolean accepted = fullResult != null
                && fullResult.startsWith("EasySet ")
                && !fullResult.contains("Passages : non lus");
        return seen + " ; " + (accepted ? "accepté" : "non associé");
    }

    private String shortTokens(Collection<EasySetValues.Line> stage) {
        Set<String> hits = new LinkedHashSet<>();
        int labelHeight = label.bottom - label.top;
        int top = label.top - Math.max(28, labelHeight);
        int bottom = label.bottom + Math.max(85, labelHeight * 2);
        int minX = label.right + Math.max(12, labelHeight / 2);
        for (EasySetValues.Line line : stage) {
            if (line.bottom <= line.top || line.right <= line.left) continue;
            int y = (line.top + line.bottom) / 2;
            if (y < top || y > bottom || line.left < minX) continue;
            String token = line.text.trim();
            if (!SMALL_TOKEN.matcher(token).matches()) continue;
            hits.add("«" + token + "»");
            if (hits.size() >= 3) break;
        }
        if (hits.isEmpty()) return "aucun petit caractère détecté à droite";
        return String.join(", ", hits);
    }
}
