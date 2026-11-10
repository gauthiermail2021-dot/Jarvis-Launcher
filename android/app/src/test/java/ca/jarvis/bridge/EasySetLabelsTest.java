package ca.jarvis.bridge;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertTrue;

public class EasySetLabelsTest {
    @Test public void recognizesFrenchAccents() {
        String result = EasySetLabels.diagnose(Arrays.asList(
                "EasySet", "Puissance (%)", "Vitesse (mm/s)", "Passer"));
        assertTrue(result.contains("EasySet reconnu"));
        assertTrue(result.contains("Puissance : repérée"));
        assertTrue(result.contains("Vitesse : repérée"));
        assertTrue(result.contains("Passages : repérés"));
    }

    @Test public void ignoresNonEasySetScreens() {
        assertTrue(EasySetLabels.diagnose(Collections.singletonList("Bonjour"))
                .contains("non reconnu"));
    }

    @Test public void doesNotRecordRecognizedNumbers() {
        String result = EasySetLabels.diagnose(Arrays.asList(
                "EasySet", "Puissance 88%", "Vitesse 123", "Passages 9"));
        assertTrue(!result.contains("88") && !result.contains("123")
                && !result.contains(" 9"));
    }
}
