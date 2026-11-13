package ca.jarvis.bridge;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public final class EasySetPanelStateTest {
    private static EasySetValues.Line t(String s) {
        return new EasySetValues.Line(s, 40, 100, 420, 142);
    }

    @Test public void closedXtoolSettingsWithClickInstruction() {
        assertEquals(EasySetPanelState.State.CLOSED,
                EasySetPanelState.classify(Arrays.asList(
                        t("Conception"), t("Paramètres"), t("Sortie"),
                        t("Type de traitement"), t("Réglages"),
                        t("Volet EasySet"),
                        t("Cliquer sur le volet EasySet pour définir les"),
                        t("paramètres"))));
    }

    @Test public void closedXtoolSettingsWithoutRecognizedHintStillRequiresContext() {
        assertEquals(EasySetPanelState.State.CLOSED,
                EasySetPanelState.classify(Arrays.asList(
                        t("Volet EasySet"), t("Paramètres"), t("Réglages"))));
        assertEquals(EasySetPanelState.State.UNKNOWN,
                EasySetPanelState.classify(Arrays.asList(t("Volet EasySet"))));
        assertEquals(EasySetPanelState.State.UNKNOWN,
                EasySetPanelState.classify(Arrays.asList(
                        t("Volet EasySet"), t("Conception"))));
    }

    @Test public void openedPanelWithNoNumbersMustNotBeCalledClosed() {
        assertEquals(EasySetPanelState.State.OPEN,
                EasySetPanelState.classify(Arrays.asList(
                        t("Volet EasySet"), t("Puissance(%)"),
                        t("Vitesse(mm/s)"), t("Passer"))));
    }

    @Test public void partiallyVisibleOpenPanelMustNotBeCalledClosed() {
        assertEquals(EasySetPanelState.State.UNKNOWN,
                EasySetPanelState.classify(Arrays.asList(
                        t("Volet EasySet"), t("Paramètres"),
                        t("Réglages"), t("Puissance(%)"))));
    }

    @Test public void rejectJarvisOwnOverlayOrUnrelatedScreen() {
        assertEquals(EasySetPanelState.State.UNKNOWN,
                EasySetPanelState.classify(Arrays.asList(
                        t("JARVIS • RÉSULTAT EASYSET"), t("Volet EasySet"),
                        t("Paramètres"), t("Sortie"))));
        assertEquals(EasySetPanelState.State.UNKNOWN,
                EasySetPanelState.classify(Collections.emptyList()));
        assertEquals(EasySetPanelState.State.UNKNOWN,
                EasySetPanelState.classify(Arrays.asList(
                        t("Réglages"), t("Puissance"), t("Vitesse"))));
    }
}
