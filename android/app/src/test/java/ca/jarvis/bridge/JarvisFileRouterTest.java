package ca.jarvis.bridge;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class JarvisFileRouterTest {
    private final byte[] svg = ("<svg xmlns='http://www.w3.org/2000/svg' "
            + "width='50mm' height='50mm' viewBox='0 0 50 50'>"
            + "<circle cx='25' cy='25' r='15'/></svg>").getBytes(StandardCharsets.UTF_8);

    @Test public void acceptsRealSvgWithOctetStream() throws Exception {
        assertEquals(JarvisFileRouter.Kind.SVG_2D,
                JarvisFileRouter.classify("piece.SVG", "application/octet-stream", svg));
        JarvisFileRouter.requireSvgForXtool("piece.SVG", "application/octet-stream", svg, 5000000);
    }

    @Test public void acceptsSvgWithoutKnownFilename() throws Exception {
        assertEquals(JarvisFileRouter.Kind.SVG_2D,
                JarvisFileRouter.classify("document:1928", "application/octet-stream", svg));
        JarvisFileRouter.requireSvgForXtool("document:1928", "application/octet-stream", svg, 5000000);
    }

    @Test public void recognizesCad3dExtensions() {
        for (String name : new String[]{"a.STL", "a.step", "a.STP", "a.sldprt",
                "a.obj", "a.3mf", "a.iges", "a.f3d", "a.sldasm"}) {
            assertEquals(name, JarvisFileRouter.Kind.CAD_3D,
                    JarvisFileRouter.classify(name, "application/octet-stream", new byte[]{1,2,3}));
        }
    }

    @Test public void rejectsCadBeforeOpeningXtool() {
        byte[] step = "ISO-10303-21; HEADER; ENDSEC;".getBytes(StandardCharsets.UTF_8);
        assertThrows(Exception.class, () -> JarvisFileRouter.requireSvgForXtool(
                "boitier.step", "application/octet-stream", step, 5000000));
    }

    @Test public void detectsDisguisedStepAsSvg() {
        byte[] data = "ISO-10303-21; HEADER;".getBytes(StandardCharsets.UTF_8);
        assertEquals(JarvisFileRouter.Kind.CAD_3D,
                JarvisFileRouter.classify("piece.svg", "image/svg+xml", data));
    }

    @Test public void detectsAsciiStlRenamedSvg() {
        byte[] data = "solid demo\nfacet normal 0 0 1\nendfacet\nendsolid demo"
                .getBytes(StandardCharsets.UTF_8);
        assertEquals(JarvisFileRouter.Kind.CAD_3D,
                JarvisFileRouter.classify("part.svg", "image/svg+xml", data));
    }

    @Test public void recognizesDxfButDoesNotForwardIt() {
        assertEquals(JarvisFileRouter.Kind.DXF_2D,
                JarvisFileRouter.classify("face.dxf", "application/octet-stream", new byte[]{1}));
        assertThrows(Exception.class, () -> JarvisFileRouter.requireSvgForXtool(
                "face.dxf", "application/octet-stream", new byte[]{1}, 5000000));
    }

    @Test public void blocksUnknownFormats() {
        assertEquals(JarvisFileRouter.Kind.UNKNOWN,
                JarvisFileRouter.classify("image.png", "image/png", new byte[]{1,2,3}));
        assertThrows(Exception.class, () -> JarvisFileRouter.requireSvgForXtool(
                "image.png", "image/png", new byte[]{1,2,3}, 5000000));
    }

    @Test public void refusesMalformedSvgDespiteExtension() {
        assertThrows(Exception.class, () -> JarvisFileRouter.requireSvgForXtool(
                "malicious.svg", "image/svg+xml",
                "<svg width='50mm'><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8),
                5000000));
    }

    @Test public void understandsUrlFilename() {
        assertEquals(JarvisFileRouter.Kind.CAD_3D,
                JarvisFileRouter.classify("https://example.com/model.stl?download=1",
                        "application/octet-stream", new byte[]{1}));
    }
}
