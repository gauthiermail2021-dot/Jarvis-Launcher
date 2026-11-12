package ca.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Explicit one-tap UI selection test on the visible row "rouge" in XCS.
 * Never uses a stored screen coordinate, never scrolls, never retries,
 * never changes treatments, EasySet fields or laser controls.
 */
final class XtoolRedLayerOneShot {
    private static final int MAX_DEPTH = 14;

    private XtoolRedLayerOneShot() { }

    static void run(AccessibilityService service, Handler handler, Runnable finished) {
        execute(service, handler, details -> {
            finished.run();
            JarvisBubbleService.showEasySetResult(
                    "TEST CALQUE ROUGE\n" + details
                    + "\nAucun parametre ni commande laser modifie.");
        });
    }

    interface Outcome {
        void complete(String description);
    }

    private static void execute(AccessibilityService service, Handler handler,
                                Outcome callback) {
        AccessibilityNodeInfo root = null;
        try {
            root = service.getRootInActiveWindow();
            if (!isXtool(root)) {
                callback.complete("xTool non actif : aucun toucher.");
                return;
            }

            AccessibilityNodeInfo red = findVisibleLabel(root, true, 0);
            if (red == null) {
                callback.complete("Libelle exact « rouge » absent ou non visible.\n"
                        + "Ouvrir le panneau Calque dans XCS.");
                return;
            }

            Rect bounds = new Rect();
            try {
                red.getBoundsInScreen(bounds);
            } finally {
                red.recycle();
            }

            // The named red row must be present in the actual layer sheet.
            if (!hasLayerSheet(root, bounds.top)) {
                callback.complete("Panneau Calque non confirme : aucun toucher.");
                return;
            }

            WindowManager wm = (WindowManager)
                    service.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                callback.complete("Dimensions de l'ecran indisponibles.");
                return;
            }
            Point display = new Point();
            wm.getDefaultDisplay().getRealSize(display);
            if (!XtoolLayerTapPolicy.isValidTarget(
                    bounds.left, bounds.top, bounds.right, bounds.bottom,
                    display.x, display.y)) {
                callback.complete("Position du calque rouge invalide : aucun toucher.");
                return;
            }

            int x = bounds.centerX();
            int y = bounds.centerY();
            Path path = new Path();
            path.moveTo(x, y);
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0, 95L))
                    .build();

            boolean dispatched = service.dispatchGesture(
                    gesture, new AccessibilityService.GestureResultCallback() {
                        @Override public void onCompleted(GestureDescription description) {
                            handler.postDelayed(() -> {
                                AccessibilityNodeInfo after = null;
                                try {
                                    after = service.getRootInActiveWindow();
                                    if (!isXtool(after)) {
                                        callback.complete("Toucher effectue, mais XCS n'est "
                                                + "plus au premier plan.");
                                        return;
                                    }
                                    AccessibilityNodeInfo selected =
                                            findVisibleLabel(after, true, 0);
                                    if (selected == null) {
                                        callback.complete("Toucher effectue a X=" + x
                                                + ", Y=" + y
                                                + ". Calque rouge non visible ensuite.\n"
                                                + "Verifier le resultat dans XCS.");
                                        return;
                                    }
                                    boolean selectedFlag;
                                    try {
                                        selectedFlag = selected.isSelected();
                                    } finally {
                                        selected.recycle();
                                    }
                                    callback.complete("Calque « rouge » trouve.\n"
                                            + "Toucher Android : effectue a X="
                                            + x + ", Y=" + y + ".\n"
                                            + (selectedFlag
                                                    ? "Selection confirmee par Android."
                                                    : "Selection a verifier visuellement "
                                                    + "(XCS n'expose pas forcement "
                                                    + "l'etat selectionne)."));
                                } catch (RuntimeException error) {
                                    callback.complete("Toucher effectue; verification "
                                            + "Android indisponible.");
                                } finally {
                                    if (after != null) after.recycle();
                                }
                            }, 550L);
                        }
                        @Override public void onCancelled(GestureDescription description) {
                            callback.complete("Toucher annule par Android.");
                        }
                    }, handler);
            if (!dispatched) {
                callback.complete("Android a refuse le toucher.");
            }
        } catch (RuntimeException error) {
            callback.complete("Erreur Android : " + error.getClass().getSimpleName());
        } finally {
            if (root != null) root.recycle();
        }
    }

    private static boolean hasLayerSheet(AccessibilityNodeInfo root, int redTop) {
        return findVisibleLayerHeading(root, redTop, 0);
    }

    private static boolean findVisibleLayerHeading(AccessibilityNodeInfo node,
                                                   int redTop, int depth) {
        if (node == null || depth > MAX_DEPTH || !isXtool(node)) return false;
        CharSequence text = node.getText();
        if (node.isVisibleToUser() && text != null
                && "calque".equalsIgnoreCase(text.toString().trim())) {
            Rect area = new Rect();
            node.getBoundsInScreen(area);
            if (area.top < redTop && area.bottom < redTop) return true;
        }
        int count = Math.min(40, node.getChildCount());
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                if (findVisibleLayerHeading(child, redTop, depth + 1)) return true;
            } finally {
                child.recycle();
            }
        }
        return false;
    }

    private static AccessibilityNodeInfo findVisibleLabel(
            AccessibilityNodeInfo node, boolean redOnly, int depth) {
        if (node == null || depth > MAX_DEPTH || !isXtool(node)) return null;
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        if (node.isVisibleToUser() && (
                (text != null && XtoolLayerTapPolicy.exactRed(text.toString()))
                || (description != null
                    && XtoolLayerTapPolicy.exactRed(description.toString())))) {
            return AccessibilityNodeInfo.obtain(node);
        }
        int count = Math.min(40, node.getChildCount());
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                AccessibilityNodeInfo match = findVisibleLabel(child, redOnly, depth + 1);
                if (match != null) return match;
            } finally {
                child.recycle();
            }
        }
        return null;
    }

    private static boolean isXtool(AccessibilityNodeInfo root) {
        return root != null && root.getPackageName() != null
                && EasySetForegroundScanPolicy.XCS_PACKAGE
                .contentEquals(root.getPackageName());
    }
}
