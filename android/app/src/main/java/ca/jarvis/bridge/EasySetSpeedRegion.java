package ca.jarvis.bridge;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Read-only numeric crop for the xTool EasySet speed slider.
 *
 * The speed number is located on a row BELOW the "Vitesse" heading,
 * unlike the Passer number on its heading row. The crop only targets
 * the right-hand editable numeric cell, never the slider or next section.
 */
final class EasySetSpeedRegion {
    private static final Pattern LABEL = Pattern.compile(
            "(?iu)^\\s*(?:vitesse|speed)\\b");
    private static final Pattern PASSES = Pattern.compile(
            "(?iu)^\\s*(?:passer|passages|passes|pass count)\\b");

    final int left, top, right, bottom;

    private EasySetSpeedRegion(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    int width() { return right - left; }
    int height() { return bottom - top; }

    static EasySetSpeedRegion find(Collection<EasySetValues.Line> lines,
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
            if (heading == null || item.top > heading.top) heading = item;
        }
        if (heading == null) return null;

        int h = heading.bottom - heading.top;
        int top = Math.max(0, heading.bottom + Math.max(6, Math.round(h * 0.25f)));
        int bottom = Math.min(screenHeight,
                heading.bottom + Math.max(65, Math.round(h * 2.15f)));
        for (EasySetValues.Line item : lines) {
            if (item != null && PASSES.matcher(item.text).find()
                    && item.top > heading.bottom) {
                bottom = Math.min(bottom, item.top - Math.max(8, h / 3));
            }
        }
        int left = Math.max(0, Math.round(screenWidth * 0.80f));
        int right = Math.min(screenWidth, Math.round(screenWidth * 0.91f));
        if (left <= heading.right || right - left < 12
                || bottom - top < 12 || bottom > screenHeight)
            return null;
        // Tesseract accepts an isolated numeric field up to 180x128 px.
        if (right - left > 180 || bottom - top > 128) return null;
        return new EasySetSpeedRegion(left, top, right, bottom);
    }
}
