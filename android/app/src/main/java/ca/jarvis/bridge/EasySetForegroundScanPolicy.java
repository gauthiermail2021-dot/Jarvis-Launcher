package ca.jarvis.bridge;

/**
 * Pure decision gate for a one-shot, consented read-only EasySet capture
 * when the xTool Creative Space window is brought to the foreground.
 * No window content, images or text are read by this policy.
 */
final class EasySetForegroundScanPolicy {
    static final String XCS_PACKAGE = "com.makeblock.xcs";
    static final long MIN_INTERVAL_MS = 15_000L;

    private EasySetForegroundScanPolicy() { }

    static boolean shouldRequest(String packageName, boolean authorizedProjection,
                                 boolean bubbleVisible, long elapsedMs, long lastRequestMs) {
        return XCS_PACKAGE.equals(packageName)
                && authorizedProjection
                && bubbleVisible
                && elapsedMs >= lastRequestMs
                && elapsedMs - lastRequestMs >= MIN_INTERVAL_MS;
    }
}
