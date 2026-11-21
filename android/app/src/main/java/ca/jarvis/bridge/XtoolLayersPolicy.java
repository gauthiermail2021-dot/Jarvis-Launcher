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

    /**
     * A user-marked upper-right WHITE board spot while the Conception
     * bottom sheet is open. The screenshot's selected Conception label is
     * around the middle of the screen, with the marked point near 93% of
     * window width and 48% of the label's top edge.
     *
     * Always REFUSE if the sheet isn't in the expected upper-half/middle
     * position. This is an observed test-SVG position, not a generic free
     * canvas detector for arbitrary drawings.
     */
    static boolean validMarkedBlankCanvasTap(int x, int y,
                                             int viewLeft, int viewTop,
                                             int viewRight, int conceptionTop,
                                             int screenWidth, int screenHeight) {
        int width = viewRight - viewLeft;
        int canvasHeight = conceptionTop - viewTop;
        if (screenWidth <= 0 || screenHeight <= 0 || width <= 0
                || canvasHeight <= 0) return false;
        return viewLeft >= 0 && viewTop >= 0
                && viewRight <= screenWidth
                && conceptionTop <= screenHeight
                && width > screenWidth * 70L / 100
                && viewTop < screenHeight * 7L / 100
                // Must really be the expanded Conception sheet, not a footer.
                && conceptionTop >= screenHeight * 39L / 100
                && conceptionTop <= screenHeight * 64L / 100
                && x >= viewLeft + width * 90L / 100
                && x <= viewLeft + width * 96L / 100
                && y >= viewTop + canvasHeight * 46L / 100
                && y <= viewTop + canvasHeight * 50L / 100
                // Never touch the controls, rulers, or grey-bottom zone.
                && y >= screenHeight * 18L / 100
                && y < screenHeight * 34L / 100
                && y < conceptionTop - screenHeight * 8L / 100;
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
