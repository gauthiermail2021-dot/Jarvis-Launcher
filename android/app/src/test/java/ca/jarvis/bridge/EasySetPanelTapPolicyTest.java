package ca.jarvis.bridge;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public final class EasySetPanelTapPolicyTest {
    @Test public void recognizesOnlyActualVoletEasySetClickedRow() {
        assertTrue(EasySetPanelTapPolicy.namesEasySetRow(
                Arrays.<CharSequence>asList("Volet EasySet"), null));
        assertTrue(EasySetPanelTapPolicy.namesEasySetRow(
                Collections.<CharSequence>emptyList(), "Volet EasySet"));
        assertTrue(EasySetPanelTapPolicy.namesEasySetRow(
                Arrays.<CharSequence>asList("Volet EasySet >"), null));
        assertFalse(EasySetPanelTapPolicy.namesEasySetRow(
                Arrays.<CharSequence>asList("Paramétrage en un clic"), null));
        assertFalse(EasySetPanelTapPolicy.namesEasySetRow(
                Arrays.<CharSequence>asList(
                        "Cliquer sur le volet EasySet pour définir les paramètres"), null));
        assertFalse(EasySetPanelTapPolicy.namesEasySetRow(
                Collections.<CharSequence>emptyList(), null));
    }

    @Test public void namedEasySetTapDoesNotNeedPriorClosedFrame() {
        assertTrue(EasySetPanelTapPolicy.shouldRequest(
                "com.makeblock.xcs", true, true, true, false, false,
                2_000L, 0L, 0L));
        // A named click also works after an earlier SUCCESSFUL scan.
        assertTrue(EasySetPanelTapPolicy.shouldRequest(
                "com.makeblock.xcs", true, true, true, false, false,
                20_000L, 10_000L, 10_000L));
        assertFalse(EasySetPanelTapPolicy.shouldRequest(
                "com.makeblock.xcs", true, true, true, false, false,
                10_300L, 10_000L, 0L));
    }

    @Test public void unlabeledXToolCustomTapHasBoundedFallback() {
        assertTrue(EasySetPanelTapPolicy.shouldRequest(
                "com.makeblock.xcs", true, true, false, true, true,
                9_000L, 0L, 0L));
        assertFalse(EasySetPanelTapPolicy.shouldRequest(
                "com.makeblock.xcs", true, true, false, true, true,
                9_500L, 0L, 9_000L));
        assertFalse(EasySetPanelTapPolicy.shouldRequest(
                "com.makeblock.xcs", true, true, false, true, false,
                20_000L, 0L, 0L));
        assertFalse(EasySetPanelTapPolicy.shouldRequest(
                "com.makeblock.xcs", true, true, false, false, true,
                20_000L, 0L, 0L));
    }

    @Test public void noConsentOrOtherAppNeverTriggersScan() {
        assertFalse(EasySetPanelTapPolicy.shouldRequest(
                "com.google.android.gm", true, true, true, false, true,
                20_000L, 0L, 0L));
        assertFalse(EasySetPanelTapPolicy.shouldRequest(
                "com.makeblock.xcs", false, true, true, false, true,
                20_000L, 0L, 0L));
        assertFalse(EasySetPanelTapPolicy.shouldRequest(
                "com.makeblock.xcs", true, false, true, false, true,
                20_000L, 0L, 0L));
    }
}
