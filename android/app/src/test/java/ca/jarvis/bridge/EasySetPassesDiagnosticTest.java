package ca.jarvis.bridge;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public final class EasySetPassesDiagnosticTest {
    private static EasySetValues.Line at(String s, int l, int t, int r, int b) {
        return new EasySetValues.Line(s, l, t, r, b);
    }

    private static java.util.List<EasySetValues.Line> baseline() {
        return Arrays.asList(
                at("Volet EasySet", 20, 20, 300, 62),
                at("Puissance", 58, 170, 240, 206),
                at("63", 753, 245, 812, 285),
                at("Vitesse", 58, 393, 240, 428),
                at("152", 746, 477, 814, 512),
                at("Passer", 60, 610, 225, 645));
    }

    @Test public void reportsDigitsReadAfterFocusedPassOnly() {
        EasySetPassesDiagnostic diagnostic = new EasySetPassesDiagnostic(baseline());
        diagnostic.readColumn(Collections.emptyList());
        diagnostic.readCrop(Collections.singletonList(at("1", 771, 611, 799, 644)));
        var enhanced = Arrays.asList(at("1", 771, 611, 799, 644));
        String finalResult = EasySetValues.diagnose(baseline(), enhanced);
        String detail = diagnostic.append(finalResult);
        assertTrue(detail.contains("Libellé : repéré"));
        assertTrue(detail.contains("Écran : aucun petit caractère"));
        assertTrue(detail.contains("Colonne droite : aucun petit caractère"));
        assertTrue(detail.contains("Zone agrandie : «1» ; accepté"));
        assertTrue(detail.contains("Association : acceptée"));
        assertTrue(detail.contains("Passages : 1"));
        assertFalse(detail.contains("39.8mm"));
    }

    @Test public void differentiatesUnrecognizedLetterFromNumericDigit() {
        EasySetPassesDiagnostic diagnostic = new EasySetPassesDiagnostic(baseline());
        diagnostic.readColumn(Collections.singletonList(at("l", 770, 611, 788, 642)));
        diagnostic.readCrop(Collections.singletonList(at("›", 798, 611, 812, 645)));
        var extras = Arrays.asList(
                at("l", 770, 611, 788, 642),
                at("›", 798, 611, 812, 645));
        String detail = diagnostic.append(EasySetValues.diagnose(baseline(), extras));
        assertTrue(detail.contains("Colonne droite : «l» ; non associé"));
        assertTrue(detail.contains("Zone agrandie : «›» ; non associé"));
        assertTrue(detail.contains("Association : refusée"));
        assertTrue(detail.contains("Passages : non lus"));
    }

    @Test public void digitOutsideExpectedRowIsNotAssigned() {
        EasySetPassesDiagnostic diagnostic = new EasySetPassesDiagnostic(baseline());
        diagnostic.readColumn(Collections.singletonList(at("1", 772, 300, 795, 345)));
        String detail = diagnostic.append(EasySetValues.diagnose(
                baseline(), Collections.singletonList(at("1", 772, 300, 795, 345))));
        assertTrue(detail.contains("Passages : non lus"));
        assertTrue(detail.contains("Association : refusée"));
        assertTrue(detail.contains("aucun petit caractère détecté"));
    }

    @Test public void displaysSkippedStepsAndNeverPersistsRawOcr() {
        EasySetPassesDiagnostic diagnostic = new EasySetPassesDiagnostic(baseline());
        diagnostic.columnSkipped("OCR indisponible");
        diagnostic.cropSkipped("zone non lisible");
        String detail = diagnostic.append(EasySetValues.diagnose(baseline()));
        assertTrue(detail.contains("non exécutée (OCR indisponible)"));
        assertTrue(detail.contains("non exécuté (zone non lisible)"));
        assertTrue(detail.contains("DIAGNOSTIC PASSER (temporaire)"));
    }

    @Test public void refusesToExposeUnrelatedText() {
        var base = Arrays.asList(
                at("Volet EasySet", 30, 20, 290, 70),
                at("Puissance", 60, 150, 250, 190),
                at("Vitesse", 60, 365, 250, 405),
                at("Passer", 60, 610, 180, 647),
                at("COMPTE BANCAIRE : 1234567890", 700, 610, 850, 645),
                at("Montant 1337 dollars", 700, 652, 855, 688));
        EasySetPassesDiagnostic diagnostic = new EasySetPassesDiagnostic(base);
        String detail = diagnostic.append(EasySetValues.diagnose(base));
        assertFalse(detail.contains("COMPTE"));
        assertFalse(detail.contains("1234567890"));
        assertFalse(detail.contains("1337 dollars"));
        assertTrue(detail.contains("DIAGNOSTIC PASSER"));
    }

    @Test public void nonEasySetScreensHaveNoDiagnosticAppendix() {
        EasySetPassesDiagnostic diagnostic = new EasySetPassesDiagnostic(
                Collections.singletonList(at("Other app", 0, 0, 100, 35)));
        String result = "Paramètres EasySet non reconnus.";
        assertEquals(result, diagnostic.append(result));
    }
}
