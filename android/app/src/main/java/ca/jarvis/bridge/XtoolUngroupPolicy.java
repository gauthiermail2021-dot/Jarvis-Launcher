package ca.jarvis.bridge;

import java.util.Locale;

/** Exact label and visible bounds guard for the XCS ungroup UI action. */
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
                && height >= 20 && height <= 180
                && left + width / 2 < screenWidth
                && top + height / 2 < screenHeight;
    }
}
