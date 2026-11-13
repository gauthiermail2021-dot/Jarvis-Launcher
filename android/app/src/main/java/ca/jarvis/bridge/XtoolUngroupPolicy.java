package ca.jarvis.bridge;

import java.util.Locale;

/** Strict labels and bounded controls for XCS design actions. */
final class XtoolUngroupPolicy {
    private XtoolUngroupPolicy() { }

    static boolean isUngroupLabel(CharSequence label) {
        if (label == null) return false;
        String text = label.toString().trim().toLowerCase(Locale.ROOT);
        return "dégrouper".equals(text)
                || "degrouper".equals(text)
                || "ungroup".equals(text);
    }

    static boolean validBounds(int left, int top, int right, int bottom,
                               int screenWidth, int screenHeight) {
        int width = right - left, height = bottom - top;
        return screenWidth > 0 && screenHeight > 0
                && left >= 0 && top >= 0
                && right <= screenWidth && bottom <= screenHeight
                // The named XCS button is a whole icon+label tile
                // (observed 273x259 px), not merely a short text label.
                && width >= 35 && width <= 430
                && height >= 20 && height <= 330;
    }

    static boolean validControlBounds(
            int left, int top, int right, int bottom,
            int screenWidth, int screenHeight) {
        int width = right - left, height = bottom - top;
        return screenWidth > 0 && screenHeight > 0
                && left >= 0 && top >= 0
                && right <= screenWidth && bottom <= screenHeight
                && width >= 35 && width <= 430
                && height >= 20 && height <= 330;
    }

    /**
     * Safe fallback for a named (exact "Dégrouper") accessible node that
     * already encompasses its own icon+label tile. No arbitrary coordinates.
     */
    static boolean isNamedUngroupTile(int left, int top, int right, int bottom,
                                      int screenWidth, int screenHeight) {
        return validControlBounds(left, top, right, bottom, screenWidth, screenHeight)
                && right - left >= 85 && bottom - top >= 100;
    }

    /** Only use an icon+label tile whose accessible bounds contain the label. */
    static boolean isCompactIconTile(
            int textLeft, int textTop, int textRight, int textBottom,
            int left, int top, int right, int bottom,
            int screenWidth, int screenHeight) {
        if (!validControlBounds(left, top, right, bottom,
                screenWidth, screenHeight)) return false;
        int textHeight = textBottom - textTop;
        return textLeft >= left && textRight <= right
                && textTop >= top && textBottom <= bottom
                && top + 25 < textTop
                && bottom - top >= textHeight + 30
                && (top + bottom) / 2 < (textTop + textBottom) / 2;
    }
}
