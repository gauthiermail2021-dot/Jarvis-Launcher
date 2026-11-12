package ca.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

/**
 * One-shot, user-requested UI navigation test. The ONLY action permitted is
 * ACTION_CLICK on the exact accessible "Volet EasySet" button in xTool XCS.
 * No setting fields, gestures, start commands, or changes to laser parameters.
 */
final class EasySetOneShotClickTest {
    private EasySetOneShotClickTest() { }

    static void run(AccessibilityService service, Handler handler, Runnable finished) {
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        try {
            if (!isXtool(root)) {
                end("Fenêtre xTool non active ou non accessible.", finished);
                return;
            }
            if (panelOpen(root)) {
                end("Volet déjà ouvert. Aucun clic envoyé.", finished);
                return;
            }
            AccessibilityNodeInfo target = findExact(root, 0);
            if (target == null) {
                end("Bouton « Volet EasySet » introuvable.", finished);
                return;
            }
            boolean accepted;
            try {
                accepted = target.isClickable()
                        && target.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            } finally {
                target.recycle();
            }
            if (!accepted) {
                end("Bouton trouvé. ACTION_CLICK : refusée.", finished);
                return;
            }
            // Acceptance by Android is not proof that xTool opened the panel.
            handler.postDelayed(() -> {
                AccessibilityNodeInfo after = service.getRootInActiveWindow();
                try {
                    end("Bouton : trouvé\nACTION_CLICK : acceptée\n"
                            + "Volet ouvert : "
                            + (isXtool(after) && panelOpen(after)
                                ? "oui" : "non confirmé"), finished);
                } finally {
                    if (after != null) after.recycle();
                }
            }, 900L);
        } catch (RuntimeException e) {
            end("Erreur Android : " + e.getClass().getSimpleName(), finished);
        } finally {
            if (root != null) root.recycle();
        }
    }

    private static void end(String details, Runnable finished) {
        finished.run();
        JarvisBubbleService.showEasySetResult("TEST CLIC EASYSET\n" + details
                + "\nAucun paramètre ou commande laser modifié.");
    }

    private static boolean isXtool(AccessibilityNodeInfo root) {
        return root != null && root.getPackageName() != null
                && EasySetForegroundScanPolicy.XCS_PACKAGE.contentEquals(
                        root.getPackageName());
    }

    private static boolean panelOpen(AccessibilityNodeInfo root) {
        return hasVisibleHeading(root, "puissance")
                && hasVisibleHeading(root, "vitesse")
                && hasVisibleHeading(root, "passer");
    }

    private static boolean hasVisibleHeading(AccessibilityNodeInfo root, String heading) {
        if (root == null) return false;
        List<AccessibilityNodeInfo> matches = root.findAccessibilityNodeInfosByText(heading);
        if (matches == null) return false;
        boolean found = false;
        for (AccessibilityNodeInfo node : matches) {
            if (node == null) continue;
            try {
                CharSequence txt = node.getText();
                if (node.isVisibleToUser() && txt != null
                        && EasySetLabels.normalize(txt.toString()).startsWith(heading)) {
                    found = true;
                }
            } finally {
                node.recycle();
            }
        }
        return found;
    }

    private static AccessibilityNodeInfo findExact(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 12 || !isXtool(node)) return null;
        CharSequence txt = node.getText();
        CharSequence desc = node.getContentDescription();
        if (node.isVisibleToUser() && node.isClickable()
                && ((txt != null && "Volet EasySet".equalsIgnoreCase(txt.toString().trim()))
                || (desc != null && "Volet EasySet".equalsIgnoreCase(desc.toString().trim())))) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0, n = Math.min(40, node.getChildCount()); i < n; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                AccessibilityNodeInfo found = findExact(child, depth + 1);
                if (found != null) return found;
            } finally {
                child.recycle();
            }
        }
        return null;
    }
}
