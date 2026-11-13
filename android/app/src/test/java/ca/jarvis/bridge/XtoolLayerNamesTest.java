package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class XtoolLayerNamesTest {
    @Test public void recognizesFrenchLayerColors() {
        assertEquals("ROUGE", XtoolLayerNames.classify("Calque rouge"));
        assertEquals("VERT", XtoolLayerNames.classify("Vert"));
        assertEquals("BLEU", XtoolLayerNames.classify("Bleue"));
        assertEquals("CALQUE", XtoolLayerNames.classify("Calques"));
    }

    @Test public void recognizesEnglishLabelsAndRejectsFalseMatches() {
        assertEquals("ROUGE", XtoolLayerNames.classify("Red layer"));
        assertEquals("VERT", XtoolLayerNames.classify("Green"));
        assertEquals("BLEU", XtoolLayerNames.classify("Blue"));
        assertEquals("AUCUN", XtoolLayerNames.classify("Convertir"));
        assertEquals("AUCUN", XtoolLayerNames.classify("Passages 1"));
    }
}
