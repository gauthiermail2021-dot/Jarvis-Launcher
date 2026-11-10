package ca.jarvis.bridge;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertTrue;

public class EasySetLabelsTest {
    @Test public void recognizesFrenchEasySetScreen() {
        String result = EasySetLabels.diagnose(Arrays.asList(
                "Volet EasySet", "Puissance (%)", "Vitesse (mm/s)", "Passer"));
        assertTrue(result.contains("EasySet reconnu"));
        assertTrue(result.contains("Puissance : oui"));
        assertTrue(result.contains("Vitesse : oui"));
        assertTrue(result.contains("Passages : oui"));
    }

    @Test public void rejectsUnrelatedScreen() {
        assertTrue(EasySetLabels.diagnose(Collections.singletonList("Bonjour"))
                .contains("non reconnus"));
    }

    @Test public void rejectsJarvisOwnDiagnosticScreenEvenWithLabels() {
        String result = EasySetLabels.diagnose(Arrays.asList(
                "JARVIS 1.2.24 • EASYSET", "ANALYSE VISUELLE — LECTURE SEULE",
                "Puissance", "Vitesse", "Passages"));
        assertTrue(result.contains("Capture de Jarvis"));
        assertTrue(!result.contains("EasySet reconnu"));
    }

    @Test public void rejectsJarvis125WindowDespiteLabels() {
        String result = EasySetLabels.diagnose(Arrays.asList(
                "JARVIS 1.2.25 • EASYSET", "Puissance", "Vitesse", "Passages"));
        assertTrue(result.contains("Capture de Jarvis"));
    }

    @Test public void rejectsEasySetHeadingWithoutParameters() {
        assertTrue(EasySetLabels.diagnose(Arrays.asList("Volet EasySet", "Paramètres"))
                .contains("non reconnus"));
    }

    @Test public void recognizesTwoFieldsAsPartial() {
        String result = EasySetLabels.diagnose(Arrays.asList(
                "EasySet", "Puissance", "Vitesse"));
        assertTrue(result.contains("Puissance : oui"));
        assertTrue(result.contains("Vitesse : oui"));
        assertTrue(result.contains("Passages : non"));
    }

    @Test public void neverPersistsNumbersInSummary() {
        String result = EasySetLabels.diagnose(Arrays.asList(
                "EasySet", "Puissance 88%", "Vitesse 123", "Passages 9"));
        assertTrue(!result.contains("88") && !result.contains("123")
                && !result.contains(" 9"));
    }
}
