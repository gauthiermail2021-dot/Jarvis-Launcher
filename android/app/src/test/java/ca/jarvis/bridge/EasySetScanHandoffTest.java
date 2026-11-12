package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public final class EasySetScanHandoffTest {
    @Test public void oldForegroundOrSvgTimerNeverRejectsPanelOpen() {
        assertEquals(EasySetScanHandoff.Decision.REPLACE_EXISTING_TIMER,
                EasySetScanHandoff.decide(false, false, true));
        assertEquals(EasySetScanHandoff.Decision.START,
                EasySetScanHandoff.decide(false, false, false));
    }

    @Test public void activeCameraCaptureAndOcrDeferInsteadOfDroppingRequest() {
        assertEquals(EasySetScanHandoff.Decision.DEFER_UNTIL_CURRENT_FINISHES,
                EasySetScanHandoff.decide(true, false, true));
        assertEquals(EasySetScanHandoff.Decision.DEFER_UNTIL_CURRENT_FINISHES,
                EasySetScanHandoff.decide(false, true, false));
        assertEquals(EasySetScanHandoff.Decision.DEFER_UNTIL_CURRENT_FINISHES,
                EasySetScanHandoff.decide(true, true, false));
    }
}
