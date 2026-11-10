package ca.jarvis.bridge;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertTrue;

public class EasySetValuesTest {
    private EasySetValues.Line at(String text, int x1, int y1, int x2, int y2) {
        return new EasySetValues.Line(text, x1, y1, x2, y2);
    }

    @Test public void readsInlineFrenchValuesWithUnits() {
        String result = EasySetValues.diagnose(Arrays.asList(
                at("EasySet", 30, 10, 110, 30),
                at("Puissance (%) : 75%", 30, 50, 260, 76),
                at("Vitesse (mm/s) 200", 30, 100, 270, 126),
                at("Passer : 2", 30, 150, 190, 176)));
        assertTrue(result.contains("Puissance : 75 %"));
        assertTrue(result.contains("Vitesse : 200 mm/s"));
        assertTrue(result.contains("Passages : 2"));
    }

    @Test public void readsIsolatedNumbersOnlyFromSameRowToRight() {
        List<EasySetValues.Line> lines = Arrays.asList(
                at("EasySet", 30, 10, 90, 30),
                at("Puissance", 25, 100, 170, 127),
                at("35 %", 480, 101, 550, 128),
                at("Vitesse", 25, 180, 170, 208),
                at("150", 480, 181, 550, 208),
                at("Passer", 25, 258, 145, 287),
                at("1", 480, 260, 490, 287));
        String result = EasySetValues.diagnose(lines);
        assertTrue(result.contains("Puissance : 35 %"));
        assertTrue(result.contains("Vitesse : 150 mm/s"));
        assertTrue(result.contains("Passages : 1"));
    }

    @Test public void unrelatedDimensionsMustNotBecomeMachineSettings() {
        String result = EasySetValues.diagnose(Arrays.asList(
                at("39.8mm x 24.8mm", 30, 10, 300, 40),
                at("X:280.1mm Y:140.1mm", 35, 50, 350, 80),
                at("Volet EasySet", 25, 220, 225, 245),
                at("Puissance", 25, 310, 150, 345),
                at("Vitesse", 25, 410, 140, 445),
                at("Passer", 25, 510, 140, 545),
                at("250", 490, 105, 550, 128)));
        assertTrue(result.contains("Puissance : non lue"));
        assertTrue(result.contains("Vitesse : non lue"));
        assertTrue(result.contains("Passages : non lus"));
        assertTrue(!result.contains("Puissance : 250"));
    }

    @Test public void ignoresInvalidUnitsAndOutOfRangeNumbers() {
        String result = EasySetValues.diagnose(Arrays.asList(
                at("EasySet", 0, 0, 110, 28),
                at("Puissance: 120%", 20, 100, 160, 125),
                at("Vitesse: 70%", 20, 170, 160, 196),
                at("Passer: 0", 20, 240, 160, 266)));
        assertTrue(result.contains("Puissance : non lue"));
        assertTrue(result.contains("Vitesse : non lue"));
        assertTrue(result.contains("Passages : non lus"));
    }

    @Test public void refusesToReadValuesFromJarvisOwnWindow() {
        String result = EasySetValues.diagnose(Arrays.asList(
                at("JARVIS 1.2.27 • EASYSET", 20, 20, 400, 80),
                at("Puissance : 95%", 20, 100, 300, 130),
                at("Vitesse : 500", 20, 150, 300, 180),
                at("Passer : 5", 20, 200, 300, 230)));
        assertTrue(result.contains("Capture de Jarvis"));
        assertTrue(!result.contains("Puissance : 95"));
    }

    @Test public void supportsCommaDecimalButNoFabricatedNeighborValues() {
        String result = EasySetValues.diagnose(Arrays.asList(
                at("EasySet", 10, 10, 170, 32),
                at("Puissance 22,5%", 15, 50, 255, 75),
                at("Vitesse (mm/s)", 15, 100, 210, 125),
                at("150", 15, 200, 70, 223),
                at("Passages : 1", 15, 250, 215, 275)));
        assertTrue(result.contains("Puissance : 22,5 %"));
        assertTrue(result.contains("Vitesse : non lue"));
        assertTrue(result.contains("Passages : 1"));
    }

