package ca.jarvis.bridge;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;

/** Pure, testable classifier: ignores recognized values and unrelated screen text. */
public final class EasySetLabels {
    private EasySetLabels() { }

    public static String diagnose(Collection<String> recognizedLines) {
        boolean heading = false, power = false, speed = false, passes = false;
        if (recognizedLines == null) return "Aucune donnée lisible.";
        for (String line : recognizedLines) {
            String word = normalize(line);
            if (word.contains("easyset")) heading = true;
            if (word.contains("puissance") || word.contains("power")) power = true;
            if (word.contains("vitesse") || word.contains("speed")) speed = true;
            if (word.contains("passer") || word.contains("passage")
                    || word.contains("passes") || word.contains("pass count")) passes = true;
        }
        // Ignore random screens unless EasySet or multiple expected labels are found.
        int n = (power ? 1 : 0) + (speed ? 1 : 0) + (passes ? 1 : 0);
        if (!heading && n < 2)
            return "Volet EasySet non reconnu. Affiche les paramètres xTool et réessaie.";
        return "EasySet " + (heading ? "reconnu" : "probable") + ". "
                + "Puissance : " + (power ? "repérée" : "non repérée")
                + " ; Vitesse : " + (speed ? "repérée" : "non repérée")
                + " ; Passages : " + (passes ? "repérés" : "non repérés")
                + ". Aucun chiffre ni réglage n'est enregistré ou modifié.";
    }

    static String normalize(String value) {
        if (value == null) return "";
        String lower = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return lower.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
