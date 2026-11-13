package ca.jarvis.bridge;

import java.util.Locale;

/** Narrowly targets the xTool Layers button; never processing controls. */
final class XtoolLayersPolicy {
    private XtoolLayersPolicy() { }

    static boolean exactLayersLabel(CharSequence label) {
        if (label == null) return false;
        String word = label.toString().trim().toLowerCase(Locale.ROOT);
        return "calque".equals(word) || "calques".equals(word)
                || "layer".equals(word) || "layers".equals(word);
    }

    /** The actual bottom bar has to be visible, compact, and on-screen. */
    static boolean validBottomButton(int left, int top, int right, int bottom,
                                     int screenWidth, int screenHeight) {
        int w = right - left, h = bottom - top;
        return screenWidth > 0 && screenHeight > 0
                && left >= 0 && right <= screenWidth
                && top >= 0 && bottom <= screenHeight
                && w >= 28 && w <= 520 && h >= 20 && h <= 330
                && top + h / 2 >= screenHeight * 55L / 100;
    }

    static boolean validNamedIconTile(int left, int top, int right, int bottom,
                                      int screenWidth, int screenHeight) {
        return validBottomButton(left, top, right, bottom, screenWidth, screenHeight)
                && right - left >= 55 && bottom - top >= 50;
    }

    /** Dismiss a nearby contextual popup, not a full-screen editor surface. */
    static boolean validMenuToDismiss(int left, int top, int right, int bottom,
                                      int screenWidth, int screenHeight) {
        return screenWidth > 0 && screenHeight > 0
                && left >= 0 && top >= 0 && right <= screenWidth
                && bottom <= screenHeight
                && right - left >= 85 && bottom - top >= 70
                && bottom - top < screenHeight * 70L / 100;
    }
}
