package ca.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

/**
 * One-shot UI navigation test, explicitly invoked by the human from Jarvis.
 * Simulates precisely one short touch at the center of the accessible, visible
 * xTool row named "Volet EasySet". No fixed screen coordinates, scrolling,
 * parameter changes, repeat loops or machine commands.
 */
final class EasySetOneShotClickTest {
    private EasySetOneShotClickTest() { }

    interface Outcome {
        void onComplete(boolean opened, String details);
    }

    static void run(AccessibilityService service, Handler handler, Runnable finished) {
        execute(service, handler, (opened, details) -> {
            finished.run();
            JarvisBubbleService.showEasySetResult("TEST TOUCHER EASYSET\n" + details
                    + "\nAucun paramètre ou commande laser modifié.");
        });
    }

    /** Silent one-shot after SVG handoff; no diagnostic overlay on top of xTool. */
    static void runAutomatically(AccessibilityService service, Handler handler,
                                 Outcome callback) {
        execute(service, handler, callback);
    }

    /** Read-only preflight. No action or screenshot. */
    static boolean isXtoolPanelReady(AccessibilityService service) {
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        try {
            if (!isXtool(root)) return false;
            if (panelOpen(root)) return true;
            AccessibilityNodeInfo target = findExact(root, 0);
            if (target == null) return false;
            try { return target.isVisibleToUser(); }
            finally { target.recycle(); }
        } finally {
            if (root != null) root.recycle();
        }
    }

    private static void execute(AccessibilityService service, Handler handler,
                                Outcome callback) {
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        try {
            // Must be the active xTool window, never Jarvis, Gmail or ChatGPT.
            if (!isXtool(root)) {
                end("Fenêtre xTool non active ou non accessible.", false, callback);
                return;
            }
            if (panelOpen(root)) {
                end("Volet déjà ouvert : aucun toucher envoyé.", true, callback);
                return;
            }
            AccessibilityNodeInfo target = findExact(root, 0);
            if (target == null) {
                end("Rangée « Volet EasySet » introuvable.", false, callback);
                return;
            }
            Rect bounds = new Rect();
            try {
                if (!target.isVisibleToUser()) {
                    end("Rangée identifiée, mais non visible.", false, callback);
                    return;
                }
                target.getBoundsInScreen(bounds);
            } finally {
                target.recycle();
            }

            // Guard against stale or unexpectedly large/out-of-screen bounds.
            // The touch location is ALWAYS derived from the exact named node.
            android.graphics.Point display = new android.graphics.Point();
            android.view.WindowManager wm = (android.view.WindowManager)
                    service.getSystemService(android.content.Context.WINDOW_SERVICE);
            if (wm == null) {
                end("Dimensions de l'écran indisponibles.", false, callback);
                return;
            }
            wm.getDefaultDisplay().getRealSize(display);
            int x = bounds.centerX();
            int y = bounds.centerY();
            if (bounds.width() < 60 || bounds.height() < 20
                    || bounds.left < 0 || bounds.top < 0
                    || bounds.right > display.x || bounds.bottom > display.y
                    || x <= 0 || x >= display.x || y <= 0 || y >= display.y) {
                end("Zone tactile non valide : aucun toucher envoyé.", false, callback);
                return;
            }
            Path path = new Path();
            path.moveTo(x, y);
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0, 95L))
                    .build();

            // A successful dispatch means Android sent the touch; we still
            // verify whether the interface changed afterwards.
            boolean scheduled = service.dispatchGesture(gesture,
                    new AccessibilityService.GestureResultCallback() {
                        @Override public void onCompleted(GestureDescription description) {
                            handler.postDelayed(() -> {
                                AccessibilityNodeInfo after = service.getRootInActiveWindow();
                                try {
                                    end("Rangée : trouvée\nToucher Android : effectué\n"
                                            + "Volet ouvert : "
                                            + (isXtool(after) && panelOpen(after)
                                                ? "oui" : "non confirmé"),
                                            isXtool(after) && panelOpen(after), callback);
                                } finally {
                                    if (after != null) after.recycle();
                                }
                            }, 1000L);
                        }
                        @Override public void onCancelled(GestureDescription description) {
                            end("Toucher annulé par Android.", false, callback);
                        }
                    }, handler);
            if (!scheduled) {
                end("Android a refusé d'envoyer le toucher.", false, callback);
            }
        } catch (RuntimeException error) {
            end("Erreur Android : " + error.getClass().getSimpleName(), false, callback);
        } finally {
            if (root != null) root.recycle();
        }
    }

    private static void end(String details, boolean opened, Outcome callback) {
        callback.onComplete(opened, details);
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
