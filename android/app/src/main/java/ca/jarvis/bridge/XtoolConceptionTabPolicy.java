package ca.jarvis.bridge;

import java.util.Locale;

/** Narrow target for the XCS design tab. No laser settings are touched. */
final class XtoolConceptionTabPolicy {
    private XtoolConceptionTabPolicy() { }

    static boolean isConception(CharSequence label) {
        return label != null &&
                "conception".equals(label.toString().trim().toLowerCase(Locale.ROOT));
    }

    static boolean validBounds(int left, int top, int right, int bottom,
                               int screenWidth, int screenHeight) {
        int width = right - left;
        int height = bottom - top;
        return screenWidth > 0 && screenHeight > 0
                && left >= 0 && top >= 0
                && right <= screenWidth && bottom <= screenHeight
                && width >= 25 && width <= 750
                && height >= 20 && height <= 240;
    }
}
