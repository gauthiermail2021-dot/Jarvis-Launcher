package ca.jarvis.bridge;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/** Target-selection policy only; never invokes xTool or a laser. */
public class XtoolUngroupControlTest {
    @Test public void matchesOnlyTheExactUngroupLabel() {
        assertTrue(XtoolUngroupPolicy.isUngroupLabel("Dégrouper"));
        assertTrue(XtoolUngroupPolicy.isUngroupLabel(" DEGrouper "));
        assertTrue(XtoolUngroupPolicy.isUngroupLabel("Ungroup"));
        assertFalse(XtoolUngroupPolicy.isUngroupLabel("Grouper"));
        assertFalse(XtoolUngroupPolicy.isUngroupLabel("Dégrouper tout"));
        assertFalse(XtoolUngroupPolicy.isUngroupLabel("EasySet"));
        assertFalse(XtoolUngroupPolicy.isUngroupLabel(null));
    }

    @Test public void clickableTargetIsSmallAndOnScreen() {
        assertTrue(XtoolUngroupPolicy.validControlBounds(
                50, 1900, 240, 2130, 1440, 3088));
        assertFalse(XtoolUngroupPolicy.validControlBounds(
                0, 1900, 1440, 2250, 1440, 3088));
        assertFalse(XtoolUngroupPolicy.validControlBounds(
                -8, 1900, 240, 2130, 1440, 3088));
        assertFalse(XtoolUngroupPolicy.validControlBounds(
                50, 1900, 240, 3260, 1440, 3088));
    }

    @Test public void namedExactButtonAreaRequiresIconSizedBounds() {
        assertTrue(XtoolUngroupPolicy.isNamedUngroupTile(
                28, 2004, 301, 2263, 1080, 2400));
        assertFalse(XtoolUngroupPolicy.isNamedUngroupTile(
                28, 2100, 120, 2130, 1080, 2400));
        assertFalse(XtoolUngroupPolicy.isNamedUngroupTile(
                28, 2004, 700, 2263, 1080, 2400));
    }

    @Test public void acceptsOnlyCompactIconAboveItsOwnText() {
        assertTrue(XtoolUngroupPolicy.isCompactIconTile(
                70, 2050, 200, 2080,
                50, 1900, 220, 2100, 1440, 3088));
        assertFalse(XtoolUngroupPolicy.isCompactIconTile(
                70, 2050, 200, 2080,
                65, 2040, 210, 2090, 1440, 3088));
        assertFalse(XtoolUngroupPolicy.isCompactIconTile(
                70, 2050, 200, 2080,
                0, 1900, 1440, 2200, 1440, 3088));
    }
}
