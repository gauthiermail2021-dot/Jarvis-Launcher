package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class JarvisTestSendPolicyTest {
    @Test public void exportedLauncherMustNotSendByOpeningActivity() {
        assertFalse(JarvisTestSendPolicy.autoSendOnActivityLaunch(false));
    }

    @Test public void nonExportedBubbleRetainsOneTapSend() {
        assertTrue(JarvisTestSendPolicy.autoSendOnActivityLaunch(true));
    }
}
