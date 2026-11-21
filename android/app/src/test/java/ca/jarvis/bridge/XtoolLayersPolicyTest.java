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

    @Test public void drawingCornerNeverEqualsOldSettingsCorner() {
        assertTrue(XtoolLayersPolicy.validDrawingBottomRightTap(
                1281, 1680, 0, 0, 1440, 2000, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validDrawingBottomRightTap(
                1353, 620, 0, 0, 1440, 2000, 1440, 3088));
    }

    @Test public void userMarkedUpperRightCanvasPointIsGuarded() {
        // Galaxy-class display: bottom Conception toolbar at y=2800,
        // target x=86% of 1440 and y=30% of the editing region.
        assertTrue(XtoolLayersPolicy.validMarkedBlankCanvasTap(
                1238, 840, 0, 0, 1440, 2800, 1440, 3088));
        // A historic corner test above the canvas must not pass.
        assertFalse(XtoolLayersPolicy.validMarkedBlankCanvasTap(
                1353, 620, 0, 0, 1440, 2800, 1440, 3088));
        // Bottom grey region, far from the screenshot-marked white spot.
        assertFalse(XtoolLayersPolicy.validMarkedBlankCanvasTap(
                1238, 2250, 0, 0, 1440, 2800, 1440, 3088));
        // A bottom toolbar tile is never a valid deselect target.
        assertFalse(XtoolLayersPolicy.validMarkedBlankCanvasTap(
                1238, 2800, 0, 0, 1440, 2800, 1440, 3088));
        // Missing editor bounds must never authorize an arbitrary screen tap.
        assertFalse(XtoolLayersPolicy.validMarkedBlankCanvasTap(
                1238, 840, 0, 0, 1440, 1000, 1440, 3088));
    }

    @Test public void onlyPopupSizedMenuMayBeDismissed() {
        assertTrue(XtoolLayersPolicy.validMenuToDismiss(
                30, 1450, 700, 2200, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validMenuToDismiss(
                0, 0, 1440, 3088, 1440, 3088));
    }
}
