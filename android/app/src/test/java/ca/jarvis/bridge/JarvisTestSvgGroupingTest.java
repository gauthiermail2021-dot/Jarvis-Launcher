package ca.jarvis.bridge;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.*;

/** The exact SVG sent from the bubble exercises colors AND grouping. */
public final class JarvisTestSvgGroupingTest {
    @Test public void exactlyOneGroupContainsThreeColorSpecificShapes() throws Exception {
        byte[] bytes = JarvisRepeatableTestSvg.bytes(55);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(bytes));
        NodeList groups = document.getElementsByTagNameNS(
                "http://www.w3.org/2000/svg", "g");
        assertEquals(1, groups.getLength());
        Element group = (Element) groups.item(0);
        assertEquals("jarvis-color-test", group.getAttribute("id"));
        int shapes = 0;
        for (int i = 0; i < group.getChildNodes().getLength(); i++) {
            if (group.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE)
                shapes++;
        }
        assertEquals(3, shapes);
        assertEquals("#FF0000", ((Element) group.getElementsByTagNameNS(
                "http://www.w3.org/2000/svg", "rect").item(0))
                .getAttribute("stroke"));
        assertEquals("#008000", ((Element) group.getElementsByTagNameNS(
                "http://www.w3.org/2000/svg", "circle").item(0))
                .getAttribute("stroke"));
        assertEquals("#0000FF", ((Element) group.getElementsByTagNameNS(
                "http://www.w3.org/2000/svg", "path").item(0))
                .getAttribute("stroke"));
        JarvisFileRouter.requireSvgForXtool(
                "Jarvis_Test_055.svg", "image/svg+xml", bytes, 5_000_000);
    }
}
