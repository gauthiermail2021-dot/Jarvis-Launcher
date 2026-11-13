package ca.jarvis.bridge;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public final class EasySetControlPropertiesTest {
    @Test public void recognizesOnlyTheExpectedEasySetControls() {
        assertTrue(EasySetControlProperties.matchesHeading("Volet EasySet"));
        assertTrue(EasySetControlProperties.matchesHeading("Puissance(%)"));
        assertTrue(EasySetControlProperties.matchesHeading("Vitesse(mm/s)"));
        assertTrue(EasySetControlProperties.matchesHeading("Passer"));
        assertTrue(EasySetControlProperties.matchesHeading("Passages"));
        assertFalse(EasySetControlProperties.matchesHeading("Paramétrage en un clic"));
        assertFalse(EasySetControlProperties.matchesHeading("Conception"));
        assertFalse(EasySetControlProperties.matchesHeading("1"));
        assertFalse(EasySetControlProperties.matchesHeading(null));
    }

    @Test public void showsStateBoundsClassAndActionsWithoutPerformingAny() {
        String line = EasySetControlProperties.line("android.widget.TextView",
                false, false, true, "10,20 — 200,90",
                Arrays.asList("FOCUS", "CLICK"));
        assertTrue(line.contains("Type : android.widget.TextView"));
        assertTrue(line.contains("Cliquable : non"));
        assertTrue(line.contains("Modifiable : non"));
        assertTrue(line.contains("Focus : oui"));
        assertTrue(line.contains("Position écran : 10,20 — 200,90"));
        assertTrue(line.contains("Actions Android : FOCUS, CLICK"));
    }

    @Test public void formatsNoActionsAndNoNodes() {
        String line = EasySetControlProperties.line(null,
                false, false, false, null, Collections.emptyList());
        assertTrue(line.contains("Type : non exposé"));
        assertTrue(line.contains("Position écran : inconnue"));
        assertTrue(line.contains("Actions Android : aucune"));
        assertTrue(EasySetControlProperties.report(Collections.emptyList(), 97)
                .contains("97 nœuds examinés"));
    }

    @Test public void countsLabelsAndExplainsParentRelation() {
        String report = EasySetControlProperties.report(Arrays.asList(
                "Texte : «Volet EasySet»",
                "Texte : «Puissance(%)»",
                "Texte : «Vitesse(mm/s)»"), 102);
        assertTrue(report.contains("3 entrées EasySet"));
        assertTrue(report.contains("102 nœuds"));
        assertTrue(report.contains("parent"));
    }
}
