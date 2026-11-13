package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class XtoolLayersPolicyTest {
    @Test public void onlyNamedLayersAreMatched() {
        assertTrue(XtoolLayersPolicy.exactLayersLabel("Calques"));
        assertTrue(XtoolLayersPolicy.exactLayersLabel(" Calque "));
        assertTrue(XtoolLayersPolicy.exactLayersLabel("Layers"));
        assertFalse(XtoolLayersPolicy.exactLayersLabel("Organiser"));
        assertFalse(XtoolLayersPolicy.exactLayersLabel("Couleur de calque"));
        assertFalse(XtoolLayersPolicy.exactLayersLabel("Configuration"));
        assertFalse(XtoolLayersPolicy.exactLayersLabel(null));
    }

    @Test public void bottomBoundedButtonOnly() {
        assertTrue(XtoolLayersPolicy.validBottomButton(
                30, 2700, 260, 2840, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validBottomButton(
                30, 100, 260, 240, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validBottomButton(
                30, 2700, 1400, 2840, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validBottomButton(
                30, 2700, 260, 3250, 1440, 3088));
        assertTrue(XtoolLayersPolicy.validNamedIconTile(
                30, 2700, 260, 2840, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validNamedIconTile(
                30, 2700, 80, 2725, 1440, 3088));
    }

    @Test public void blankCornerOnlyInsideKnownDesignCanvas() {
        assertTrue(XtoolLayersPolicy.validBlankCanvasCorner(
                1353, 620, 1440, 3088, 1633));
        assertFalse(XtoolLayersPolicy.validBlankCanvasCorner(
                1353, 120, 1440, 3088, 1633));
        assertFalse(XtoolLayersPolicy.validBlankCanvasCorner(
                800, 620, 1440, 3088, 1633));
        assertFalse(XtoolLayersPolicy.validBlankCanvasCorner(
                1353, 620, 1440, 3088, 200));
    }

    @Test public void onlyPopupSizedMenuMayBeDismissed() {
        assertTrue(XtoolLayersPolicy.validMenuToDismiss(
                30, 1450, 700, 2200, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validMenuToDismiss(
                0, 0, 1440, 3088, 1440, 3088));
    }
}
