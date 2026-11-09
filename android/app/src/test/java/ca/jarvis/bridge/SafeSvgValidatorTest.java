package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.assertThrows;

import java.nio.charset.StandardCharsets;

public class SafeSvgValidatorTest {
    private void valid(String xml) throws Exception {
        SafeSvgValidator.validate(xml.getBytes(StandardCharsets.UTF_8), 5_000_000);
    }

    private void rejected(String xml) {
        assertThrows(Exception.class, () -> valid(xml));
    }

    @Test public void acceptsCircleForXtool() throws Exception {
        valid("<svg xmlns='http://www.w3.org/2000/svg' width='50mm' height='50mm' viewBox='0 0 50 50'><circle cx='25' cy='25' r='20' fill='none' stroke='black'/></svg>");
    }

    @Test public void acceptsPathsAndText() throws Exception {
        valid("<svg width='50mm' height='50mm'><g><path d='M 0 0 L 50 50'/><text x='5' y='10'>ABC</text></g></svg>");
    }

    @Test public void rejectsScript() {
        rejected("<svg><script>alert(1)</script></svg>");
    }

    @Test public void rejectsForeignObject() {
        rejected("<svg><foreignObject><body>HTML</body></foreignObject></svg>");
    }

    @Test public void rejectsExternalImage() {
        rejected("<svg><image href='https://example.com/pic.png'/></svg>");
    }

    @Test public void rejectsEventHandlers() {
        rejected("<svg onload='alert(1)'><path d='M0 0'/></svg>");
    }

    @Test public void rejectsCssResource() {
        rejected("<svg><path style='fill:url(https://example.com/a.svg)' d='M0 0'/></svg>");
    }

    @Test public void rejectsDoctypeAndEntity() {
        rejected("<!DOCTYPE svg [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><svg>&x;</svg>");
    }

    @Test public void rejectsMalformedSvg() {
        rejected("<svg><path></svg>");
    }

    @Test public void rejectsUnknownNamespace() {
        rejected("<svg xmlns='http://www.w3.org/2000/svg'><evil:tag xmlns:evil='http://example.com'/></svg>");
    }

    @Test public void rejectsOversizedSvg() {
        assertThrows(Exception.class, () -> SafeSvgValidator.validate(
                "<svg/>".getBytes(StandardCharsets.UTF_8), 4));
    }
}
