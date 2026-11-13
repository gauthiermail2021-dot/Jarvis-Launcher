package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class XtoolCloseDetectionPolicyTest {
    @Test public void cannotCloseWithoutFirstObservingXtoolAlive() {
        assertEquals(0, XtoolCloseDetectionPolicy.nextAbsences(false, 2, false));
        assertFalse(XtoolCloseDetectionPolicy.shouldFinish(false, 3));
    }

    @Test public void needsThreeReliableConsecutiveAbsences() {
        int n = XtoolCloseDetectionPolicy.nextAbsences(true, 0, false);
        assertEquals(1, n);
        assertFalse(XtoolCloseDetectionPolicy.shouldFinish(true, n));
        n = XtoolCloseDetectionPolicy.nextAbsences(true, n, false);
        assertEquals(2, n);
        assertFalse(XtoolCloseDetectionPolicy.shouldFinish(true, n));
        n = XtoolCloseDetectionPolicy.nextAbsences(true, n, false);
        assertTrue(XtoolCloseDetectionPolicy.shouldFinish(true, n));
    }

    @Test public void missingOrPositiveSampleResetsTheConfirmation() {
        assertEquals(0, XtoolCloseDetectionPolicy.nextAbsences(true, 2, null));
        assertEquals(0, XtoolCloseDetectionPolicy.nextAbsences(true, 2, true));
    }

    @Test public void noUnboundedCounts() {
        assertEquals(3, XtoolCloseDetectionPolicy.nextAbsences(true, 600, false));
    }
}
