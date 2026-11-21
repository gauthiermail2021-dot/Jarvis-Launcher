package ca.jarvis.bridge;

/**
 * The home-screen TEST SVG entry point must be exported for Android launchers
 * and can therefore be opened by other apps. Never send Gmail on its launch.
 *
 * The separate, non-exported JarvisSilentSvgSendActivity can still send on a
 * direct tap from the in-app SVG bubble.
 */
final class JarvisTestSendPolicy {
    private JarvisTestSendPolicy() { }

    static boolean autoSendOnActivityLaunch(boolean privateBubbleEntryPoint) {
        return privateBubbleEntryPoint;
    }
}
