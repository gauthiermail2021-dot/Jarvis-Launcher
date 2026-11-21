package ca.jarvis.bridge;

/**
 * Select exactly one way to activate a single identified bottom Calque tile.
 * Prefer a real Android ACTION_CLICK, using the named bounded touch only
 * when xTool exposes no clickable accessibility target.
 */
final class XtoolLayerActivationPolicy {
    enum Strategy { NONE, ACTION_CLICK, NAMED_TOUCH }

    private XtoolLayerActivationPolicy() { }

    static Strategy strategy(int exactLabels, int visibleLabels,
                             int boundedTiles, int clickableTargets) {
        if (exactLabels != 1 || visibleLabels != 1 || boundedTiles != 1)
            return Strategy.NONE;
        if (clickableTargets == 1) return Strategy.ACTION_CLICK;
        if (clickableTargets == 0) return Strategy.NAMED_TOUCH;
        return Strategy.NONE;
    }
}
