package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class XtoolUngroupPolicyTest {
    @Test public void matchesOnlyTheExplicitUngroupAction() {
        assertTrue(XtoolUngroupPolicy.isUngroupLabel("Dégrouper"));
        assertTrue(XtoolUngroupPolicy.isUngroupLabel(" DEGrouper "));
        assertTrue(XtoolUngroupPolicy.isUngroupLabel("Ungroup"));
        assertFalse(XtoolUngroupPolicy.isUngroupLabel("Grouper"));
        assertFalse(XtoolUngroupPolicy.isUngroupLabel("Dégrouper tout"));
        assertFalse(XtoolUngroupPolicy.isUngroupLabel("Couper"));
        assertFalse(XtoolUngroupPolicy.isUngroupLabel(null));
    }

    @Test public void allowsTheObservedFullUngroupButton() {
        // Android on Samsung returned [28,2004–301,2263] = 273x259 px.
        assertTrue(XtoolUngroupPolicy.validBounds(
                28, 2004, 301, 2263, 1080, 2400));
        assertTrue(XtoolUngroupPolicy.isNamedUngroupTile(
                28, 2004, 301, 2263, 1080, 2400));
        assertFalse(XtoolUngroupPolicy.isNamedUngroupTile(
                28, 2004, 301, 2263, 250, 2400));
        assertFalse(XtoolUngroupPolicy.validBounds(
                28, 2004, 800, 2263, 1080, 2400));
    }

    @Test public void rejectsInvalidOrOffscreenTouchBounds() {
        assertTrue(XtoolUngroupPolicy.validBounds(220, 2500, 450, 2590, 1440, 3088));
        assertFalse(XtoolUngroupPolicy.validBounds(-5, 2500, 450, 2590, 1440, 3088));
        assertFalse(XtoolUngroupPolicy.validBounds(220, 2500, 450, 3500, 1440, 3088));
        assertFalse(XtoolUngroupPolicy.validBounds(220, 2500, 1400, 2590, 1440, 3088));
        assertFalse(XtoolUngroupPolicy.validBounds(220, 2500, 240, 2590, 1440, 3088));
    }
}
