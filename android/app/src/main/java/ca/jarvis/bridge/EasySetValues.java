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

    private static final Pattern NUMBER = Pattern.compile(
            "(?iu)^\\s*(\\d{1,4}(?:[.,]\\d{1,2})?)\\s*(%|mm\\s*/\\s*s|fois)?\\s*$");

    public static String diagnose(Collection<Line> scannedLines) {
        if (scannedLines == null) return EasySetLabels.diagnose(null);
        List<Line> lines = new ArrayList<>(scannedLines);
        List<String> texts = new ArrayList<>();
        for (Line line : lines) texts.add(line.text);
        String gate = EasySetLabels.diagnose(texts);
        if (!gate.startsWith("EasySet ")) return gate;

        String power = read(lines, Field.POWER);
        String speed = read(lines, Field.SPEED);
        String passes = read(lines, Field.PASSES);
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
            // Inline fields: "Puissance (%) : 50%" and "Vitesse (mm/s) 120".
            suffix = suffix.replaceFirst("(?iu)^\\s*\\(\\s*(?:%|mm\\s*/\\s*s)\\s*\\)", "").trim();
            suffix = suffix.replaceFirst("^[=:：]\\s*", "").trim();
            String matched = asValue(suffix, field);
            if (matched == null && suffix.isEmpty()) {
                matched = onSameRow(lines, label, field);
            }
            if (matched != null) {
                if (chosen != null && !chosen.equals(matched)) return null;
                chosen = matched;
            }
        }
        return chosen;
    }

    private static String onSameRow(List<Line> lines, Line label, Field field) {
        if (label.right <= label.left || label.bottom <= label.top) return null;
        int labelHeight = label.bottom - label.top;
        int midY = (label.top + label.bottom) / 2;
        String candidate = null;
        for (Line line : lines) {
            if (line == label || line.right <= line.left) continue;
            int otherMidY = (line.top + line.bottom) / 2;
            int tolerance = Math.max(12, labelHeight);
            // Only associate an isolated value to the RIGHT of the label
            // on the very same visual row. Never grab sheet dimensions or
            // values from other fields.
            if (Math.abs(otherMidY - midY) > tolerance) continue;
            if (line.left < label.right - 8) continue;
            String parsed = asValue(line.text, field);
            if (parsed == null) continue;
            if (candidate != null && !candidate.equals(parsed)) return null;
            candidate = parsed;
        }
        return candidate;
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
