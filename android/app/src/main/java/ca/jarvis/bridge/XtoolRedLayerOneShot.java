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

import java.util.ArrayList;
import java.util.List;

/**
 * Select the unique exact "Rouge" layer entry in the opened xTool layer
 * sheet. One accessible ACTION_CLICK OR one bounded named touch, never
 * both. No scroll/retry, EasySet settings or machine actions.
 */
final class XtoolRedLayerOneShot {
    private static final int MAX_DEPTH = 16;
    private static final int MAX_CHILDREN = 40;

    private XtoolRedLayerOneShot() { }

    interface Outcome {
        void complete(boolean touchSent, String description);
    }

    /** Explicit diagnostic menu command: display a report after the test. */
    static void run(AccessibilityService service, Handler handler, Runnable finished) {
        execute(service, handler, (sent, details) -> {
            finished.run();
            JarvisBubbleService.showEasySetResult(
                    "TEST CALQUE ROUGE\n" + details
                    + "\nAucun paramètre de la machine modifié.");
        });
    }

    /** After automatic Calques open: report to Jarvis, no blocking overlay. */
    static void runAutomatically(AccessibilityService service, Handler handler,
                                 Outcome callback) {
        execute(service, handler, callback);
    }

    private static void execute(AccessibilityService service, Handler handler,
                                Outcome callback) {
        AccessibilityNodeInfo root = null;
        List<AccessibilityNodeInfo> matches = new ArrayList<>();
        AccessibilityNodeInfo clickable = null;
        try {
            root = service.getRootInActiveWindow();
            if (!isXtool(root)) {
                callback.complete(false, "xTool n'est pas au premier plan : aucun clic.");
                return;
            }
            collectExactRed(root, 0, matches);
            if (matches.size() != 1) {
                callback.complete(false, matches.isEmpty()
                        ? "Calque « Rouge » non identifié par Android : aucun clic."
                        : "Plusieurs cibles « Rouge » : aucun clic ambigu.");
                return;
            }

            AccessibilityNodeInfo red = matches.get(0);
            Rect redRect = new Rect();
            red.getBoundsInScreen(redRect);
            if (!red.isVisibleToUser() || !red.isEnabled()
                    || !hasLayerSheet(root, redRect.top)) {
                callback.complete(false, "Ligne Rouge ou panneau Calques non confirmé : "
                        + "aucun clic.");
                return;
            }
            WindowManager wm = (WindowManager)
                    service.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                callback.complete(false, "Dimensions Android indisponibles.");
                return;
            }
            Point display = new Point();
            wm.getDefaultDisplay().getRealSize(display);
            if (display.x <= 0 || display.y <= 0) {
                callback.complete(false, "Dimensions Android invalides.");
                return;
            }

            clickable = closestNamedClickable(red, display);
            if (clickable != null) {
                Rect area = new Rect();
                clickable.getBoundsInScreen(area);
                boolean accepted = clickable.performAction(
                        AccessibilityNodeInfo.ACTION_CLICK);
                if (!accepted) {
                    callback.complete(false, "ACTION_CLICK Rouge refusée. "
                            + "Aucun toucher supplémentaire.");
                    return;
                }
                JarvisBubbleService.showXtoolTouchMarker(
                        area.centerX(), area.centerY());
                checkAfter(service, handler, callback, area.centerX(),
                        area.centerY(), "ACTION_CLICK", true);
                return;
            }

            if (!XtoolLayerTapPolicy.isValidTarget(
                    redRect.left, redRect.top, redRect.right, redRect.bottom,
                    display.x, display.y)) {
                callback.complete(false, "Rectangle Rouge invalide ou trop petit : "
                        + "aucun toucher de secours.");
                return;
            }

            int x = redRect.centerX(), y = redRect.centerY();
            Path path = new Path();
            path.moveTo(x, y);
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0L, 95L))
                    .build();
            boolean sent = service.dispatchGesture(gesture,
                    new AccessibilityService.GestureResultCallback() {
                        @Override public void onCompleted(GestureDescription unused) {
                            JarvisBubbleService.showXtoolTouchMarker(x, y);
                            checkAfter(service, handler, callback,
                                    x, y, "toucher du libellé", true);
                        }

                        @Override public void onCancelled(GestureDescription unused) {
                            callback.complete(false, "Toucher Rouge annulé par Android.");
                        }
                    }, handler);
            if (!sent) callback.complete(false, "Android a refusé le toucher Rouge.");
        } catch (RuntimeException error) {
            callback.complete(false, "Activation Rouge interrompue : "
                    + error.getClass().getSimpleName());
        } finally {
            if (clickable != null) clickable.recycle();
            for (AccessibilityNodeInfo item : matches) item.recycle();
            if (root != null) root.recycle();
        }
    }

    private static void checkAfter(AccessibilityService service, Handler handler,
                                   Outcome callback, int x, int y,
                                   String action, boolean sent) {
        handler.postDelayed(() -> {
            AccessibilityNodeInfo root = null;
            List<AccessibilityNodeInfo> matches = new ArrayList<>();
            try {
                root = service.getRootInActiveWindow();
                if (!isXtool(root)) {
                    callback.complete(sent, action + " livré à (" + x + "," + y
                            + "), mais xTool n'est plus au premier plan.");
                    return;
                }
                collectExactRed(root, 0, matches);
                boolean selected = matches.size() == 1 && matches.get(0).isSelected();
                callback.complete(sent, action + " livré au calque Rouge ("
                        + x + "," + y + "). "
                        + (selected ? "Sélection signalée par Android."
                        : "Sélection à confirmer visuellement dans xTool."));
            } catch (RuntimeException error) {
                callback.complete(sent, action + " livré, mais lecture de suivi "
                        + "indisponible.");
            } finally {
                for (AccessibilityNodeInfo item : matches) item.recycle();
                if (root != null) root.recycle();
            }
        }, 650L);
    }

    private static AccessibilityNodeInfo closestNamedClickable(
            AccessibilityNodeInfo red, Point screen) {
        AccessibilityNodeInfo current = AccessibilityNodeInfo.obtain(red);
        for (int level = 0; current != null && level < 4; level++) {
            Rect area = new Rect();
            current.getBoundsInScreen(area);
            if (!isXtool(current) || !current.isVisibleToUser()
                    || !current.isEnabled()) break;
            if (current.isClickable() && XtoolLayerTapPolicy.isValidTarget(
                    area.left, area.top, area.right, area.bottom,
                    screen.x, screen.y)) return current;
            AccessibilityNodeInfo parent = current.getParent();
            current.recycle();
            current = parent;
        }
        if (current != null) current.recycle();
        return null;
    }

    private static void collectExactRed(AccessibilityNodeInfo node, int depth,
                                        List<AccessibilityNodeInfo> matches) {
        if (!isXtool(node) || depth > MAX_DEPTH || matches.size() > 1) return;
        if (node.isVisibleToUser() && node.isEnabled()
                && (XtoolLayerTapPolicy.exactRed(safe(node.getText()))
                    || XtoolLayerTapPolicy.exactRed(
                            safe(node.getContentDescription())))) {
            matches.add(AccessibilityNodeInfo.obtain(node));
        }
        if (depth >= MAX_DEPTH) return;
        int n = Math.min(MAX_CHILDREN, node.getChildCount());
        for (int i = 0; i < n && matches.size() <= 1; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                collectExactRed(child, depth + 1, matches);
            } finally {
                child.recycle();
            }
        }
    }

    private static boolean hasLayerSheet(AccessibilityNodeInfo root, int redTop) {
        return findLayerHeadingAbove(root, redTop, 0);
    }

    private static boolean findLayerHeadingAbove(AccessibilityNodeInfo node,
                                                  int redTop, int depth) {
        if (!isXtool(node) || depth > MAX_DEPTH) return false;
        if (node.isVisibleToUser()
                && (XtoolLayerTapPolicy.exactLayerHeading(safe(node.getText()))
                    || XtoolLayerTapPolicy.exactLayerHeading(
                            safe(node.getContentDescription())))) {
            Rect heading = new Rect();
            node.getBoundsInScreen(heading);
            if (heading.bottom > 0 && heading.bottom < redTop) return true;
        }
        int n = Math.min(MAX_CHILDREN, node.getChildCount());
        for (int i = 0; i < n; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                if (findLayerHeadingAbove(child, redTop, depth + 1)) return true;
            } finally {
                child.recycle();
            }
        }
        return false;
    }

    private static String safe(CharSequence input) {
        return input == null ? "" : input.toString();
    }

    private static boolean isXtool(AccessibilityNodeInfo node) {
        return node != null && node.getPackageName() != null
                && EasySetForegroundScanPolicy.XCS_PACKAGE
                        .contentEquals(node.getPackageName());
    }
}
