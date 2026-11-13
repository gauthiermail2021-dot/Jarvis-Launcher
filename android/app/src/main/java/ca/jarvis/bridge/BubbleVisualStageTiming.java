package ca.jarvis.bridge;

/**
 * Pure timing constants for the passive Jarvis Gmail-to-SVG visualization.
 * This adds no delay to network work, SVG imports, or xTool.
 */
final class BubbleVisualStageTiming {
    static final long MIN_VISIBLE_MS = 500L;
    static final long OPENING_ANIMATION_MS = 500L;

    private BubbleVisualStageTiming() { }

    static long holdMillis(boolean animationsEnabled, boolean openingFromSmall) {
        if (!animationsEnabled) return 0L;
        // A label cannot be read during the 54 -> 110 dp bubble expansion.
        // Start the 500 ms readable hold AFTER that animation.
        return MIN_VISIBLE_MS + (openingFromSmall ? OPENING_ANIMATION_MS : 0L);
    }
}
