package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public final class JarvisGmailTargetPolicyTest {
    @Test public void acceptsOnlyMessageIdCharacters() {
        assertEquals("1a1d229db7d4716e", JarvisGmailTargetPolicy.validMessageId(
                "1a1d229db7d4716e"));
        assertNull(JarvisGmailTargetPolicy.validMessageId(null));
        assertNull(JarvisGmailTargetPolicy.validMessageId(""));
        assertNull(JarvisGmailTargetPolicy.validMessageId("../messages/01234567890"));
        assertNull(JarvisGmailTargetPolicy.validMessageId("abc"));
        assertNull(JarvisGmailTargetPolicy.validMessageId("1234567890?format=full"));
        assertNull(JarvisGmailTargetPolicy.validMessageId(
                "12345678901234567890123456789012345678901234567890123456789012345"));
    }

    @Test public void existingGmailGateStillRequired() {
        assertTrue(JarvisMailGate.allow(
                JarvisRepeatableTestSvg.subject(45), true));
        assertFalse(JarvisMailGate.allow(
                JarvisRepeatableTestSvg.subject(45), false));
    }
}
