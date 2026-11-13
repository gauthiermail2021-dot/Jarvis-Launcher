package ca.jarvis.bridge;

import java.util.Collection;

/**
 * Classifies one already-consented screenshot, without reading xTool's
 * accessibility window content or sending any touch/laser commands.
 *
 * Missing OCR digits are NOT proof that the panel is closed. Classification
 * requires the distinct "Volet EasySet" settings row and the surrounding
 * xTool settings/instruction context, with none of the open-panel labels.
 */
final class EasySetPanelState {
    enum State { OPEN, CLOSED, UNKNOWN }

    static final String CLOSED_NOTICE =
            "Volet EasySet fermé. Touche « Volet EasySet » dans xTool "
                    + "pour afficher les trois paramètres. "
                    + "Jarvis les analysera lorsque le volet s'ouvrira. "
                    + "Aucun réglage modifié.";

    private EasySetPanelState() { }

    static State classify(Collection<EasySetValues.Line> lines) {
        if (lines == null || lines.isEmpty()) return State.UNKNOWN;

        boolean panelRow = false;
        boolean settingsTab = false;
        boolean settingsContext = false;
        boolean power = false, speed = false, passes = false;
        boolean ownJarvisOverlay = false;
        StringBuilder all = new StringBuilder();

        for (EasySetValues.Line line : lines) {
            if (line == null) continue;
            String text = EasySetLabels.normalize(line.text);
            if (text.isEmpty()) continue;
            all.append(' ').append(text);

            if (text.contains("jarvis resultat easyset")
                    || text.contains("jarvis easyset ferme")
                    || text.contains("jarvis bridge"))
                ownJarvisOverlay = true;

            if (text.equals("volet easyset")
                    || text.startsWith("volet easyset "))
                panelRow = true;

            if (text.equals("parametres") || text.startsWith("parametres "))
                settingsTab = true;
            if (text.startsWith("reglages")
                    || text.startsWith("sortie")
                    || text.startsWith("type de traitement"))
                settingsContext = true;

            // A word inside an instruction, e.g. "Puissance / Vitesse",
            // must not be mistaken for the heading of an opened field.
            if (text.equals("puissance") || text.startsWith("puissance ")
                    || text.equals("power") || text.startsWith("power "))
                power = true;
            if (text.equals("vitesse") || text.startsWith("vitesse ")
                    || text.equals("speed") || text.startsWith("speed "))
                speed = true;
            if (text.equals("passer") || text.startsWith("passer ")
                    || text.equals("passages") || text.startsWith("passages "))
                passes = true;
        }
        if (ownJarvisOverlay) return State.UNKNOWN;
        if (power && speed && passes) return State.OPEN;
        if (power || speed || passes) return State.UNKNOWN;

        String joined = all.toString();
        boolean instruction = joined.contains("cliquer sur le volet easyset")
                && joined.contains("parametre");
        if (panelRow && (instruction || (settingsTab && settingsContext)))
            return State.CLOSED;
        return State.UNKNOWN;
    }
}
