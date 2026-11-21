package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class XtoolLayerStripPolicyTest {
    @Test public void samsungClippedCalquesRequiresSwipe() {
        // Actual accessibility rectangle from the 2026-11-21 Samsung report.
        assertTrue(XtoolLayerStripPolicy.clippedAtRight(
                1288, 2718, 1440, 2907, 1440, 3088));
        assertFalse(XtoolLayerStripPolicy.fullyRevealed(
                1288, 2718, 1440, 2907, 1440, 3088));
    }

    @Test public void centeredButtonMayBeTouchedOnlyAfterReveal() {
        assertTrue(XtoolLayerStripPolicy.fullyRevealed(
                620, 2718, 770, 2907, 1440, 3088));
        assertFalse(XtoolLayerStripPolicy.clippedAtRight(
                620, 2718, 770, 2907, 1440, 3088));
    }

    @Test public void rejectCanvasAndWrongToolbar() {
        assertFalse(XtoolLayerStripPolicy.clippedAtRight(
                1288, 900, 1440, 1000, 1440, 3088));
        assertFalse(XtoolLayerStripPolicy.fullyRevealed(
                620, 800, 770, 989, 1440, 3088));
        assertFalse(XtoolLayerStripPolicy.clippedAtRight(
                1288, 2718, 1440, 3098, 1440, 3088));
    }

    @Test public void namedToolbarAnchorCanRevealOffscreenCalque() {
        assertTrue(XtoolLayerStripPolicy.validToolbarAnchor(
                550, 2740, 740, 2900, 1440, 3088));
        assertFalse(XtoolLayerStripPolicy.validToolbarAnchor(
                550, 900, 740, 1080, 1440, 3088));
        assertFalse(XtoolLayerStripPolicy.validToolbarAnchor(
                1350, 2740, 1480, 2900, 1440, 3088));
    }

    @Test public void horizontalGestureMustStayInObservedBottomStrip() {
        // x=79% was on xTool's fixed right-hand Settings tile.
        assertTrue(XtoolLayerStripPolicy.safeSwipe(
                1008, 129, 2812, 1440, 3088, 2718, 2907));
        assertFalse(XtoolLayerStripPolicy.safeSwipe(
                979, 288, 2812, 1440, 3088, 2718, 2907));
        assertFalse(XtoolLayerStripPolicy.safeSwipe(
                1137, 129, 2812, 1440, 3088, 2718, 2907));
        assertFalse(XtoolLayerStripPolicy.safeSwipe(
                1008, 129, 620, 1440, 3088, 2718, 2907));
        assertFalse(XtoolLayerStripPolicy.safeSwipe(
                129, 1008, 2812, 1440, 3088, 2718, 2907));
    }
}
