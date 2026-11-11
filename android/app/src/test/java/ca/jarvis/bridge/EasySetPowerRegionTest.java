package ca.jarvis.bridge;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public final class EasySetPowerRegionTest {
    private static EasySetValues.Line at(String s, int l, int t, int r, int b) {
        return new EasySetValues.Line(s, l, t, r, b);
    }

    @Test public void cropsPowerDigitFromSamsungScreenshotOnePercent() {
        EasySetPowerRegion r = EasySetPowerRegion.find(Arrays.asList(
                at("Volet EasySet", 24, 39, 282, 80),
                at("Puissance(%)", 68, 177, 302, 211),
                at("Vitesse(mm/s)", 70, 370, 331, 412),
                at("Passer", 70, 564, 200, 611)), 864, 1536);
        assertNotNull(r);
        assertTrue(r.left > 645);
        assertTrue(r.left < 769);
        assertTrue(r.right >= 778 && r.right <= 800);
        assertTrue(r.top < 253 && r.bottom > 265);
        assertTrue("Must not overlap next Vitesse field", r.bottom < 370);
        assertTrue("Small isolated numeric region", r.width() < 180
                && r.height() < 128);
        assertTrue(EasySetPassesTesseract.acceptsRegion(
                r.width(), r.height(), 0, 0, r.width(), r.height()));
    }

    @Test public void doesNotBorrowNextSettingOrUnrelatedLabels() {
        assertNull(EasySetPowerRegion.find(Collections.emptyList(), 864, 1536));
        assertNull(EasySetPowerRegion.find(Arrays.asList(
                at("Vitesse", 65, 390, 240, 425),
                at("Passer", 65, 582, 197, 619)), 864, 1536));
        assertNull(EasySetPowerRegion.find(Arrays.asList(
                at("Puissance", 735, 170, 800, 202)), 864, 1536));
        // If next heading immediately follows, never scan across it.
        assertNull(EasySetPowerRegion.find(Arrays.asList(
                at("Puissance", 70, 170, 290, 208),
                at("Vitesse", 70, 211, 280, 251)), 864, 1536));
    }

    @Test public void worksWithPowerAndSpeedAlreadySeenInOCR() {
        var ocr = Arrays.asList(
                at("Volet EasySet", 25, 35, 260, 65),
                at("Puissance(%)", 67, 179, 303, 211),
                at("Vitesse(mm/s)", 65, 370, 320, 404),
                at("20", 734, 448, 777, 483),
                at("Passer", 70, 578, 198, 610));
        String baseline = EasySetValues.diagnose(ocr);
        assertTrue(baseline.contains("Puissance : non lue"));
        assertTrue(baseline.contains("Vitesse : 20 mm/s"));
        assertTrue(baseline.contains("Passages : non lus"));
        assertNotNull(EasySetPowerRegion.find(ocr, 864, 1536));
    }
}
