package ca.jarvis.bridge;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Profil SVG vectoriel sûr pour l'importation automatique.
 * Les SVG plus complexes doivent être ouverts et validés manuellement.
 */
final class SafeSvgValidator {
    private static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final int MAX_ELEMENTS = 25000;
    private static final int MAX_DEPTH = 64;

    private static final Set<String> SHAPES = new HashSet<>(Arrays.asList(
            "svg", "g", "path", "rect", "circle", "ellipse",
            "line", "polyline", "polygon", "text", "tspan",
            "defs", "linearGradient", "radialGradient", "stop",
            "clipPath", "title", "desc"
    ));

    private SafeSvgValidator() { }

    static void validate(byte[] bytes, int maxBytes) throws Exception {
        if (bytes == null || bytes.length == 0 || bytes.length > maxBytes) {
            throw new Exception("Taille SVG invalide (maximum " + maxBytes + " octets)");
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("<!doctype") || lower.contains("<!entity")) {
            throw new Exception("DTD et entités XML interdits");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        factory.setExpandEntityReferences(false);
        try { factory.setXIncludeAware(false); } catch (UnsupportedOperationException ignored) { }
        // Certaines versions Android ne prennent pas en charge tous les réglages
        // JAXP. Le rejet explicite des DTD et le résolveur bloquant restent actifs.
        try { factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true); }
        catch (Exception ignored) { }

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> {
            throw new SAXException("Ressources XML externes interdites");
        });
        builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void fatalError(org.xml.sax.SAXParseException e) throws SAXException {
                throw e;
            }
        });

        Document document = builder.parse(new InputSource(new ByteArrayInputStream(bytes)));
        Element root = document.getDocumentElement();
        if (root == null || !"svg".equals(localName(root))) {
            throw new Exception("Racine <svg> obligatoire");
        }

        ArrayDeque<NodeDepth> stack = new ArrayDeque<>();
        stack.push(new NodeDepth(root, 1));
        int elements = 0;

        while (!stack.isEmpty()) {
            NodeDepth current = stack.pop();
            Node node = current.node;

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                if (++elements > MAX_ELEMENTS || current.depth > MAX_DEPTH) {
                    throw new Exception("SVG trop complexe");
                }
                String ns = node.getNamespaceURI();
                if (ns != null && !SVG_NS.equals(ns)) {
                    throw new Exception("Élément hors de l'espace SVG");
                }
                String name = localName(node);
                if (!SHAPES.contains(name)) {
                    throw new Exception("Élément SVG non autorisé : " + name);
                }

                NamedNodeMap attrs = node.getAttributes();
                if (attrs.getLength() > 60) throw new Exception("Trop d'attributs SVG");
                for (int i = 0; i < attrs.getLength(); i++) {
                    Node attr = attrs.item(i);
                    String attrName = localName(attr).toLowerCase(Locale.ROOT);
                    String fullName = attr.getNodeName().toLowerCase(Locale.ROOT);
                    if (fullName.startsWith("xmlns")) continue;
                    if (attrName.startsWith("on")
                            || "href".equals(attrName)
                            || "src".equals(attrName)
                            || "base".equals(attrName)
                            || "schemalocation".equals(attrName)) {
                        throw new Exception("Attribut SVG non autorisé : " + attr.getNodeName());
                    }
                    String value = attr.getNodeValue().toLowerCase(Locale.ROOT)
                            .replaceAll("\\s+", "");
                    if (value.contains("url(") || value.contains("javascript:")
                            || value.contains("data:") || value.contains("https:")
                            || value.contains("http:") || value.contains("file:")
                            || value.contains("content:") || value.contains("@import")
                            || value.contains("expression(")) {
                        throw new Exception("Ressource ou expression externe interdite");
                    }
                }
            } else if (node.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE
                    || node.getNodeType() == Node.DOCUMENT_TYPE_NODE
                    || node.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
                throw new Exception("Directive XML non autorisée");
            }

            NodeList children = node.getChildNodes();
            for (int i = children.getLength() - 1; i >= 0; i--) {
                stack.push(new NodeDepth(children.item(i), current.depth + 1));
            }
        }
    }

    private static String localName(Node node) {
        String local = node.getLocalName();
        return local == null ? node.getNodeName() : local;
    }

    private static final class NodeDepth {
        final Node node;
        final int depth;
        NodeDepth(Node node, int depth) { this.node = node; this.depth = depth; }
    }
}
