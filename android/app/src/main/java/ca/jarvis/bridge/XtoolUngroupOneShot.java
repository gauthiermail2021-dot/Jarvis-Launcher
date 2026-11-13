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
 * One XCS "Dégrouper" attempt after "Conception".
 * The label can be a non-clickable TextView BELOW the real action icon:
 * prefer the accessible clickable action container over tapping the text.
 * Never use fixed screen coordinates or laser controls.
 */
final class XtoolUngroupOneShot {
    private static final int MAX_DEPTH = 14;

    interface Outcome {
        void onComplete(boolean touchSent, boolean verifiedUngrouped);
    }

    private XtoolUngroupOneShot() { }

    /** false = UI not ready; true = one named action attempted or failed. */
    static boolean tryOnce(AccessibilityService service, Handler handler, Outcome callback) {
        AccessibilityNodeInfo root = null;
        AccessibilityNodeInfo label = null;
        AccessibilityNodeInfo target = null;
        try {
            root = service.getRootInActiveWindow();
            if (!isXtool(root)) return false;
            label = findNamedEnabled(root, 0);
            if (label == null) return false;

            WindowManager wm = (WindowManager)
                    service.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                callback.onComplete(false, false);
                return true;
            }
            Point size = new Point();
            wm.getDefaultDisplay().getRealSize(size);
            Rect labelBounds = new Rect();
            label.getBoundsInScreen(labelBounds);
            if (!XtoolUngroupPolicy.validBounds(labelBounds.left, labelBounds.top,
                    labelBounds.right, labelBounds.bottom, size.x, size.y)) {
                callback.onComplete(false, false);
                return true;
            }

            // Accessible ACTION_CLICK on an actual labelled control/ancestor is
            // preferable to a gesture on its text child (as in the screenshot).
            target = closestClickableControl(label, size);
            if (target != null) {
                if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    callback.onComplete(true, false);
                    return true;
                }
                // Some XCS views advertise CLICK but reject the action.
                // In that case, gesture once at that SAME named container.
                Rect bounds = new Rect();
                target.getBoundsInScreen(bounds);
                dispatchSingleGesture(service, handler, bounds, callback);
                return true;
            }

            // If XCS exposes icon+label as a compact, non-clickable container,
            // tap its centre (the icon area), not the label below.
            target = compactIconContainer(label, labelBounds, size);
            if (target != null) {
                Rect bounds = new Rect();
                target.getBoundsInScreen(bounds);
                dispatchSingleGesture(service, handler, bounds, callback);
                return true;
            }

            // No trustworthy action container: fail visibly rather than
            // claiming success after tapping a text-only label.
            callback.onComplete(false, false);
            return true;
        } catch (RuntimeException error) {
            callback.onComplete(false, false);
            return true;
        } finally {
            if (target != null) target.recycle();
            if (label != null) label.recycle();
            if (root != null) root.recycle();
        }
    }

    private static void dispatchSingleGesture(AccessibilityService service,
                                              Handler handler, Rect bounds,
                                              Outcome callback) {
        Path p = new Path();
        p.moveTo(bounds.centerX(), bounds.centerY());
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, 95L))
                .build();
        boolean accepted = service.dispatchGesture(gesture,
                new AccessibilityService.GestureResultCallback() {
                    @Override public void onCompleted(GestureDescription desc) {
                        // Gesture completion is NOT proof of successful ungroup.
                        callback.onComplete(true, false);
                    }
                    @Override public void onCancelled(GestureDescription desc) {
                        callback.onComplete(false, false);
                    }
                }, handler);
        if (!accepted) callback.onComplete(false, false);
    }

    private static AccessibilityNodeInfo closestClickableControl(
            AccessibilityNodeInfo label, Point size) {
        AccessibilityNodeInfo current = AccessibilityNodeInfo.obtain(label);
        for (int level = 0; current != null && level <= 4; level++) {
            if (!isXtool(current) || !current.isVisibleToUser()
                    || !current.isEnabled()) break;
            Rect r = new Rect();
            current.getBoundsInScreen(r);
            if (current.isClickable() && XtoolUngroupPolicy.validClickTargetBounds(
                    r.left, r.top, r.right, r.bottom, size.x, size.y))
                return current;
            AccessibilityNodeInfo next = current.getParent();
            current.recycle();
            current = next;
        }
        if (current != null) current.recycle();
        return null;
    }

    private static AccessibilityNodeInfo compactIconContainer(
            AccessibilityNodeInfo label, Rect text, Point size) {
        AccessibilityNodeInfo current = label.getParent();
        for (int level = 0; current != null && level < 3; level++) {
            if (!isXtool(current) || !current.isVisibleToUser()
                    || !current.isEnabled()) break;
            Rect container = new Rect();
            current.getBoundsInScreen(container);
            if (XtoolUngroupPolicy.isCompactIconContainer(
                    text.left, text.top, text.right, text.bottom,
                    container.left, container.top, container.right,
                    container.bottom, size.x, size.y))
                return current;
            AccessibilityNodeInfo next = current.getParent();
            current.recycle();
            current = next;
        }
        if (current != null) current.recycle();
        return null;
    }

    private static AccessibilityNodeInfo findNamedEnabled(
            AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > MAX_DEPTH || !isXtool(node)) return null;
        if (node.isVisibleToUser() && node.isEnabled()
                && (XtoolUngroupPolicy.isUngroupLabel(node.getText())
                || XtoolUngroupPolicy.isUngroupLabel(
                        node.getContentDescription()))) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0, n = Math.min(node.getChildCount(), 40); i < n; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                AccessibilityNodeInfo result = findNamedEnabled(child, depth + 1);
                if (result != null) return result;
            } finally { child.recycle(); }
        }
        return null;
    }

    private static boolean isXtool(AccessibilityNodeInfo node) {
        return node != null && node.getPackageName() != null
                && EasySetForegroundScanPolicy.XCS_PACKAGE
                    .contentEquals(node.getPackageName());
    }
}
