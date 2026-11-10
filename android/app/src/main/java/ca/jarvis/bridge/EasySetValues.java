package ca.jarvis.bridge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative read-only OCR interpretation for the visible EasySet values.
 * No Android dependency: pure Java tests can exercise real OCR text and bounds.
 * A value is omitted instead of borrowing an unrelated number on the screen.
 */
public final class EasySetValues {
    private EasySetValues() { }

    public static final class Line {
        public final String text;
        public final int left, top, right, bottom;

        public Line(String text, int left, int top, int right, int bottom) {
            this.text = text == null ? "" : text;
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }

    private enum Field {
        POWER("Puissance", "%", "(?iu)^\\s*(?:puissance|power)\\b"),
        SPEED("Vitesse", "mm/s", "(?iu)^\\s*(?:vitesse|speed)\\b"),
        PASSES("Passages", "", "(?iu)^\\s*(?:passer|passage|passages|passes|pass count)\\b");

        final String title, unit;
        final Pattern heading;

        Field(String title, String unit, String regex) {
            this.title = title;
            this.unit = unit;
            this.heading = Pattern.compile(regex);
        }
    }

    // The xTool value cells may carry a small navigation chevron on the right.
    // Accept only that trailing UI icon, not arbitrary OCR text or units.
    private static final Pattern NUMBER = Pattern.compile(
            "(?iu)^\\s*(\\d{1,4}(?:[.,]\\d{1,2})?)\\s*(%|mm\\s*/\\s*s|fois)?\\s*[›>▶▸►]?\\s*$");

    public static String diagnose(Collection<Line> scannedLines) {
        return diagnose(scannedLines, null);
    }

    /**
     * A second, enlarged OCR pass may provide missing right-column values.
     * Never replace a value already read from the full screen with one from
     * the enhancement; keep xTool's headings and safety checks anchored in
     * the original screenshot.
     */
    public static String diagnose(Collection<Line> scannedLines,
                                  Collection<Line> enhancedRightColumn) {
        if (scannedLines == null) return EasySetLabels.diagnose(null);
        List<Line> lines = new ArrayList<>(scannedLines);
        List<String> texts = new ArrayList<>();
        for (Line line : lines) texts.add(line.text);
        String gate = EasySetLabels.diagnose(texts);
        if (!gate.startsWith("EasySet ")) return gate;

        String power = read(lines, Field.POWER);
        String speed = read(lines, Field.SPEED);
        String passes = read(lines, Field.PASSES);
        if (enhancedRightColumn != null && !enhancedRightColumn.isEmpty()
                && (power == null || speed == null || passes == null)) {
            List<Line> combined = new ArrayList<>(lines);
            combined.addAll(enhancedRightColumn);
            if (power == null) power = read(combined, Field.POWER);
            if (speed == null) speed = read(combined, Field.SPEED);
            if (passes == null) passes = read(combined, Field.PASSES);
        }
        return gate.substring(0, gate.indexOf(" — ") > 0 ? gate.indexOf(" — ") : gate.length())
                + " — Puissance : " + (power == null ? "non lue" : power + " %")
                + " ; Vitesse : " + (speed == null ? "non lue" : speed + " mm/s")
                + " ; Passages : " + (passes == null ? "non lus" : passes)
                + ". Valeurs OCR indicatives : vérifie toujours dans xTool. "
                + "Aucun réglage modifié.";
    }

    private static String read(List<Line> lines, Field field) {
        String chosen = null;
        for (Line label : lines) {
            Matcher heading = field.heading.matcher(label.text);
            if (!heading.find()) continue;
            String suffix = label.text.substring(heading.end()).trim();
            // Inline OCR: "Puissance (%) : 50%" or "Vitesse (mm/s) 78".
            suffix = suffix.replaceFirst(
                    "(?iu)^\\s*\\(\\s*(?:%|mm\\s*/\\s*s)\\s*\\)", "").trim();
            suffix = suffix.replaceFirst("^[=:：]\\s*", "").trim();
            String matched = asValue(suffix, field);

            // The Android xTool UI puts its values on the far right, often
            // one row BELOW the label (at the height of the slider).
            // Never "rescue" a malformed inline number using another cell.
            if (matched == null && !suffix.matches(".*\\d.*")) {
                matched = inFieldZone(lines, label, field);
            }
            if (matched != null) {
                // Conflicting observations are ambiguous; omit, do not guess.
                if (chosen != null && !chosen.equals(matched)) return null;
                chosen = matched;
            }
        }
        return chosen;
    }

    /**
     * Field-specific zone, limited above the next EasySet parameter heading.
     * Works for the xTool "label / slider / value aligned at far right" UI
     * without collecting a value from another setting or from the SVG canvas.
     */
    private static String inFieldZone(List<Line> lines, Line label, Field field) {
        if (label.right <= label.left || label.bottom <= label.top) return null;
        int height = label.bottom - label.top;
        int top = label.top - Math.max(10, height / 2);
        int bottom = label.top + Math.max(190, height * 7);

        for (Line other : lines) {
            if (other == label || other.top <= label.top + height / 2) continue;
            if (isSettingHeading(other.text)) {
                bottom = Math.min(bottom, other.top - Math.max(8, height / 4));
            }
        }
        if (bottom <= top) return null;
        String candidate = null;
        for (Line other : lines) {
            if (other == label || other.right <= other.left || other.bottom <= other.top)
                continue;
            int centerY = (other.top + other.bottom) / 2;
            if (centerY < top || centerY >= bottom) continue;
            // The editable numeric cell is to the RIGHT of the label.
            // Reject positions that might be the left-hand slider or its tick marks.
            if (other.left < label.right + Math.max(12, height / 2)) continue;
            String parsed = asValue(other.text, field);
            if (parsed == null) continue;
            if (candidate != null && !candidate.equals(parsed)) return null;
            candidate = parsed;
        }
        return candidate;
    }

    private static boolean isSettingHeading(String value) {
        if (value == null) return false;
        for (Field candidate : Field.values()) {
            if (candidate.heading.matcher(value).find()) return true;
        }
        return false;
    }

    private static String asValue(String text, Field field) {
        if (text == null || text.isEmpty()) return null;
        Matcher number = NUMBER.matcher(text);
        if (!number.matches()) return null;
        String raw = number.group(1);
        String unit = number.group(2);
        // Misread unit is a strong signal the number belongs elsewhere.
        if (field == Field.POWER && unit != null && !unit.equals("%")) return null;
        if (field == Field.SPEED && unit != null
                && !unit.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").equals("mm/s")) return null;
        if (field == Field.PASSES && unit != null && !unit.equalsIgnoreCase("fois")) return null;
        double value;
        try { value = Double.parseDouble(raw.replace(',', '.')); }
        catch (NumberFormatException ignored) { return null; }
        if (field == Field.POWER && (value < 0 || value > 100)) return null;
        if (field == Field.SPEED && (value <= 0 || value > 3000)) return null;
        if (field == Field.PASSES && (value < 1 || value > 99 || value != Math.floor(value))) return null;
        if (raw.startsWith("0") && raw.length() > 1 && raw.charAt(1) != ','
                && raw.charAt(1) != '.') return null;
        return raw.replace('.', ',');
    }
}
