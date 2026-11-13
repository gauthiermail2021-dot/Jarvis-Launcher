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
 * Touch only the precisely named "Conception" tab in XCS. One gesture maximum
 * for each SVG import. No fixed coordinates and no laser control.
 */
final class XtoolConceptionTabOneShot {
    interface Outcome { void onComplete(boolean touchDelivered); }
    private XtoolConceptionTabOneShot() { }

    static boolean tryOnce(AccessibilityService svc, Handler handler,
                           Outcome callback) {
        AccessibilityNodeInfo root = null;
        AccessibilityNodeInfo target = null;
        try {
            root = svc.getRootInActiveWindow();
            if (!isXtool(root)) return false;
            target = findTab(root, 0);
            if (target == null) return false;
            Rect rect = new Rect();
            target.getBoundsInScreen(rect);

            WindowManager wm = (WindowManager)
                    svc.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                callback.onComplete(false);
                return true;
            }
            Point point = new Point();
            wm.getDefaultDisplay().getRealSize(point);
            if (!XtoolConceptionTabPolicy.validBounds(
                    rect.left, rect.top, rect.right, rect.bottom,
                    point.x, point.y)) {
                callback.onComplete(false);
                return true;
            }

            Path gesturePath = new Path();
            gesturePath.moveTo(rect.centerX(), rect.centerY());
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(
                            gesturePath, 0L, 95L)).build();
            boolean accepted = svc.dispatchGesture(gesture,
                    new AccessibilityService.GestureResultCallback() {
                        @Override public void onCompleted(GestureDescription desc) {
                            // Delivered does not prove the tab is selected.
                            callback.onComplete(true);
                        }
                        @Override public void onCancelled(GestureDescription desc) {
                            callback.onComplete(false);
                        }
                    }, handler);
            if (!accepted) callback.onComplete(false);
            return true;
        } catch (RuntimeException error) {
            callback.onComplete(false);
            return true;
        } finally {
            if (target != null) target.recycle();
            if (root != null) root.recycle();
        }
    }

    static String inspect(AccessibilityNodeInfo root) {
        if (!isXtool(root)) return "fenêtre XCS absente";
        AccessibilityNodeInfo tab = findTab(root, 0);
        if (tab == null) return "onglet non exposé";
        try {
            Rect rect = new Rect();
            tab.getBoundsInScreen(rect);
            return "visible=" + (tab.isVisibleToUser() ? "oui" : "non")
                    + ", actif=" + (tab.isEnabled() ? "oui" : "non")
                    + ", sélection=" + (tab.isSelected() ? "oui" : "non")
                    + ", rectangle=[" + rect.left + "," + rect.top
                    + "–" + rect.right + "," + rect.bottom + "]";
        } finally {
            tab.recycle();
        }
    }

    private static AccessibilityNodeInfo findTab(AccessibilityNodeInfo node,
                                                  int depth) {
        if (!isXtool(node) || depth > 14) return null;
        if (node.isVisibleToUser() && node.isEnabled()
                && (XtoolConceptionTabPolicy.isConception(node.getText())
                || XtoolConceptionTabPolicy.isConception(
                        node.getContentDescription()))) {
            return AccessibilityNodeInfo.obtain(node);
        }
        int limit = Math.min(node.getChildCount(), 40);
        for (int i = 0; i < limit; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                AccessibilityNodeInfo result = findTab(child, depth + 1);
                if (result != null) return result;
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
