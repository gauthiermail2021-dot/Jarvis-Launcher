package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JarvisMailGateTest {
    @Test public void acceptsApprovedSelfSentMessage() {
        assertTrue(JarvisMailGate.allow("JARVIS-SVG-AUTO — Test cercle", true));
    }

    @Test public void rejectsMessagesNotSentByAccount() {
        assertFalse(JarvisMailGate.allow("JARVIS-SVG-AUTO — Test cercle", false));
    }

    @Test public void rejectsUnrelatedSelfSentMessages() {
        assertFalse(JarvisMailGate.allow("Bon matin", true));
    }

    @Test public void handlesWhitespaceAndCase() {
        assertTrue(JarvisMailGate.allow("   jarvis-svg-auto test", true));
    }

    @Test public void rejectsNullSubject() {
        assertFalse(JarvisMailGate.allow(null, true));
    }
}
