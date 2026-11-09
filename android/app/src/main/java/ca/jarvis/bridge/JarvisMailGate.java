package ca.jarvis.bridge;

import java.util.Locale;

/** Limite les traitements automatiques aux messages envoyés depuis ce compte. */
final class JarvisMailGate {
    static final String SUBJECT_PREFIX = "JARVIS-SVG-AUTO";

    private JarvisMailGate() { }

    static boolean allow(String subject, boolean hasSentLabel) {
        return hasSentLabel && subject != null
                && subject.trim().toUpperCase(Locale.ROOT).startsWith(SUBJECT_PREFIX);
    }
}
