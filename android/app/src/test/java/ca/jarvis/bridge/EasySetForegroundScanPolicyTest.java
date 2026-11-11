package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public final class EasySetForegroundScanPolicyTest {
    @Test public void readsExistingXToolProjectWhenConsentAlreadyGranted() {
        assertTrue(EasySetForegroundScanPolicy.shouldRequest(
                "com.makeblock.xcs", true, true, 25_000, -15_000));
    }

    @Test public void refusesCaptureWithoutAndroidProjectionConsent() {
        assertFalse(EasySetForegroundScanPolicy.shouldRequest(
                "com.makeblock.xcs", false, true, 25_000, -15_000));
    }

    @Test public void refusesCaptureWhenJarvisBubbleIsNotVisible() {
        assertFalse(EasySetForegroundScanPolicy.shouldRequest(
                "com.makeblock.xcs", true, false, 25_000, -15_000));
    }

    @Test public void willNotObserveOtherApplications() {
        assertFalse(EasySetForegroundScanPolicy.shouldRequest(
                "com.google.android.gm", true, true, 25_000, -15_000));
        assertFalse(EasySetForegroundScanPolicy.shouldRequest(
                "com.openai.chatgpt", true, true, 25_000, -15_000));
    }

    @Test public void debouncesRepeatedXtoolWindowEvents() {
        assertFalse(EasySetForegroundScanPolicy.shouldRequest(
                "com.makeblock.xcs", true, true, 26_000, 25_000));
        assertTrue(EasySetForegroundScanPolicy.shouldRequest(
                "com.makeblock.xcs", true, true, 40_000, 25_000));
    }
}
