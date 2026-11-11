package ca.jarvis.bridge;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public final class EasySetAccessiblePanelTest {
    @Test public void recognizesThreeVisibleSettingsLabelsWithoutNumbers() {
        assertEquals(EasySetAccessiblePanel.State.OPEN,
                EasySetAccessiblePanel.classify(Arrays.asList(
                        "Volet EasySet", "Puissance(%)", "Vitesse(mm/s)", "Passer")));
    }

    @Test public void recognizesCollapsedEasySetFromTheAccessibleRowAlone() {
        assertEquals(EasySetAccessiblePanel.State.CLOSED,
                EasySetAccessiblePanel.classify(Arrays.asList(
                        "Conception", "Volet EasySet", "Paramètres")));
    }

    @Test public void doesNotTreatInstructionsOrPartialHeadingsAsOpen() {
        assertEquals(EasySetAccessiblePanel.State.UNKNOWN,
                EasySetAccessiblePanel.classify(Arrays.asList(
                        "Cliquer sur le volet EasySet pour définir les paramètres",
                        "Puissance(%)", "Vitesse(mm/s)")));
        assertEquals(EasySetAccessiblePanel.State.UNKNOWN,
                EasySetAccessiblePanel.classify(Arrays.asList(
                        "Puissance", "Vitesse", "1")));
    }

    @Test public void rejectsUnrelatedLongWindowContent() {
        assertEquals(EasySetAccessiblePanel.State.UNKNOWN,
                EasySetAccessiblePanel.classify(Arrays.asList(
                        "Bienvenue dans Jarvis, cliquer sur le volet EasySet",
                        "Rien à voir avec la fenêtre"));
        assertEquals(EasySetAccessiblePanel.State.UNKNOWN,
                EasySetAccessiblePanel.classify(Collections.emptyList()));
    }

    @Test public void supportsEnglishLabelsWhenXtoolLocaleChanges() {
        assertEquals(EasySetAccessiblePanel.State.OPEN,
                EasySetAccessiblePanel.classify(Arrays.asList(
                        "EasySet", "Power(%)", "Speed(mm/s)", "Passes")));
    }
}
