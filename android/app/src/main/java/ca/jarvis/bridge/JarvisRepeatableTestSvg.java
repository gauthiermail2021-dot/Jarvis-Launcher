package ca.jarvis.bridge;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Same visible vector geometry as the original trapezoid test sent on
 * 2026-11-10. A non-rendered SVG XML comment changes on each successful send,
 * so Jarvis' SHA-256 duplicate suppression cannot mistake the NEW test mail
 * for the old test file.
 */
final class JarvisRepeatableTestSvg {
    private JarvisRepeatableTestSvg() {}

    static String filename(long number) {
        if (number <= 0 || number > 999_999_999L)
            throw new IllegalArgumentException("Numéro de test hors limites");
        return String.format(Locale.ROOT, "Jarvis_Test_%03d.svg", number);
    }

    static String subject(long number) {
        return String.format(Locale.ROOT,
                "JARVIS-SVG-AUTO - Bouton TEST #%03d", number);
    }

    static byte[] bytes(long number) {
        if (number <= 0 || number > 999_999_999L)
            throw new IllegalArgumentException("Numéro de test hors limites");
        String svg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"44mm\" "
                + "height=\"30mm\" viewBox=\"0 0 44 30\">\n"
                + "  <!-- Jarvis automatic test #" + number
                + " : only the invisible comment changes -->\n"
                + "  <g fill=\"none\" stroke=\"#000000\" stroke-width=\"0.20\">\n"
                + "    <path d=\"M 1.5 1.5 H 42.5 L 37.5 28.5 H 6.5 Z\" />\n"
                + "    <circle cx=\"10\" cy=\"12\" r=\"2\" />\n"
                + "    <circle cx=\"34\" cy=\"12\" r=\"2\" />\n"
                + "    <path d=\"M 22 6 L 27 12 L 22 18 L 17 12 Z\" />\n"
                + "  </g>\n"
                + "</svg>\n";
        return svg.getBytes(StandardCharsets.UTF_8);
    }
}
