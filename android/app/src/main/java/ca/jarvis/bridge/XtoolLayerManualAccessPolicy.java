package ca.jarvis.bridge;

/**
 * A manual xTool navigation command is a direct, user-initiated operation:
 * requiring a live foreground xTool window is sufficient even if the SVG
 * session was explicitly ended. Automatic navigation still requires both.
 * Does not change the Gmail/session lifecycle or any machine parameters.
 */
final class XtoolLayerManualAccessPolicy {
    private XtoolLayerManualAccessPolicy() { }

    static boolean permitted(boolean isXtoolForeground,
                             boolean svgSessionActive,
                             boolean directUserCommand) {
        return isXtoolForeground && (svgSessionActive || directUserCommand);
    }
}
