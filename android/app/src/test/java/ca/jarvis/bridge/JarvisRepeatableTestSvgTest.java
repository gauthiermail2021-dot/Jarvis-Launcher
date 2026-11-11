package ca.jarvis.bridge;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import static org.junit.Assert.*;

public final class JarvisRepeatableTestSvgTest {
    @Test public void addsOneToFileName() {
        assertEquals("Jarvis_Test_001.svg", JarvisRepeatableTestSvg.filename(1));
        assertEquals("Jarvis_Test_002.svg", JarvisRepeatableTestSvg.filename(2));
        assertEquals("Jarvis_Test_010.svg", JarvisRepeatableTestSvg.filename(10));
        assertEquals("Jarvis_Test_1000.svg", JarvisRepeatableTestSvg.filename(1000));
    }

    @Test public void usesApprovedMailSubjectPrefix() {
        assertTrue(JarvisMailGate.allow(JarvisRepeatableTestSvg.subject(5), true));
        assertFalse(JarvisMailGate.allow(JarvisRepeatableTestSvg.subject(5), false));
    }

    @Test public void sameVisibleDesignButDifferentFileBytes() throws Exception {
        String first = new String(JarvisRepeatableTestSvg.bytes(1), StandardCharsets.UTF_8);
        String second = new String(JarvisRepeatableTestSvg.bytes(2), StandardCharsets.UTF_8);
        assertNotEquals(first, second);
        assertEquals(first.replace("#1", "#N"), second.replace("#2", "#N"));
        assertFalse(MessageDigest.isEqual(
                MessageDigest.getInstance("SHA-256").digest(JarvisRepeatableTestSvg.bytes(1)),
                MessageDigest.getInstance("SHA-256").digest(JarvisRepeatableTestSvg.bytes(2))));
        assertTrue(first.contains("width=\"44mm\" height=\"30mm\""));
        assertTrue(first.contains("<circle cx=\"10\" cy=\"12\" r=\"2\""));
    }

    @Test public void allNumberedTemplatesAreValidSvgForXTool() throws Exception {
        for (int id : new int[]{1, 2, 10, 100}) {
            JarvisFileRouter.requireSvgForXtool(
                    JarvisRepeatableTestSvg.filename(id), "image/svg+xml",
                    JarvisRepeatableTestSvg.bytes(id), 5_000_000);
        }
    }
}
