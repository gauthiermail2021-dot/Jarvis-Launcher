package ca.jarvis.bridge;

import java.util.Collection;

/**
 * Passive EasySet panel classification using only the labels exposed by
 * xTool's Android accessibility tree. No numeric values, screenshots,
 * editing actions or machine commands are involved.
 */
final class EasySetAccessiblePanel {
    enum State { OPEN, CLOSED, UNKNOWN }

    private EasySetAccessiblePanel() { }

    static State classify(Collection<String> accessibleLabels) {
        if (accessibleLabels == null || accessibleLabels.isEmpty())
            return State.UNKNOWN;
        boolean row = false, power = false, speed = false, passes = false;
        for (String raw : accessibleLabels) {
            String label = EasySetLabels.normalize(raw);
            if (label.isEmpty() || label.length() > 48) continue;
            if (label.equals("volet easyset")
                    || label.startsWith("volet easyset "))
                row = true;
            else if (label.equals("puissance") || label.startsWith("puissance ")
                    || label.equals("power") || label.startsWith("power "))
                power = true;
            else if (label.equals("vitesse") || label.startsWith("vitesse ")
                    || label.equals("speed") || label.startsWith("speed "))
                speed = true;
            else if (label.equals("passer") || label.startsWith("passer ")
                    || label.equals("passages") || label.startsWith("passages ")
                    || label.equals("passes") || label.startsWith("passes "))
                passes = true;
        }
        if (power && speed && passes) return State.OPEN;
        if (row && !power && !speed && !passes) return State.CLOSED;
        return State.UNKNOWN;
    }
}
