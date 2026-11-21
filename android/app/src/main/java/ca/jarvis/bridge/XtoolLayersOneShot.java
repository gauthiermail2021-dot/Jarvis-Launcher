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
 * Third one-shot drawing-editor step: open the named Calques bottom button
 * after Dégrouper. No global Back, no guessed screen corners, no laser actions.
 */
final class XtoolLayersOneShot {
    interface Outcome { void finished(boolean touchDelivered); }

    private static volatile String lastAttempt =
            "Aucune ouverture de Calques tentée depuis le démarrage.";

    private XtoolLayersOneShot() { }

    static String lastAttempt() { return lastAttempt; }

    /**
     * Returns false if the named and bounded button is not presently exposed.
     * Returns true if a single attempt was made (callback tells delivered).
     */
    static boolean tryOnce(AccessibilityService service, Handler handler,
                           Outcome callback) {
        AccessibilityNodeInfo root = null, label = null, target = null;
        try {
            root = service.getRootInActiveWindow();
            if (!isXtool(root)) return false;
            Point screen = screen(service);
            if (screen == null) return false;

            label = findBottomLayers(root, screen, 0);
            if (label == null) return false;
            Rect area = new Rect();
            label.getBoundsInScreen(area);
            if (!label.isEnabled()) {
                lastAttempt = "Calques visible mais désactivé; aucun toucher.";
                callback.finished(false);
                return true;
            }
            target = closestClickableControl(label, screen);
            if (target != null) {
                if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    Rect mark = new Rect();
                    target.getBoundsInScreen(mark);
                    JarvisBubbleService.showXtoolTouchMarker(
                            mark.centerX(), mark.centerY());
                    lastAttempt = "ACTION_CLICK acceptée sur Calques.";
                    callback.finished(true);
                    return true;
                }
                Rect touch = new Rect();
                target.getBoundsInScreen(touch);
                tap(service, handler, touch, callback);
                return true;
            }

            // Like xTool's Dégrouper button, a named icon+text tile may
            // be non-clickable in the accessibility tree.
            if (XtoolLayersPolicy.validNamedIconTile(
                    area.left, area.top, area.right, area.bottom,
                    screen.x, screen.y)) {
                tap(service, handler, area, callback);
                return true;
            }
            lastAttempt = "Libellé Calques trouvé, mais aucun contrôle "
                    + "cliquable/tuile identifié. Aucun toucher.";
            callback.finished(false);
            return true;
        } catch (RuntimeException e) {
            lastAttempt = "Erreur de ciblage Calques : "
                    + e.getClass().getSimpleName();
            callback.finished(false);
            return true;
        } finally {
            if (target != null) target.recycle();
            if (label != null) label.recycle();
            if (root != null) root.recycle();
        }
    }

    /**
     * If xTool explicitly exposes the Dégrouper contextual menu as
     * dismissable, close only that matching popup. No Android global Back:
     * a global Back might instead leave the drawing.
     */
    static boolean dismissUngroupPopupIfExposed(AccessibilityService service) {
        AccessibilityNodeInfo root = null, ungroup = null;
        try {
            root = service.getRootInActiveWindow();
            if (!isXtool(root)) return false;
            Point screen = screen(service);
            if (screen == null) return false;
            ungroup = findUngroup(root, 0);
            if (ungroup == null) return false;
            AccessibilityNodeInfo parent = ungroup.getParent();
            for (int depth = 0; parent != null && depth < 5; depth++) {
                if (!isXtool(parent)) {
                    parent.recycle();
                    return false;
                }
                Rect box = new Rect();
                parent.getBoundsInScreen(box);
                if (parent.isDismissable() && parent.isVisibleToUser()
                        && XtoolLayersPolicy.validMenuToDismiss(
                                box.left, box.top, box.right, box.bottom,
                                screen.x, screen.y)) {
                    boolean accepted = parent.performAction(
                            AccessibilityNodeInfo.ACTION_DISMISS);
                    parent.recycle();
                    if (accepted) {
                        lastAttempt = "Menu Dégrouper fermé par ACTION_DISMISS.";
                        return true;
                    }
                    return false;
                }
                AccessibilityNodeInfo next = parent.getParent();
                parent.recycle();
                parent = next;
            }
            return false;
        } catch (RuntimeException ignored) {
            return false;
        } finally {
            if (ungroup != null) ungroup.recycle();
            if (root != null) root.recycle();
        }
    }

    /**
     * A popup can only be closed through its explicitly exposed Android
     * ACTION_DISMISS. Never guess a blank canvas corner: on the Samsung
     * the previous heuristic activated "Réglages" instead of deselecting.
     */
    static boolean dismissUngroupMenu(AccessibilityService service,
                                      Handler handler, Outcome callback) {
        if (!dismissUngroupPopupIfExposed(service)) return false;
        handler.post(() -> callback.finished(true));
        return true;
    }

    /** Report whether xTool still exposes a visible Dégrouper menu. */
    static boolean ungroupPopupVisible(AccessibilityService service) {
        AccessibilityNodeInfo root = null;
        AccessibilityNodeInfo ungroup = null;
        try {
            root = service.getRootInActiveWindow();
            if (!isXtool(root)) return false;
            ungroup = findUngroup(root, 0);
            return ungroup != null && ungroup.isVisibleToUser();
        } finally {
            if (ungroup != null) ungroup.recycle();
            if (root != null) root.recycle();
        }
    }


    /**
     * One bounded tap inside the bottom-right of the xTool drawing area.
     * The actual Conception tab and xTool window bounds determine the position.
     * No top-right screen corner, no settings controls, and no machine action.
     * A completed gesture is NOT proof that the shapes are deselected.
     */
    static boolean deselectAtDrawingBottomRight(AccessibilityService service,
                                               Handler handler, Outcome callback) {
        AccessibilityNodeInfo root = null, tab = null, layers = null;
        try {
            root = service.getRootInActiveWindow();
            if (!isXtool(root)) return false;
            Point display = screen(service);
            if (display == null) return false;
            tab = findSelectedConception(root, 0);
            layers = findBottomLayers(root, display, 0);
            if (tab == null || layers == null) {
                lastAttempt = "Désélection : onglet Conception ou Calques introuvable.";
                return false;
            }
            Rect rootArea = new Rect();
            Rect tabArea = new Rect();
            root.getBoundsInScreen(rootArea);
            tab.getBoundsInScreen(tabArea);
            int drawingBottom = tabArea.top;
            int x = rootArea.left + rootArea.width() * 89 / 100;
            int y = rootArea.top + (drawingBottom - rootArea.top) * 84 / 100;
            if (!XtoolLayersPolicy.validDrawingBottomRightTap(
                    x, y, rootArea.left, rootArea.top, rootArea.right,
                    drawingBottom, display.x, display.y)) {
                lastAttempt = "Désélection : coin droit hors de la page de dessin.";
                return false;
            }
            if (coveredByAccessibleControl(root, x, y,
                    display.x, display.y, 0)) {
                lastAttempt = "Désélection : bouton ou libellé au coin droit; arrêt.";
                return false;
            }

            Path path = new Path();
            path.moveTo(x, y);
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(
                            path, 0L, 95L)).build();
            boolean accepted = service.dispatchGesture(gesture,
                    new AccessibilityService.GestureResultCallback() {
                        @Override public void onCompleted(GestureDescription description) {
                            lastAttempt = "Coin inférieur droit du dessin touché; "
                                    + "désélection visuelle à confirmer.";
                            callback.finished(true);
                        }
                        @Override public void onCancelled(GestureDescription description) {
                            lastAttempt = "Désélection : geste annulé par Android.";
                            callback.finished(false);
                        }
                    }, handler);
            if (accepted) {
                JarvisBubbleService.showXtoolTouchMarker(x, y);
            } else {
                lastAttempt = "Désélection : Android a refusé le geste.";
                handler.post(() -> callback.finished(false));
            }
            return true;
        } catch (RuntimeException e) {
            lastAttempt = "Désélection : " + e.getClass().getSimpleName();
            return false;
        } finally {
            if (layers != null) layers.recycle();
            if (tab != null) tab.recycle();
            if (root != null) root.recycle();
        }
    }

    private static boolean coveredByAccessibleControl(AccessibilityNodeInfo node,
                                                       int x, int y, int width,
                                                       int height, int depth) {
        if (!isXtool(node) || depth > 14) return false;
        Rect box = new Rect();
        node.getBoundsInScreen(box);
        if (node.isVisibleToUser() && box.contains(x, y)
                && box.width() > 0 && box.height() > 0
                && box.width() < width * 65L / 100
                && box.height() < height * 45L / 100
                && (node.isClickable() || node.isLongClickable()
                    || hasText(node.getText())
                    || hasText(node.getContentDescription()))) {
            return true;
        }
        int n = Math.min(40, node.getChildCount());
        for (int i = 0; i < n; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                if (coveredByAccessibleControl(child, x, y,
                        width, height, depth + 1)) return true;
            } finally {
                child.recycle();
            }
        }
        return false;
    }

    private static boolean hasText(CharSequence value) {
        return value != null && value.length() > 0;
    }

    private static AccessibilityNodeInfo findSelectedConception(
            AccessibilityNodeInfo node, int depth) {
        if (!isXtool(node) || depth > 14) return null;
        if (node.isVisibleToUser() && node.isSelected()
                && (XtoolConceptionTabPolicy.isConception(node.getText())
                || XtoolConceptionTabPolicy.isConception(
                        node.getContentDescription()))) {
            return AccessibilityNodeInfo.obtain(node);
        }
        int n = Math.min(40, node.getChildCount());
        for (int i = 0; i < n; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                AccessibilityNodeInfo match = findSelectedConception(child, depth + 1);
                if (match != null) return match;
            } finally {
                child.recycle();
            }
        }
        return null;
    }

    private static void tap(AccessibilityService service, Handler handler,
                            Rect box, Outcome callback) {
        Path path = new Path();
        path.moveTo(box.centerX(), box.centerY());
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0L, 95L))
                .build();
        boolean accepted = service.dispatchGesture(gesture,
                new AccessibilityService.GestureResultCallback() {
                    @Override public void onCompleted(GestureDescription d) {
                        lastAttempt = "Toucher Calques envoyé; ouverture à vérifier.";
                        callback.finished(true);
                    }
                    @Override public void onCancelled(GestureDescription d) {
                        lastAttempt = "Toucher Calques annulé par Android.";
                        callback.finished(false);
                    }
                }, handler);
        if (accepted) {
            JarvisBubbleService.showXtoolTouchMarker(
                    box.centerX(), box.centerY());
        } else {
            lastAttempt = "Toucher Calques refusé par Android.";
            callback.finished(false);
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
            if (current.isClickable() && XtoolLayersPolicy.validBottomButton(
                    rect.left, rect.top, rect.right, rect.bottom,
                    screen.x, screen.y)) return current;
            AccessibilityNodeInfo next = current.getParent();
            current.recycle();
            current = next;
        }
        if (current != null) current.recycle();
        return null;
    }

    private static AccessibilityNodeInfo findBottomLayers(
            AccessibilityNodeInfo node, Point screen, int depth) {
        if (!isXtool(node) || depth > 14) return null;
        Rect box = new Rect();
        node.getBoundsInScreen(box);
        if (node.isVisibleToUser() && node.isEnabled()
                && (XtoolLayersPolicy.exactLayersLabel(node.getText())
                    || XtoolLayersPolicy.exactLayersLabel(node.getContentDescription()))
                && XtoolLayersPolicy.validBottomButton(
                        box.left, box.top, box.right, box.bottom,
                        screen.x, screen.y)) {
            return AccessibilityNodeInfo.obtain(node);
        }
        int n = Math.min(40, node.getChildCount());
        for (int i = 0; i < n; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                AccessibilityNodeInfo match = findBottomLayers(child, screen, depth + 1);
                if (match != null) return match;
            } finally {
                child.recycle();
            }
        }
        return null;
    }

    private static AccessibilityNodeInfo findUngroup(AccessibilityNodeInfo node, int depth) {
        if (!isXtool(node) || depth > 14) return null;
        if (node.isVisibleToUser()
                && (XtoolUngroupPolicy.isUngroupLabel(node.getText())
                    || XtoolUngroupPolicy.isUngroupLabel(
                            node.getContentDescription()))) {
            return AccessibilityNodeInfo.obtain(node);
        }
        int n = Math.min(40, node.getChildCount());
        for (int i = 0; i < n; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                AccessibilityNodeInfo match = findUngroup(child, depth + 1);
                if (match != null) return match;
            } finally {
                child.recycle();
            }
        }
        return null;
    }

    private static Point screen(AccessibilityService service) {
        WindowManager manager = (WindowManager)
                service.getSystemService(Context.WINDOW_SERVICE);
        if (manager == null) return null;
        Point point = new Point();
        manager.getDefaultDisplay().getRealSize(point);
        return point.x > 0 && point.y > 0 ? point : null;
    }

    private static boolean isXtool(AccessibilityNodeInfo node) {
        return node != null && node.getPackageName() != null
                && EasySetForegroundScanPolicy.XCS_PACKAGE
                .contentEquals(node.getPackageName());
    }
}
