package ca.jarvis.bridge;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public final class EasySetPassesRegionTest {
    private static EasySetValues.Line at(String s, int l, int t, int r, int b) {
        return new EasySetValues.Line(s, l, t, r, b);
    }

    @Test public void cropsPasserRowFromActualSamsungScreenshot() {
        EasySetPassesRegion region = EasySetPassesRegion.find(Arrays.asList(
                at("Volet EasySet", 40, 45, 295, 91),
                at("Puissance(%)", 77, 180, 310, 215),
                at("Vitesse(mm/s)", 75, 393, 329, 426),
                at("Passer", 77, 595, 207, 625),
                at("Paramétrage en un clic", 76, 745, 530, 786)), 900, 1600);
        assertNotNull(region);
        assertTrue(region.left <= 77);
        assertTrue(region.right >= 795);
        assertTrue(region.top <= 595);
        assertTrue(region.bottom >= 626);
        assertTrue("Must not encompass Vitesse numerical row", region.top > 490);
        assertTrue("Must not reach next section", region.bottom < 740);
    }

    @Test public void neverPicksHeadingInRightPanelOrOutsideScreen() {
        assertNull(EasySetPassesRegion.find(Arrays.asList(
                at("Passer", 820, 400, 885, 443)), 900, 1600));
        assertNull(EasySetPassesRegion.find(Arrays.asList(
                at("Passer", 75, 1605, 215, 1645)), 900, 1600));
    }

    @Test public void missingPasserHeadingMeansNoCropAndNoInventedValue() {
        assertNull(EasySetPassesRegion.find(Arrays.asList(
                at("Puissance", 70, 230, 250, 270),
                at("Vitesse", 70, 420, 250, 460)), 900, 1600));
        assertNull(EasySetPassesRegion.find(Collections.emptyList(), 900, 1600));
    }

    @Test public void croppedPasserOcrCannotBorrowSpeedNumbers() {
        var base = Arrays.asList(
                at("Volet EasySet", 20, 30, 300, 72),
                at("Puissance", 60, 155, 246, 188),
                at("63", 780, 232, 830, 275),
                at("Vitesse", 60, 350, 236, 389),
                at("134", 777, 460, 832, 494),
                at("Passer", 60, 595, 198, 633));
        var misplaced = Arrays.asList(at("134", 778, 472, 835, 495));
        String result = EasySetValues.diagnose(base, misplaced);
        assertTrue(result.contains("Puissance : 63 %"));
        assertTrue(result.contains("Vitesse : 134 mm/s"));
        assertTrue(result.contains("Passages : non lus"));
        var recognized = Arrays.asList(at("1", 791, 592, 811, 626));
        result = EasySetValues.diagnose(base, recognized);
        assertTrue(result.contains("Passages : 1"));
    }

    @Test public void neverGuessesOneFromLetterOrArrow() {
        var base = Arrays.asList(
                at("Volet EasySet", 20, 30, 300, 70),
                at("Puissance", 60, 170, 250, 208),
                at("Vitesse", 60, 390, 250, 429),
                at("Passer", 60, 595, 198, 632));
        var ambiguous = Arrays.asList(
                at("l", 778, 595, 795, 632),
                at("›", 798, 595, 815, 632));
        String result = EasySetValues.diagnose(base, ambiguous);
        assertTrue(result.contains("Passages : non lus"));
    }
}
