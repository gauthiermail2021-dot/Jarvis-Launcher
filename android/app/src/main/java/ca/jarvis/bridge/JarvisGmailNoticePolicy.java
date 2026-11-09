package ca.jarvis.bridge;

import java.util.Locale;

/** No Gmail notification means a valid SVG until its attachment is checked. */
final class JarvisGmailNoticePolicy {
    private JarvisGmailNoticePolicy() { }

    /** Only offer a manual fallback alert when the notification hints at Jarvis/SVG.
     * Automatic SVG checks still use the Gmail label and do not depend on this hint.
     */
    static boolean isJarvisHint(String title, String body) {
        String visible = (title == null ? "" : title) + " "
                + (body == null ? "" : body);
        String normalized = visible.toLowerCase(Locale.ROOT);
        return normalized.contains("jarvis-svg-auto")
                || normalized.contains("jarvis svg")
                || normalized.contains(".svg");
    }
}
