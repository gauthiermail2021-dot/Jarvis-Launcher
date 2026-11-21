package ca.jarvis.bridge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** 1.2.73: only the bottom-right of the drawing canvas, not Réglages. */
public final class XtoolDeselectPolicyTest {
    @Test public void acceptsRightCornerInsideDrawingPage() {
        assertTrue(XtoolLayersPolicy.validDrawingBottomRightTap(
                1281, 1680, 0, 0, 1440, 2000, 1440, 3088));
    }

    @Test public void rejectsOldSettingsCornerAndToolbar() {
        assertFalse(XtoolLayersPolicy.validDrawingBottomRightTap(
                1353, 760, 0, 0, 1440, 2000, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validDrawingBottomRightTap(
                1281, 2800, 0, 0, 1440, 2000, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validDrawingBottomRightTap(
                720, 1680, 0, 0, 1440, 2000, 1440, 3088));
    }

    @Test public void unknownDrawingBoundsAlwaysFailClosed() {
        assertFalse(XtoolLayersPolicy.validDrawingBottomRightTap(
                1281, 1680, 0, 0, 1440, 0, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validDrawingBottomRightTap(
                1281, 1680, 0, 0, 1440, 2950, 1440, 3088));
    }
}
