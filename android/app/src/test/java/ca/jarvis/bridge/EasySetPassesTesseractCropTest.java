package ca.jarvis.bridge;

import org.junit.Test;

import static org.junit.Assert.*;

public final class EasySetPassesTesseractCropTest {
    @Test public void acceptsClonedNumericOnlyField() {
        // The field cloned from the xTool screenshot is ~78 x 62 px.
        assertTrue(EasySetPassesTesseract.acceptsRegion(
                78, 62, 0, 0, 78, 62));
    }

    @Test public void rejectsWholeScreenForOcr() {
        assertFalse(EasySetPassesTesseract.acceptsRegion(
                869, 1536, 0, 0, 869, 1536));
    }

    @Test public void acceptsSmallOriginalScreenRoi() {
        assertTrue(EasySetPassesTesseract.acceptsRegion(
                869, 1536, 734, 558, 812, 620));
    }

    @Test public void rejectsOutOfBoundsCrop() {
        assertFalse(EasySetPassesTesseract.acceptsRegion(
                78, 62, -1, 0, 78, 62));
    }
}
