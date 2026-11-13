package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class XtoolConceptionTabPolicyTest {
    @Test public void matchesOnlyTheExactDesignTab() {
        assertTrue(XtoolConceptionTabPolicy.isConception("Conception"));
        assertTrue(XtoolConceptionTabPolicy.isConception(" CONCEPTION "));
        assertFalse(XtoolConceptionTabPolicy.isConception("Conception et découpe"));
        assertFalse(XtoolConceptionTabPolicy.isConception("EasySet"));
        assertFalse(XtoolConceptionTabPolicy.isConception("Découpe"));
        assertFalse(XtoolConceptionTabPolicy.isConception(null));
    }

    @Test public void acceptsVisibleTabNotGuessedCoordinates() {
        assertTrue(XtoolConceptionTabPolicy.validBounds(
                250, 2150, 430, 2240, 1440, 3088));
        assertFalse(XtoolConceptionTabPolicy.validBounds(
                -10, 2150, 430, 2240, 1440, 3088));
        assertFalse(XtoolConceptionTabPolicy.validBounds(
                250, 2150, 430, 3290, 1440, 3088));
        assertFalse(XtoolConceptionTabPolicy.validBounds(
                250, 2150, 230, 2240, 1440, 3088));
    }
}
