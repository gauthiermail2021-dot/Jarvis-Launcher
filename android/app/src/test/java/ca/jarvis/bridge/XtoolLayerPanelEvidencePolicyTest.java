package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class XtoolLayerPanelEvidencePolicyTest {
    @Test public void requiresTwoDifferentColors() {
        assertTrue(XtoolLayerPanelEvidencePolicy.hasMultipleColors(true, false, true));
        assertTrue(XtoolLayerPanelEvidencePolicy.hasMultipleColors(false, true, true));
        assertFalse(XtoolLayerPanelEvidencePolicy.hasMultipleColors(false, false, true));
        assertFalse(XtoolLayerPanelEvidencePolicy.hasMultipleColors(false, false, false));
    }
}
