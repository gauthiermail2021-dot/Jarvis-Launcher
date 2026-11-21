package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class XtoolLayerActivationPolicyTest {
    @Test public void clickableTargetWins() {
        assertEquals(XtoolLayerActivationPolicy.Strategy.ACTION_CLICK,
                XtoolLayerActivationPolicy.strategy(1, 1, 1, 1));
    }

    @Test public void namedTouchOnlyWhenAccessibilityHasNoClick() {
        assertEquals(XtoolLayerActivationPolicy.Strategy.NAMED_TOUCH,
                XtoolLayerActivationPolicy.strategy(1, 1, 1, 0));
    }

    @Test public void rejectAmbiguousOrAbsentTargets() {
        assertEquals(XtoolLayerActivationPolicy.Strategy.NONE,
                XtoolLayerActivationPolicy.strategy(0, 0, 0, 0));
        assertEquals(XtoolLayerActivationPolicy.Strategy.NONE,
                XtoolLayerActivationPolicy.strategy(2, 2, 2, 0));
        assertEquals(XtoolLayerActivationPolicy.Strategy.NONE,
                XtoolLayerActivationPolicy.strategy(1, 1, 1, 2));
        assertEquals(XtoolLayerActivationPolicy.Strategy.NONE,
                XtoolLayerActivationPolicy.strategy(1, 0, 1, 1));
    }
    @Test public void onlyAcceptedActionsCountAsSent() {
        assertTrue(XtoolLayerActivationPolicy.actionSent(
                "ACTION_CLICK acceptée sur Calques."));
        assertTrue(XtoolLayerActivationPolicy.actionSent(
                "Geste livré au contrôle nommé Calques."));
        assertFalse(XtoolLayerActivationPolicy.actionSent(
                "ACTION_CLICK refusée par xTool."));
        assertFalse(XtoolLayerActivationPolicy.actionSent(null));
    }
}
