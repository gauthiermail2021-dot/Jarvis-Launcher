package ca.jarvis.bridge;

/**
 * Text-based panel evidence only: at least two distinct, visible color
 * labels. Not a guarantee of the actual SVG/layer processing settings.
 */
final class XtoolLayerPanelEvidencePolicy {
    private XtoolLayerPanelEvidencePolicy() { }

    static boolean hasMultipleColors(boolean red, boolean green, boolean blue) {
        return (red ? 1 : 0) + (green ? 1 : 0) + (blue ? 1 : 0) >= 2;
    }
}
