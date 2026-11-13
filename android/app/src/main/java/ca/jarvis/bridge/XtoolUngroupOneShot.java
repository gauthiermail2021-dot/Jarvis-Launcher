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
 * One attempt to activate the exact accessible "Dégrouper" action in XCS.
 * No guessed screen coordinates, no settings or machine controls.
 *
 * Some XCS toolbars expose the label as a text-only accessibility node and
 * put the actual clickable action on a compact ancestor or an icon above it.
 * A gesture on the center of the label is therefore not reliable.
 */
final class XtoolUngroupOneShot {
    private static final int MAX_DEPTH = 14;

    interface Outcome {
        void onComplete(boolean touchSent, boolean verifiedUngrouped);
    }

    private static volatile String lastAttempt =
            "Aucun clic Dégrouper tenté depuis le dernier démarrage.";

    private XtoolUngroupOneShot() { }

    static String lastAttempt() {
        return lastAttempt;
    }

    /**
     * false: named control not yet available, caller may check later.
     * true: one named target was handled; do not retry and risk repeated clicks.
     */
    static boolean tryOnce(AccessibilityService service, Handler handler, Outcome callback) {
        AccessibilityNodeInfo root = null;
        AccessibilityNodeInfo label = null;
        AccessibilityNodeInfo target = null;
        try {
            root = service.getRootInActiveWindow();
            if (!isXtool(root)) return false;
            label = findVisibleUngroup(root, 0);
            if (label == null) return false;

            WindowManager wm = (WindowManager)
                    service.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                lastAttempt = "Fenêtre Android: dimensions indisponibles.";
                callback.onComplete(false, false);
                return true;
            }
            Point screen = new Point();
            wm.getDefaultDisplay().getRealSize(screen);
            Rect labelBounds = new Rect();
            label.getBoundsInScreen(labelBounds);

            if (!label.isEnabled()) {
                lastAttempt = "« Dégrouper » visible mais désactivé dans Android.";
                callback.onComplete(false, false);
                return true;
            }
            if (!XtoolUngroupPolicy.validBounds(labelBounds.left,
                    labelBounds.top, labelBounds.right, labelBounds.bottom,
                    screen.x, screen.y)) {
                lastAttempt = "Libellé Dégrouper: rectangle Android invalide.";
                callback.onComplete(false, false);
                return true;
            }

            // Prefer the actual Accessibility ACTION_CLICK on a bounded,
            // visible, enabled clickable node, never on a full-screen toolbar.
            target = closestClickableControl(label, screen);
            if (target != null) {
                if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    lastAttempt = "ACTION_CLICK acceptée sur le contrôle Dégrouper. "
                            + "Séparation visuelle non vérifiée.";
                    callback.onComplete(true, false);
                    return true;
                }
                Rect touch = new Rect();
                target.getBoundsInScreen(touch);
                dispatchTap(service, handler, touch, "contrôle cliquable", callback);
                return true;
            }

            // Some icon tiles have no ACTION_CLICK in the accessibility tree.
            // Only use a compact ancestor that contains the exact label and
            // has a meaningful icon space above it. No blind fixed offsets.
            target = compactIconTile(label, labelBounds, screen);
            if (target != null) {
                Rect touch = new Rect();
                target.getBoundsInScreen(touch);
                dispatchTap(service, handler, touch, "tuile icône+texte", callback);
                return true;
            }
            lastAttempt = "Libellé Dégrouper trouvé; aucun contrôle cliquable "
                    + "ou tuile compacte identifié. Aucun toucher envoyé.";
            callback.onComplete(false, false);
            return true;
        } catch (RuntimeException error) {
            lastAttempt = "Erreur Android lors du ciblage: "
                    + error.getClass().getSimpleName();
            callback.onComplete(false, false);
            return true;
        } finally {
            if (target != null) target.recycle();
            if (label != null) label.recycle();
            if (root != null) root.recycle();
        }
    }

    private static void dispatchTap(AccessibilityService service,
                                    Handler handler, Rect target,
                                    String kind, Outcome callback) {
        Path path = new Path();
        path.moveTo(target.centerX(), target.centerY());
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(
                        path, 0L, 95L)).build();
        boolean accepted = service.dispatchGesture(gesture,
                new AccessibilityService.GestureResultCallback() {
                    @Override public void onCompleted(GestureDescription description) {
                        lastAttempt = "Toucher Android envoyé sur " + kind
                                + ". Séparation visuelle non vérifiée.";
                        callback.onComplete(true, false);
                    }
                    @Override public void onCancelled(GestureDescription description) {
                        lastAttempt = "Toucher Dégrouper annulé par Android (" + kind + ").";
                        callback.onComplete(false, false);
                    }
                }, handler);
        if (!accepted) {
            lastAttempt = "Android a refusé le toucher sur " + kind + ".";
            callback.onComplete(false, false);
        }
    }

    private static AccessibilityNodeInfo closestClickableControl(
            AccessibilityNodeInfo label, Point screen) {
        AccessibilityNodeInfo current = AccessibilityNodeInfo.obtain(label);
        for (int level = 0; current != null && level <= 4; level++) {
            if (!isXtool(current) || !current.isVisibleToUser()
                    || !current.isEnabled()) break;
            Rect rect = new Rect();
            current.getBoundsInScreen(rect);
            if (current.isClickable() && XtoolUngroupPolicy.validControlBounds(
                    rect.left, rect.top, rect.right, rect.bottom,
                    screen.x, screen.y)) {
                return current; // caller owns obtained node
            }
            AccessibilityNodeInfo next = current.getParent();
            current.recycle();
            current = next;
        }
        if (current != null) current.recycle();
        return null;
    }

    private static AccessibilityNodeInfo compactIconTile(
            AccessibilityNodeInfo label, Rect text, Point screen) {
        AccessibilityNodeInfo current = label.getParent();
        for (int level = 0; current != null && level < 3; level++) {
            if (!isXtool(current) || !current.isVisibleToUser()
                    || !current.isEnabled()) break;
            Rect rect = new Rect();
            current.getBoundsInScreen(rect);
            if (XtoolUngroupPolicy.isCompactIconTile(
                    text.left, text.top, text.right, text.bottom,
                    rect.left, rect.top, rect.right, rect.bottom,
                    screen.x, screen.y)) return current;
            AccessibilityNodeInfo next = current.getParent();
            current.recycle();
            current = next;
        }
        if (current != null) current.recycle();
        return null;
    }

    private static AccessibilityNodeInfo findVisibleUngroup(
            AccessibilityNodeInfo node, int depth) {
        if (!isXtool(node) || depth > MAX_DEPTH) return null;
        if (node.isVisibleToUser()
                && (XtoolUngroupPolicy.isUngroupLabel(node.getText())
                || XtoolUngroupPolicy.isUngroupLabel(
                        node.getContentDescription()))) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0, n = Math.min(node.getChildCount(), 40); i < n; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                AccessibilityNodeInfo found = findVisibleUngroup(child, depth + 1);
                if (found != null) return found;
            } finally {
                child.recycle();
            }
        }
        return null;
    }

    private static boolean isXtool(AccessibilityNodeInfo node) {
        return node != null && node.getPackageName() != null
                && EasySetForegroundScanPolicy.XCS_PACKAGE
                .contentEquals(node.getPackageName());
    }
}
