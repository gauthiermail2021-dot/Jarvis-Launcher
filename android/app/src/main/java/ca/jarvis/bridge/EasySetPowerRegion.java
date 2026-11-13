package ca.jarvis.bridge;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Field-specific, read-only crop of the xTool EasySet power numeric cell.
 * Pure Java so its limits can be verified without Android.
 *
 * Unlike "Passer", "Puissance" has a label ABOVE the numeric slider row.
 * The crop deliberately excludes the slider and the light-gray chevron,
 * and never crosses the next Vitesse field heading.
 */
final class EasySetPowerRegion {
    private static final Pattern LABEL = Pattern.compile(
            "(?iu)^\\s*(?:puissance|power)\\b");
    private static final Pattern SPEED = Pattern.compile(
            "(?iu)^\\s*(?:vitesse|speed)\\b");

    final int left, top, right, bottom;

    private EasySetPowerRegion(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    int width() { return right - left; }
    int height() { return bottom - top; }

    static EasySetPowerRegion find(Collection<EasySetValues.Line> lines,
                                    int screenWidth, int screenHeight) {
        if (lines == null || screenWidth < 280 || screenHeight < 300)
            return null;

        EasySetValues.Line heading = null;
        for (EasySetValues.Line item : lines) {
            if (item == null || !LABEL.matcher(item.text).find()) continue;
            int h = item.bottom - item.top;
            if (h < 8 || h > screenHeight / 5 || item.right <= item.left)
                continue;
            if (item.top < 0 || item.bottom > screenHeight
                    || item.left > screenWidth * 0.50f) continue;
            // Prefer the last valid label if another heading is present.
            if (heading == null || item.top > heading.top) heading = item;
        }
        if (heading == null) return null;

        int h = heading.bottom - heading.top;
        int top = Math.max(0, heading.bottom + Math.max(6, Math.round(h * 0.25f)));
        int bottom = Math.min(screenHeight,
                heading.bottom + Math.max(65, Math.round(h * 2.15f)));
        for (EasySetValues.Line line : lines) {
            if (line != null && SPEED.matcher(line.text).find()
                    && line.top > heading.bottom) {
                bottom = Math.min(bottom, line.top - Math.max(8, h / 3));
            }
        }
        // This band does not touch the far-left green slider or the chevron.
        int left = Math.max(0, Math.round(screenWidth * 0.80f));
        int right = Math.min(screenWidth, Math.round(screenWidth * 0.91f));
        if (left <= heading.right || right - left < 12
                || bottom - top < 12 || bottom > screenHeight) return null;

        // The crop must be small enough for EasySetPassesTesseract's
        // isolated numeric-field protection (<= 180 x 128 pixels).
        if (right - left > 180 || bottom - top > 128) return null;
        return new EasySetPowerRegion(left, top, right, bottom);
    }
}
