package ca.jarvis.bridge;

import java.util.Locale;

/** Named, screen-bounded XCS design UI targets; no fixed screen positions. */
final class XtoolUngroupPolicy {
    private XtoolUngroupPolicy() { }

    static boolean isUngroupLabel(CharSequence label) {
        if (label == null) return false;
        String text = label.toString().trim().toLowerCase(Locale.ROOT);
        return "dégrouper".equals(text)
                || "degrouper".equals(text) || "ungroup".equals(text);
    }

    static boolean validBounds(int left, int top, int right, int bottom,
                               int screenWidth, int screenHeight) {
        int width = right - left, height = bottom - top;
        return screenWidth > 0 && screenHeight > 0
                && left >= 0 && top >= 0
                && right <= screenWidth && bottom <= screenHeight
                && width >= 35 && width <= 650
                && height >= 20 && height <= 180;
    }

    static boolean validClickTargetBounds(
            int left, int top, int right, int bottom,
            int screenWidth, int screenHeight) {
        int width = right - left, height = bottom - top;
        return screenWidth > 0 && screenHeight > 0
                && left >= 0 && top >= 0
                && right <= screenWidth && bottom <= screenHeight
                && width >= 35 && width <= 430
                && height >= 20 && height <= 360;
    }

    /** Guard an icon+text tile inferred from the actual text's ancestors. */
    static boolean isCompactIconContainer(
            int textLeft, int textTop, int textRight, int textBottom,
            int left, int top, int right, int bottom,
            int screenWidth, int screenHeight) {
        int textHeight = textBottom - textTop;
        if (!validClickTargetBounds(left, top, right, bottom,
                screenWidth, screenHeight)) return false;
        return textLeft >= left && textRight <= right
                && textTop >= top && textBottom <= bottom
                && top + 25 < textTop
                && bottom - top >= textHeight + 30
                && (top + bottom) / 2 < (textTop + textBottom) / 2;
    }
}
