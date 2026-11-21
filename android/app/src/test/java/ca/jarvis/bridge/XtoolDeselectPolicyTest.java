package ca.jarvis.bridge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Guards geometry used by the one-shot, non-machine canvas selection test. */
public final class XtoolDeselectPolicyTest {
    @Test public void acceptsBoundedBlankCornerAboveConceptionTab() {
        assertTrue(XtoolLayersPolicy.validBlankCanvasCorner(
                1353, 760, 1440, 3088, 2000));
    }

    @Test public void rejectsControlsAndUnexpectedCanvasPositions() {
        assertFalse(XtoolLayersPolicy.validBlankCanvasCorner(
                720, 760, 1440, 3088, 2000)); // drawing center
        assertFalse(XtoolLayersPolicy.validBlankCanvasCorner(
                1353, 3000, 1440, 3088, 2000)); // bottom toolbar
        assertFalse(XtoolLayersPolicy.validBlankCanvasCorner(
                1353, 760, 1440, 3088, 0)); // no known tab
    }

    @Test public void bottomLayersControlMustBeOnScreen() {
        assertTrue(XtoolLayersPolicy.validBottomButton(
                160, 2600, 350, 2740, 1440, 3088));
        assertFalse(XtoolLayersPolicy.validBottomButton(
                0, 100, 1440, 2800, 1440, 3088));
    }
}
