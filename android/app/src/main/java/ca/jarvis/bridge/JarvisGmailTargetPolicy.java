package ca.jarvis.bridge;

/**
 * Only a Gmail message ID is accepted as a direct read target.
 * Even a valid ID must still pass SENT, subject, label and SVG checks.
 */
final class JarvisGmailTargetPolicy {
    private JarvisGmailTargetPolicy() { }

    static String validMessageId(String id) {
        return id != null && id.matches("[a-zA-Z0-9]{10,64}") ? id : null;
    }
}
