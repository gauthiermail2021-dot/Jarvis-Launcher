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
 * Single named gesture to ungroup SVG objects in XCS after handoff.
 * No fixed coordinates, no guessing, no changes to power/speed/type/machine.
 */
final class XtoolUngroupOneShot {
    private static final int MAX_DEPTH = 14;

    interface Outcome {
        void onComplete(boolean touchSent, boolean likelyUngrouped);
    }

    private XtoolUngroupOneShot() { }

    /**
     * Returns true only when the named control was found and processed.
     * If not available yet, caller may retry a bounded read later.
     */
    static boolean tryOnce(AccessibilityService service, Handler handler, Outcome callback) {
        AccessibilityNodeInfo root = null;
        try {
            root = service.getRootInActiveWindow();
            if (!isXtool(root)) return false;
            AccessibilityNodeInfo target = findNamedEnabled(root, 0);
            if (target == null) return false;
            Rect bounds = new Rect();
            try {
                target.getBoundsInScreen(bounds);
            } finally {
                target.recycle();
            }

            WindowManager wm = (WindowManager)
                    service.getSystemService(Context.WINDOW_SERVICE);
            if (wm == null) {
                callback.onComplete(false, false);
                return true;
            }
            Point screen = new Point();
            wm.getDefaultDisplay().getRealSize(screen);
            if (!XtoolUngroupPolicy.validBounds(bounds.left, bounds.top,
                    bounds.right, bounds.bottom, screen.x, screen.y)) {
                callback.onComplete(false, false);
                return true;
            }

            Path path = new Path();
            path.moveTo(bounds.centerX(), bounds.centerY());
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(
                            path, 0, 95L))
                    .build();
            boolean accepted = service.dispatchGesture(gesture,
                    new AccessibilityService.GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription description) {
                            handler.postDelayed(() -> {
                                AccessibilityNodeInfo after = null;
                                AccessibilityNodeInfo stillEnabled = null;
                                try {
                                    after = service.getRootInActiveWindow();
                                    if (!isXtool(after)) {
                                        callback.onComplete(true, false);
                                        return;
                                    }
                                    stillEnabled = findNamedEnabled(after, 0);
                                    callback.onComplete(true, stillEnabled == null);
                                } catch (RuntimeException ignored) {
                                    callback.onComplete(true, false);
                                } finally {
                                    if (stillEnabled != null) stillEnabled.recycle();
                                    if (after != null) after.recycle();
                                }
                            }, 850L);
                        }
                        @Override
                        public void onCancelled(GestureDescription description) {
                            callback.onComplete(false, false);
                        }
                    }, handler);
            if (!accepted) callback.onComplete(false, false);
            return true;
        } catch (RuntimeException ignored) {
            callback.onComplete(false, false);
            return true;
        } finally {
            if (root != null) root.recycle();
        }
    }

    private static AccessibilityNodeInfo findNamedEnabled(
            AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > MAX_DEPTH || !isXtool(node)) return null;
        if (node.isVisibleToUser() && node.isEnabled()
                && (XtoolUngroupPolicy.isUngroupLabel(node.getText())
                || XtoolUngroupPolicy.isUngroupLabel(node.getContentDescription()))) {
            return AccessibilityNodeInfo.obtain(node);
        }
        int count = Math.min(node.getChildCount(), 40);
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                AccessibilityNodeInfo target = findNamedEnabled(child, depth + 1);
                if (target != null) return target;
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
