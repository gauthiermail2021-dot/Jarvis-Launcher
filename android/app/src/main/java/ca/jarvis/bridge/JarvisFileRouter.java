package ca.jarvis.bridge;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Identifie la destination AVANT de lancer une application.
 * Seuls les SVG 2D sûrs sont transférés automatiquement à xTool P2S.
 * Les formats CAD 3D ne sont jamais envoyés à xTool.
 */
final class JarvisFileRouter {
    enum Kind { SVG_2D, DXF_2D, CAD_3D, UNKNOWN }

    /** Refus permanent du contenu : ne jamais le retraiter après une notification. */
    static final class RejectedFileException extends Exception {
        final Kind kind;

        RejectedFileException(Kind kind, String message) {
            super(message);
            this.kind = kind;
        }
    }

    private static final Set<String> CAD_EXTENSIONS = new HashSet<>(Arrays.asList(
            "stl", "step", "stp", "sldprt", "sldasm", "slddrw",
            "obj", "3mf", "iges", "igs", "f3d", "f3z",
            "3dm", "skp", "blend", "amf", "gcode", "gco"
    ));

    private JarvisFileRouter() { }

    static Kind classify(String filename, String mimeType, byte[] bytes) {
        String ext = extension(filename);
        if (CAD_EXTENSIONS.contains(ext)) return Kind.CAD_3D;
        if ("dxf".equals(ext)) return Kind.DXF_2D;
        // Ne jamais faire confiance à la seule extension : un CAD renommé .svg
        // ne doit pas démarrer xTool.
        String head = head(bytes).toLowerCase(Locale.ROOT);
        if (head.contains("iso-10303-21;") || head.startsWith("solid ")
                && head.contains("facet normal")) return Kind.CAD_3D;

        String mime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (mime.contains("model/stl") || mime.contains("model/step")
                || mime.contains("model/obj") || mime.contains("model/3mf")
                || mime.contains("application/sla") || mime.contains("model/iges")) {
            return Kind.CAD_3D;
        }
        if (mime.contains("image/vnd.dxf") || mime.contains("application/dxf")) {
            return Kind.DXF_2D;
        }

        if ("svg".equals(ext) || head.contains("<svg")
                || "image/svg+xml".equals(mime)) return Kind.SVG_2D;
        return Kind.UNKNOWN;
    }

    static void requireSvgForXtool(
            String filename, String mimeType, byte[] bytes, int maxBytes) throws Exception {
        Kind kind = classify(filename, mimeType, bytes);
        if (kind == Kind.CAD_3D) {
            throw new RejectedFileException(kind, "CAD 3D détecté : transfert vers xTool P2S bloqué. "
                    + "Pour la découpe laser, exporter une vue 2D SVG depuis SolidWorks.");
        }
        if (kind == Kind.DXF_2D) {
            throw new RejectedFileException(kind, "Fichier DXF 2D détecté. "
                    + "L'importation automatique Jarvis utilise actuellement SVG; "
                    + "convertir en SVG ou importer le DXF manuellement dans xTool.");
        }
        if (kind != Kind.SVG_2D) {
            throw new RejectedFileException(kind, "Format non reconnu : aucun transfert automatique vers xTool.");
        }
        try {
            SafeSvgValidator.validate(bytes, maxBytes);
        } catch (Exception invalidSvg) {
            throw new RejectedFileException(Kind.SVG_2D,
                    "SVG invalide ou non sécurisé : " + invalidSvg.getMessage());
        }
    }

    private static String extension(String filename) {
        if (filename == null) return "";
        String normalized = filename.toLowerCase(Locale.ROOT);
        int query = normalized.indexOf('?');
        if (query >= 0) normalized = normalized.substring(0, query);
        int fragment = normalized.indexOf('#');
        if (fragment >= 0) normalized = normalized.substring(0, fragment);
        int slash = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        int dot = normalized.lastIndexOf('.');
        return dot > slash ? normalized.substring(dot + 1) : "";
    }

    private static String head(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        return new String(bytes, 0, Math.min(bytes.length, 1024), StandardCharsets.UTF_8)
                .replace("\uFEFF", "").trim();
    }
}
