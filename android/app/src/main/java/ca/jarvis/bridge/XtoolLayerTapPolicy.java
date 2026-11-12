package ca.jarvis.bridge;

import java.util.Locale;

/**
 * Small pure-Java safety checks for a single named red-layer UI selection.
 * These checks are independent of Android screen coordinates.
 */
final class XtoolLayerTapPolicy {
    private XtoolLayerTapPolicy() { }

    static boolean exactRed(String label) {
        return label != null
                && "rouge".equals(label.trim().toLowerCase(Locale.ROOT));
    }

    static boolean isValidTarget(int left, int top, int right, int bottom,
                                 int screenWidth, int screenHeight) {
        if (screenWidth <= 0 || screenHeight <= 0) return false;
        int width = right - left;
        int height = bottom - top;
        if (width < 40 || width > 500 || height < 24 || height > 210) return false;
        if (left < 0 || top < 0 || right > screenWidth
                || bottom > screenHeight) return false;
        int midX = left + width / 2;
        int midY = top + height / 2;
        return midX > 0 && midX < screenWidth
                && midY > screenHeight / 3 && midY < screenHeight;
    }
}
