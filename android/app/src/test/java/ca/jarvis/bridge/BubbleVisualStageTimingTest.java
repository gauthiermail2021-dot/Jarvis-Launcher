package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public final class BubbleVisualStageTimingTest {
    @Test public void eachAlreadyOpenStageHasHalfSecondReadableHold() {
        assertEquals(500L, BubbleVisualStageTiming.holdMillis(true, false));
    }

    @Test public void firstStageHasExtraTimeForAnimatedExpansion() {
        assertEquals(1000L, BubbleVisualStageTiming.holdMillis(true, true));
        assertEquals(500L, BubbleVisualStageTiming.OPENING_ANIMATION_MS);
        assertEquals(500L, BubbleVisualStageTiming.MIN_VISIBLE_MS);
    }

    @Test public void disablingVisualAnimationsAddsNoDelay() {
        assertEquals(0L, BubbleVisualStageTiming.holdMillis(false, false));
        assertEquals(0L, BubbleVisualStageTiming.holdMillis(false, true));
    }
}
