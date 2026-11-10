package ca.jarvis.bridge;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;

/**
 * Cautious, offline classifier for single screenshots.
 * Does not retain text, numeric parameters or the screen image.
 */
public final class EasySetLabels {
    private EasySetLabels() { }

    public static String diagnose(Collection<String> recognizedLines) {
        if (recognizedLines == null || recognizedLines.isEmpty())
            return "Écran illisible : aucun libellé reconnu. Réessaie dans xTool.";
        boolean heading = false, power = false, speed = false, passes = false;
        boolean jarvisScreen = false;
        for (String line : recognizedLines) {
            String word = normalize(line);
            if (word.isEmpty()) continue;
            // Reject the Jarvis diagnostic UI. "EasySet" in our own heading
            // is never sufficient evidence that xTool was captured.
            if (word.contains("analyse visuelle")
                    || word.contains("analyser easyset")
                    || word.contains("afficher le dernier resultat")
                    || word.contains("jarvis 1 2 27")
                    || word.contains("jarvis 1 2 26")
                    || word.contains("jarvis 1 2 25")
                    || word.contains("jarvis resultat easyset")
                    || word.contains("jarvis 1 2 24")
                    || word.contains("jarvis 1 2 23")) jarvisScreen = true;
            if (word.contains("easyset")) heading = true;
            if (word.contains("puissance") || word.contains("power")) power = true;
            if (word.contains("vitesse") || word.contains("speed")) speed = true;
            if (word.contains("passer") || word.contains("passage")
                    || word.contains("passes") || word.contains("pass count")) passes = true;
        }
        if (jarvisScreen)
            return "Capture de Jarvis, pas de xTool. Rouvre EasySet dans xTool "
                    + "et lance le diagnostic depuis la bulle.";
        int count = (power ? 1 : 0) + (speed ? 1 : 0) + (passes ? 1 : 0);
        if (count < 2)
            return "Paramètres EasySet non reconnus. Ouvre vraiment le volet "
                    + "Puissance / Vitesse / Passer dans xTool, puis réessaie.";
        return "EasySet " + (heading ? "reconnu" : "probable")
                + " — Puissance : " + (power ? "oui" : "non")
                + " ; Vitesse : " + (speed ? "oui" : "non")
                + " ; Passages : " + (passes ? "oui" : "non")
                + ". Lecture seule : contrôle les valeurs dans xTool.";
    }

    static String normalize(String value) {
        if (value == null) return "";
        String lower = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return lower.replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }
}
