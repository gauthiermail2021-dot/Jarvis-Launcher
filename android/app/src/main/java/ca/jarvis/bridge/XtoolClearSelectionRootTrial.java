package ca.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Deliberately limited trial of Android's explicit CLEAR_SELECTION on the
 * exact xTool window root observed in the Samsung 1.2.75 diagnostic.
 * No gestures, no layer operations, no machine settings or commands.
 */
final class XtoolClearSelectionRootTrial {
    private XtoolClearSelectionRootTrial() { }

    static String performOnce(AccessibilityService service) {
        AccessibilityNodeInfo root = null;
        try {
            root = service.getRootInActiveWindow();
            if (root == null || root.getPackageName() == null
                    || !EasySetForegroundScanPolicy.XCS_PACKAGE
                            .contentEquals(root.getPackageName())) {
                return "Action CLEAR_SELECTION non tentée : fenêtre xTool absente.";
            }
            if (root.getClassName() == null
                    || !"android.widget.FrameLayout".contentEquals(
                            root.getClassName())) {
                return "Action non tentée : racine inattendue ("
                        + safe(root.getClassName()) + ").";
            }
            Rect bounds = new Rect();
            root.getBoundsInScreen(bounds);
            if (!root.isVisibleToUser() || bounds.width() < 500
                    || bounds.height() < 700) {
                return "Action non tentée : cadre Android non visible ou trop petit.";
            }

            boolean advertised = false;
            for (AccessibilityNodeInfo.AccessibilityAction action
                    : root.getActionList()) {
                if (action.getId() == AccessibilityNodeInfo.ACTION_CLEAR_SELECTION) {
                    advertised = true;
                    break;
                }
            }
            if (!advertised) {
                return "Action non tentée : CLEAR_SELECTION non exposée par xTool.";
            }

            boolean before = root.isSelected();
            boolean accepted = root.performAction(
                    AccessibilityNodeInfo.ACTION_CLEAR_SELECTION);
            return "TEST ANDROID — ACTION_CLEAR_SELECTION (une fois)\n"
                    + "Racine : android.widget.FrameLayout\n"
                    + "selected avant = " + before + "\n"
                    + "Action exposée = oui\n"
                    + "performAction = " + (accepted ? "ACCEPTÉE" : "REFUSÉE") + "\n"
                    + "Attention : acceptée ne confirme PAS que les formes "
                    + "xTool sont désélectionnées. Vérifier visuellement.\n"
                    + "Aucun clic sur l'écran ni changement de paramètres.";
        } catch (RuntimeException failure) {
            return "Action CLEAR_SELECTION non confirmée : "
                    + failure.getClass().getSimpleName()
                    + ". Aucun autre clic envoyé.";
        } finally {
            if (root != null) root.recycle();
        }
    }

    private static String safe(CharSequence raw) {
        return raw == null ? "inconnu" : raw.toString();
    }
}
