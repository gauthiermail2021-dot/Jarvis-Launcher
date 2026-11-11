package ca.jarvis.bridge;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public final class EasySetSpeedRegionTest {
    private static EasySetValues.Line at(String s, int l, int t, int r, int b) {
        return new EasySetValues.Line(s, l, t, r, b);
    }

    @Test public void speedCropIncludesSingleDigitBelowItsLabel() {
        EasySetSpeedRegion field = EasySetSpeedRegion.find(Arrays.asList(
                at("Volet EasySet", 24, 42, 278, 90),
                at("Puissance(%)", 70, 190, 310, 220),
                at("Vitesse(mm/s)", 72, 393, 321, 423),
                at("Passer", 74, 590, 185, 623)
        ), 843, 1536);
        assertNotNull(field);
        assertTrue("Narrow numeric-only region", field.width() <= 180);
        assertTrue("Located at far-right numeric cell", field.left >= 670);
        assertTrue("Contains speed digit around y=471", field.top < 467
                && field.bottom > 485);
        assertTrue("Must exclude Passer", field.bottom < 590);
        assertTrue(EasySetPassesTesseract.acceptsRegion(
                field.width(), field.height(), 0, 0,
                field.width(), field.height()));
    }

    @Test public void missingOrMisplacedHeadingNeverGuessesValue() {
        assertNull(EasySetSpeedRegion.find(Collections.emptyList(), 843, 1536));
        assertNull(EasySetSpeedRegion.find(Arrays.asList(
                at("Puissance", 60, 195, 220, 223),
                at("Passer", 75, 600, 199, 640)
        ), 843, 1536));
        assertNull(EasySetSpeedRegion.find(Arrays.asList(
                at("Vitesse", 730, 400, 805, 430)
        ), 843, 1536));
        assertNull(EasySetSpeedRegion.find(Arrays.asList(
                at("Vitesse", 70, 400, 290, 430),
                at("Passer", 70, 435, 205, 467)
        ), 843, 1536));
    }

    @Test public void numericValidationUsesSpeedRangeAndRejectsGarbage() {
        assertEquals(Integer.valueOf(1),
                EasySetPassesTesseract.acceptedNumber("1", 95, 2));
        assertEquals(Integer.valueOf(20),
                EasySetPassesTesseract.acceptedNumber("20", 95, 2));
        assertEquals(Integer.valueOf(152),
                EasySetPassesTesseract.acceptedNumber("152", 95, 2));
        assertEquals(Integer.valueOf(3000),
                EasySetPassesTesseract.acceptedNumber("3000", 95, 2));
        assertNull(EasySetPassesTesseract.acceptedNumber("0", 95, 2));
        assertNull(EasySetPassesTesseract.acceptedNumber("3001", 95, 2));
        assertNull(EasySetPassesTesseract.acceptedNumber("01", 95, 2));
        assertNull(EasySetPassesTesseract.acceptedNumber("1›", 95, 2));
        assertNull(EasySetPassesTesseract.acceptedNumber("1", 49, 2));
        assertEquals(Integer.valueOf(1),
                EasySetPassesTesseract.acceptedNumber("1", 95, 0));
        assertEquals(Integer.valueOf(1),
                EasySetPassesTesseract.acceptedNumber("1", 95, 1));
        assertNull(EasySetPassesTesseract.acceptedNumber("3000", 95, 0));
        assertNull(EasySetPassesTesseract.acceptedNumber("3000", 95, 1));
    }
}
