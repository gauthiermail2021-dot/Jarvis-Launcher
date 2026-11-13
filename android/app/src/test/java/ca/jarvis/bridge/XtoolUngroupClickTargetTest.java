package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class XtoolUngroupClickTargetTest {
    @Test public void exactNamedControlOnly() {
        assertTrue(XtoolUngroupPolicy.isUngroupLabel("Dégrouper"));
        assertFalse(XtoolUngroupPolicy.isUngroupLabel("Grouper"));
        assertFalse(XtoolUngroupPolicy.isUngroupLabel("Déplacer vers"));
    }

    @Test public void acceptsSmallClickableActionTilesNotWholePanel() {
        assertTrue(XtoolUngroupPolicy.validClickTargetBounds(
                45, 1900, 225, 2110, 1440, 3088));
        assertFalse(XtoolUngroupPolicy.validClickTargetBounds(
                0, 1800, 1440, 2450, 1440, 3088));
        assertFalse(XtoolUngroupPolicy.validClickTargetBounds(
                -20, 1900, 225, 2110, 1440, 3088));
    }

    @Test public void compactIconAboveLabelCanBeChosen() {
        assertTrue(XtoolUngroupPolicy.isCompactIconContainer(
                60, 2050, 205, 2085,
                42, 1880, 235, 2100, 1440, 3088));
        // A text-only parent gives no evidence of an icon tile.
        assertFalse(XtoolUngroupPolicy.isCompactIconContainer(
                60, 2050, 205, 2085,
                50, 2035, 218, 2100, 1440, 3088));
        // A full-width toolbar must not be guessed as a click target.
        assertFalse(XtoolUngroupPolicy.isCompactIconContainer(
                60, 2050, 205, 2085,
                0, 1770, 1440, 2180, 1440, 3088));
    }
}