    @Test public void readsXtoolSliderValuesBelowTheirLabels() {
        // Reproduces the screenshot geometry: slider values are far right
        // and around 100 px below the printed headings.
        String result = EasySetValues.diagnose(Arrays.asList(
                at("Volet EasySet", 12, 80, 340, 140),
                at("Puissance", 72, 292, 280, 334),
                at("65% ›", 1030, 408, 1130, 446),
                at("Vitesse(mm/s)", 72, 580, 434, 622),
                at("78 ▶", 1040, 670, 1120, 714),
                at("Passer", 72, 862, 226, 908),
                at("1 ›", 1080, 858, 1138, 912),
                at("Paramétrage en un clic", 65, 1084, 580, 1130)));
        assertTrue(result.contains("Puissance : 65 %"));
        assertTrue(result.contains("Vitesse : 78 mm/s"));
        assertTrue(result.contains("Passages : 1"));
    }

    @Test public void valueBelowNextSettingIsNeverBorrowed() {
        String result = EasySetValues.diagnose(Arrays.asList(
                at("EasySet", 10, 30, 170, 65),
                at("Puissance", 50, 140, 240, 176),
                at("Vitesse", 50, 270, 240, 306),
                at("78", 700, 350, 760, 385),
                at("Passer", 50, 480, 240, 515),
                at("2", 700, 490, 760, 525)));
        assertTrue(result.contains("Puissance : non lue"));
        assertTrue(result.contains("Vitesse : 78 mm/s"));
        assertTrue(result.contains("Passages : 2"));
    }

    @Test public void twoDifferentNumbersInSameZoneAreAmbiguous() {
        String result = EasySetValues.diagnose(Arrays.asList(
                at("EasySet", 20, 20, 200, 60),
                at("Puissance", 30, 150, 220, 190),
                at("65", 790, 240, 840, 280),
                at("66", 950, 250, 1000, 290),
                at("Vitesse", 30, 440, 220, 480),
                at("75", 900, 500, 960, 540),
                at("Passer", 30, 640, 210, 680),
                at("1", 950, 640, 990, 680)));
        assertTrue(result.contains("Puissance : non lue"));
        assertTrue(result.contains("Vitesse : 75 mm/s"));
    }

    @Test public void rejectsCanvasNumbersAndSliderLeftSideValues() {
        String result = EasySetValues.diagnose(Arrays.asList(
                at("39.8 mm x 24.8 mm", 35, 15, 370, 60),
                at("Volet EasySet", 22, 80, 430, 135),
                at("Puissance", 65, 250, 310, 300),
                at("80", 80, 330, 120, 375),
                at("Vitesse", 65, 520, 300, 570),
                at("78", 70, 620, 125, 660),
                at("Passer", 65, 770, 230, 812),
                at("1", 1000, 770, 1030, 812)));
        assertTrue(result.contains("Puissance : non lue"));
        assertTrue(result.contains("Vitesse : non lue"));
        assertTrue(result.contains("Passages : 1"));
    }

    @Test public void malformedInlineValueIsNeverReplacedWithNeighborNumber() {
        String result = EasySetValues.diagnose(Arrays.asList(
                at("EasySet", 20, 20, 230, 58),
                at("Puissance : 120%", 30, 150, 320, 186),
                at("44", 900, 235, 955, 267),
                at("Vitesse", 30, 420, 220, 460),
                at("78", 900, 495, 955, 535),
                at("Passer", 30, 650, 170, 686),
                at("1", 900, 650, 920, 686)));
        assertTrue(result.contains("Puissance : non lue"));
        assertTrue(result.contains("Vitesse : 78 mm/s"));
    }

}
