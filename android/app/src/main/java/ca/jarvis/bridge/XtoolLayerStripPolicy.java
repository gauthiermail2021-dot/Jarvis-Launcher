package ca.jarvis.bridge;

/**
 * Geometrical gates for the xTool bottom navigation strip.
 * All coordinates are computed from a live, EXACT "Calque(s)" accessibility
 * label. These checks never authorize a canvas tap or laser action.
 */
final class XtoolLayerStripPolicy {
    private XtoolLayerStripPolicy() { }

    static boolean clippedAtRight(int left, int top, int right, int bottom,
                                  int screenWidth, int screenHeight) {
        return XtoolLayersPolicy.validNamedIconTile(
                left, top, right, bottom, screenWidth, screenHeight)
                && left >= screenWidth * 66L / 100
                && right >= screenWidth * 91L / 100
                && top >= screenHeight * 72L / 100
                && bottom <= screenHeight * 97L / 100;
    }

    static boolean fullyRevealed(int left, int top, int right, int bottom,
                                 int screenWidth, int screenHeight) {
        return XtoolLayersPolicy.validNamedIconTile(
                left, top, right, bottom, screenWidth, screenHeight)
                && left >= screenWidth * 10L / 100
                && right <= screenWidth * 88L / 100
                && top >= screenHeight * 72L / 100
                && bottom <= screenHeight * 97L / 100;
    }

    static boolean validToolbarAnchor(int left, int top, int right, int bottom,
                                      int screenWidth, int screenHeight) {
        return XtoolLayersPolicy.validBottomButton(
                left, top, right, bottom, screenWidth, screenHeight)
                && left >= screenWidth * 8L / 100
                && right <= screenWidth * 92L / 100
                && top >= screenHeight * 72L / 100
                && bottom <= screenHeight * 97L / 100;
    }

    static boolean safeSwipe(int startX, int endX, int y,
                             int screenWidth, int screenHeight,
                             int labelTop, int labelBottom) {
        return screenWidth > 0 && screenHeight > 0
                // The fixed Settings tile begins near 78% on Samsung.
                // Never start a scroll over that non-scrollable zone.
                && startX >= screenWidth * 60L / 100
                && startX <= screenWidth * 73L / 100
                && endX >= screenWidth * 15L / 100
                && endX <= screenWidth * 30L / 100
                && startX - endX > screenWidth * 35L / 100
                && y >= labelTop && y <= labelBottom
                && y >= screenHeight * 72L / 100
                && y <= screenHeight * 96L / 100;
    }
}
