package ca.jarvis.bridge;

/**
 * Pure decision gate for a one-shot, consented read-only EasySet capture
 * when the xTool Creative Space window is brought to the foreground.
 * No window content, images or text are read by this policy.
 */
final class EasySetForegroundScanPolicy {
    static final String XCS_PACKAGE = "com.makeblock.xcs";
    static final long MIN_INTERVAL_MS = 15_000L;
    static final long PANEL_CHANGE_INTERVAL_MS = 1_200L;

    private EasySetForegroundScanPolicy() { }

    /**
     * Only after a consented OCR frame identified the closed xTool panel:
     * allow one event-driven retry on xTool's next content change.
     * A fast UI redraw is not evidence that it is open; the OCR must verify.
     */
    static boolean shouldRequestAfterPanelChange(String packageName,
            boolean authorizedProjection, boolean bubbleVisible,
            boolean awaitingOpen, long elapsedMs, long lastRequestMs) {
        return awaitingOpen
                && XCS_PACKAGE.equals(packageName)
                && authorizedProjection && bubbleVisible
                && elapsedMs >= lastRequestMs
                && elapsedMs - lastRequestMs >= PANEL_CHANGE_INTERVAL_MS;
    }


    static boolean shouldRequest(String packageName, boolean authorizedProjection,
                                 boolean bubbleVisible, long elapsedMs, long lastRequestMs) {
        return XCS_PACKAGE.equals(packageName)
                && authorizedProjection
                && bubbleVisible
                && elapsedMs >= lastRequestMs
                && elapsedMs - lastRequestMs >= MIN_INTERVAL_MS;
    }
}
