package ca.jarvis.bridge;

import java.util.HashSet;
import java.util.Set;

/** Un résultat accepté OU refusé ne doit être traité qu'une fois par courriel. */
final class JarvisMailLedger {
    private JarvisMailLedger() { }

    static String identity(String messageId, String filename) {
        return messageId + ":" + filename;
    }

    static boolean isAlreadyHandled(String identity, String lastIdentity, Set<String> handled) {
        return identity.equals(lastIdentity) || handled.contains(identity);
    }

    static boolean wasLastMessage(String messageId, String lastIdentity) {
        return lastIdentity != null && lastIdentity.startsWith(messageId + ":");
    }

    static Set<String> remember(Set<String> handled, String identity, int maxEntries) {
        Set<String> updated = new HashSet<>(handled);
        updated.add(identity);
        if (updated.size() > maxEntries) {
            // Le curseur temporel enregistré par MainActivity demeure la garantie
            // historique. Les IDs ne servent que de garde-fou supplémentaire.
            updated.clear();
            updated.add(identity);
        }
        return updated;
    }
}
