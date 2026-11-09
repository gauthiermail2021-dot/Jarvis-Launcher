package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class JarvisGmailNoticePolicyTest {
    @Test public void ordinaryEmailDoesNotGenerateManualJarvisAlert() {
        assertFalse(JarvisGmailNoticePolicy.isJarvisHint("Un message", "Salut demain"));
        assertFalse(JarvisGmailNoticePolicy.isJarvisHint("Gmail", "3 nouveaux messages"));
    }

    @Test public void matchingJarvisSvgSubjectRetainsFallback() {
        assertTrue(JarvisGmailNoticePolicy.isJarvisHint("JARVIS-SVG-AUTO — TEST 1.2.17", "Voir pièce jointe"));
        assertTrue(JarvisGmailNoticePolicy.isJarvisHint("Gmail", "Libellé Jarvis SVG"));
        assertTrue(JarvisGmailNoticePolicy.isJarvisHint("Gmail", "pièce_jointe.svg"));
    }

    @Test public void nullAndCaseVariantsAreHandled() {
        assertFalse(JarvisGmailNoticePolicy.isJarvisHint(null, null));
        assertTrue(JarvisGmailNoticePolicy.isJarvisHint(null, "JARVIS SVG"));
    }
}
