package ca.jarvis.bridge;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XtoolLayerTapPolicyTest {
    @Test public void exactRedAcceptsOnlyColorLabel() {
        assertTrue(XtoolLayerTapPolicy.exactRed("rouge"));
        assertTrue(XtoolLayerTapPolicy.exactRed(" Rouge "));
        assertFalse(XtoolLayerTapPolicy.exactRed("Calque rouge"));
        assertFalse(XtoolLayerTapPolicy.exactRed("vert"));
        assertFalse(XtoolLayerTapPolicy.exactRed(null));
    }

    @Test public void onlyExactHeadingAndUniqueRedMayBeTargeted() {
        assertTrue(XtoolLayerTapPolicy.exactLayerHeading("Calque"));
        assertTrue(XtoolLayerTapPolicy.exactLayerHeading("Calques"));
        assertTrue(XtoolLayerTapPolicy.exactLayerHeading(" Layers "));
        assertFalse(XtoolLayerTapPolicy.exactLayerHeading("Paramètres"));
        assertFalse(XtoolLayerTapPolicy.exactLayerHeading("Couleur de calque"));
        assertFalse(XtoolLayerTapPolicy.exactLayerHeading(null));

        assertTrue(XtoolLayerTapPolicy.uniqueRedTarget(1));
        assertFalse(XtoolLayerTapPolicy.uniqueRedTarget(0));
        assertFalse(XtoolLayerTapPolicy.uniqueRedTarget(2));
        assertFalse(XtoolLayerTapPolicy.uniqueRedTarget(3));
    }

    @Test public void boundsAreStrictAndVisible() {
        assertTrue(XtoolLayerTapPolicy.isValidTarget(
                213, 2774, 399, 2865, 1440, 3088));
        assertFalse(XtoolLayerTapPolicy.isValidTarget(
                -5, 2774, 399, 2865, 1440, 3088));
        assertFalse(XtoolLayerTapPolicy.isValidTarget(
                213, 2774, 399, 4000, 1440, 3088));
        assertFalse(XtoolLayerTapPolicy.isValidTarget(
                200, 200, 400, 280, 1440, 3088));
        assertFalse(XtoolLayerTapPolicy.isValidTarget(
                213, 2774, 213, 2865, 1440, 3088));
    }
}
