package ca.jarvis.bridge;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Same three-color SVG vector geometry for every numbered test. A non-rendered SVG XML comment changes on each successful send,
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
                + "  <!-- Jarvis grouped RGB test #" + number
                + ": per-email unique marker -->\n"
                + "  <g id=\"jarvis-color-test\" fill=\"none\" stroke-width=\"0.30\">\n"
                + "    <rect x=\"3\" y=\"8\" width=\"10\" height=\"14\" stroke=\"#FF0000\" />\n"
                + "    <circle cx=\"22\" cy=\"15\" r=\"6\" stroke=\"#008000\" />\n"
                + "    <path d=\"M 35 22 L 41 22 L 38 8 Z\" stroke=\"#0000FF\" />\n"
                + "  </g>\n"
                + "</svg>\n";
        return svg.getBytes(StandardCharsets.UTF_8);
    }
}
