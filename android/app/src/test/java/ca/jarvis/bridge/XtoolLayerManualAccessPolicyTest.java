package ca.jarvis.bridge;

import org.junit.Test;
import static org.junit.Assert.*;

public class XtoolLayerManualAccessPolicyTest {
    @Test public void manualCommandCanOperateOnForegroundEditorAfterSessionEnds() {
        assertTrue(XtoolLayerManualAccessPolicy.permitted(true, false, true));
    }

    @Test public void autoCommandRequiresActiveSvgSession() {
        assertFalse(XtoolLayerManualAccessPolicy.permitted(true, false, false));
        assertTrue(XtoolLayerManualAccessPolicy.permitted(true, true, false));
    }

    @Test public void neverOperateOnOtherForegroundApp() {
        assertFalse(XtoolLayerManualAccessPolicy.permitted(false, true, true));
        assertFalse(XtoolLayerManualAccessPolicy.permitted(false, false, true));
        assertFalse(XtoolLayerManualAccessPolicy.permitted(false, true, false));
    }
}
