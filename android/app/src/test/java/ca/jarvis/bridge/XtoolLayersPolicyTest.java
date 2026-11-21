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

    @Test public void userMarkedCyanSpotInNewSamsungScreenshot() {
        // Screenshot supplied by the user: 879 x 1536, teal target near
        // (817, 374), Conception label beginning around y=780.
        assertTrue(XtoolLayersPolicy.validMarkedBlankCanvasTap(
                817, 374, 0, 0, 879, 780, 879, 1536));
        // The old 1.2.86 computed point at 86% / 30% misses that target.
        assertFalse(XtoolLayersPolicy.validMarkedBlankCanvasTap(
                756, 234, 0, 0, 879, 780, 879, 1536));
        // Red-circled Dégrouper tool within the Conception bottom sheet.
        assertFalse(XtoolLayersPolicy.validMarkedBlankCanvasTap(
                460, 1480, 0, 0, 879, 780, 879, 1536));
        // Grey region below the white canvas, not a blank board target.
        assertFalse(XtoolLayersPolicy.validMarkedBlankCanvasTap(
                817, 600, 0, 0, 879, 780, 879, 1536));
        // The Conception tab may be in the page footer on other screens:
        // never use a guessed point when there is no expanded bottom sheet.
        assertFalse(XtoolLayersPolicy.validMarkedBlankCanvasTap(
                817, 1344, 0, 0, 879, 2800, 879, 3088));
    }

    @Test public void correspondingFullResolutionSamsungPoint() {
        assertTrue(XtoolLayersPolicy.validMarkedBlankCanvasTap(
                1339, 768, 0, 0, 1440, 1600, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validMarkedBlankCanvasTap(
                1339, 768, 0, 0, 1440, 200, 1440, 3088));
    }

    @Test public void onlyPopupSizedMenuMayBeDismissed() {
        assertTrue(XtoolLayersPolicy.validMenuToDismiss(
                30, 1450, 700, 2200, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validMenuToDismiss(
                0, 0, 1440, 3088, 1440, 3088));
    }
}
