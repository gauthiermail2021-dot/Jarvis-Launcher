package ca.jarvis.bridge;

import org.junit.Test;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class JarvisMailLedgerTest {
    @Test public void rejectedEmailIsHandledOnceAcrossRepeatedNotifications() {
        String identity = JarvisMailLedger.identity("gmail-123", "file.svg");
        Set<String> handled = JarvisMailLedger.remember(Collections.emptySet(), identity, 100);
        assertTrue(JarvisMailLedger.isAlreadyHandled(identity, identity, handled));
        assertTrue(JarvisMailLedger.wasLastMessage("gmail-123", identity));
        assertFalse(JarvisMailLedger.wasLastMessage("gmail-456", identity));
    }

    @Test public void aNewMessageWithSameFilenameIsNotLost() {
        String previous = "gmail-123:file.svg";
        Set<String> handled = JarvisMailLedger.remember(Collections.emptySet(), previous, 100);
        assertFalse(JarvisMailLedger.isAlreadyHandled(
                "gmail-124:file.svg", previous, handled));
    }

    @Test public void matchingContentFromNewMessageStillHasNewIdentity() {
        assertNotEquals(JarvisMailLedger.identity("gmail-123", "part.svg"),
                JarvisMailLedger.identity("gmail-124", "part.svg"));
    }

    @Test public void ledgerKeepsBoundedSize() {
        Set<String> old = new HashSet<>();
        old.add("id1:file.svg"); old.add("id2:file.svg");
        Set<String> latest = JarvisMailLedger.remember(old, "id3:file.svg", 2);
        assertEquals(1, latest.size());
        assertTrue(latest.contains("id3:file.svg"));
    }

    @Test public void previousMessageLookupDoesNotConfuseIdsWithCommonPrefix() {
        assertFalse(JarvisMailLedger.wasLastMessage("abc", "abcdef:file.svg"));
        assertTrue(JarvisMailLedger.wasLastMessage("abc", "abc:file.svg"));
    }

    @Test public void oldSetIsNotMutated() {
        Set<String> old = new HashSet<>();
        old.add("old:file.svg");
        JarvisMailLedger.remember(old, "new:file.svg", 100);
        assertEquals(1, old.size());
    }
}
