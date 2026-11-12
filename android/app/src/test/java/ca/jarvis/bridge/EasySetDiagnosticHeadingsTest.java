package ca.jarvis.bridge;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public final class EasySetDiagnosticHeadingsTest {
    @Test public void keepsExactLabelsAndDuplicateSourcesInOrder() {
        String labels = EasySetDiagnosticHeadings.enumerate(Arrays.asList(
                EasySetDiagnosticHeadings.one("Texte", "Volet EasySet"),
                EasySetDiagnosticHeadings.one("Texte", "Puissance(%)"),
                EasySetDiagnosticHeadings.one("Description", "Puissance(%)"),
                EasySetDiagnosticHeadings.one("Texte", "Vitesse(mm/s)"),
                EasySetDiagnosticHeadings.one("Texte", "Passer")));
        assertTrue(labels.contains("1. Texte : «Volet EasySet»"));
        assertTrue(labels.contains("2. Texte : «Puissance(%)»"));
        assertTrue(labels.contains("3. Description : «Puissance(%)»"));
        assertTrue(labels.contains("4. Texte : «Vitesse(mm/s)»"));
        assertTrue(labels.contains("5. Texte : «Passer»"));
    }

    @Test public void hidesValuesAndAcceptsEmptyResult() {
        assertEquals("Texte : [libellé avec nombre masqué]",
                EasySetDiagnosticHeadings.one("Texte", "Puissance 60"));
        assertEquals("Aucun libellé relevé",
                EasySetDiagnosticHeadings.enumerate(Collections.emptyList()));
    }
}
