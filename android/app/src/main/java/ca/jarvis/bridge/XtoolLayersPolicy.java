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

    /**
     * Only a tap inside the lower-right part of the DRAWING area is accepted.
     * The visible Conception tab marks the end of that area; the display
     * corner is not a drawing corner. Excludes the old top-right position
     * that unintentionally opened Réglages on the Samsung.
     */
    static boolean validDrawingBottomRightTap(int x, int y,
                                              int viewLeft, int viewTop,
                                              int viewRight, int designBottom,
                                              int screenWidth, int screenHeight) {
        int width = viewRight - viewLeft;
        int height = designBottom - viewTop;
        return screenWidth > 0 && screenHeight > 0
                && width > screenWidth * 65L / 100
                && height > screenHeight * 35L / 100
                && viewLeft >= 0 && viewRight <= screenWidth
                && viewTop >= 0 && designBottom < screenHeight * 85L / 100
                && x >= viewLeft + width * 80L / 100
                && x <= viewLeft + width * 95L / 100
                && y >= viewTop + height * 76L / 100
                && y <= viewTop + height * 94L / 100
                && y >= screenHeight * 48L / 100
                && y < designBottom;
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
