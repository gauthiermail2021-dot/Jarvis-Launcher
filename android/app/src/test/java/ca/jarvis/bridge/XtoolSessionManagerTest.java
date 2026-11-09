package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class XtoolSessionManagerTest {
    @Test public void endingSessionSkipsMailReceivedDuringSession() {
        long lastAcceptedSvg = 1000L;
        long sessionEndedAt = 5500L;
        assertEquals(5500L,
                XtoolSessionManager.skipQueuedMailUntil(lastAcceptedSvg, sessionEndedAt));
    }

    @Test public void endingSessionNeverMovesTheGmailCursorBackward() {
        assertEquals(7500L, XtoolSessionManager.skipQueuedMailUntil(7500L, 5500L));
    }

    @Test public void onlyLaterMessagesCanPassTheGmailCursor() {
        long cutoff = XtoolSessionManager.skipQueuedMailUntil(1000L, 5500L);
        // Le scanner Gmail existant ignore internalDate <= lastProcessedTime.
        org.junit.Assert.assertFalse(5200L > cutoff);
        org.junit.Assert.assertTrue(5501L > cutoff);
    }
}
