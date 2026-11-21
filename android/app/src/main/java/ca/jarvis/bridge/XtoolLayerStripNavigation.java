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
 * One finite, guarded bottom-toolbar operation in xTool. The recorded
 * Samsung flow shows the Calques item partly clipped on the right before
 * the horizontal bar is swiped left. This class uses the CURRENT exact
 * accessible label as its anchor, never a stored pixel location.
 *
 * Maximum two swipes and one named tap, once per invocation. No deselection,
 * settings changes or laser actions. A delivered gesture is not UI proof.
 */
final class XtoolLayerStripNavigation {
    interface Result { void finished(String report); }

    private static final int MAX_DEPTH = 18;
    private static final int MAX_CHILDREN = 50;
    private static final int MAX_SWIPES = 2;
    private static final long SWIPE_DURATION_MS = 390L;

    private XtoolLayerStripNavigation() { }

    static boolean ready(AccessibilityService service) {
        AccessibilityNodeInfo root = null;
        try {
            if (!XtoolSessionManager.isActive(service)
                    || XtoolLayersOneShot.ungroupPopupVisible(service)) return false;
            root = service.getRootInActiveWindow();
            if (!isXtool(root)) return false;
            Point screen = screen(service);
            if (screen == null) return false;
            // The toolbar in the user video only becomes active after
            // the Conception contextual sheet has disappeared.
            if (conceptionSheetVisible(root, screen, 0)) return false;
            Matches matches = new Matches();
            collect(root, screen, 0, matches);
            if (matches.count == 0) return matches.anchor != null;
            if (matches.count != 1 || matches.area == null) return false;
            Rect r = matches.area;
            return XtoolLayerStripPolicy.clippedAtRight(
                    r.left, r.top, r.right, r.bottom, screen.x, screen.y)
                    || XtoolLayerStripPolicy.fullyRevealed(
                    r.left, r.top, r.right, r.bottom, screen.x, screen.y);
        } catch (RuntimeException ignored) {
            return false;
        } finally {
            if (root != null) root.recycle();
        }
    }

    static void revealAndOpen(AccessibilityService service, Handler handler,
                              Result callback) {
        phase(service, handler, callback, 0);
    }

    private static void phase(AccessibilityService service, Handler handler,
                              Result callback, int swipes) {
        AccessibilityNodeInfo root = null;
        try {
            if (!XtoolSessionManager.isActive(service)) {
                callback.finished("Session xTool terminée. Aucun geste.");
                return;
            }
            if (XtoolLayersOneShot.ungroupPopupVisible(service)) {
                callback.finished("Menu Dégrouper encore visible. Aucun geste.");
                return;
            }
            root = service.getRootInActiveWindow();
            if (!isXtool(root)) {
                callback.finished("Fenêtre xTool absente. Aucun geste.");
                return;
            }
            Point screen = screen(service);
            if (screen == null) {
                callback.finished("Dimensions Android indisponibles.");
                return;
            }
            if (conceptionSheetVisible(root, screen, 0)) {
                callback.finished("Volet Conception encore visible : "
                        + "désélectionner le dessin avant de défiler.");
                return;
            }
            Matches matches = new Matches();
            collect(root, screen, 0, matches);
            if (matches.count > 1 || (matches.count == 1 && matches.area == null)) {
                callback.finished("Cible Calque ambiguë ou hors écran. Aucun geste.");
                return;
            }
            Rect r = matches.area;
            if (r != null && XtoolLayerStripPolicy.fullyRevealed(
                    r.left, r.top, r.right, r.bottom, screen.x, screen.y)) {
                final int completedSwipes = swipes;
                XtoolLayersClickProbe.tapNamedOnce(service, handler, report ->
                        callback.finished("Défilements livrés : " + completedSwipes
                                + "\n" + report
                                + "\nOuverture à vérifier visuellement."));
                return;
            }
            if (r != null && !XtoolLayerStripPolicy.clippedAtRight(
                    r.left, r.top, r.right, r.bottom, screen.x, screen.y)) {
                callback.finished("Calque visible mais emplacement non validé. "
                        + "Aucun autre geste.");
                return;
            }
            if (r == null) r = matches.anchor;
            if (r == null) {
                callback.finished("Calque invisible et aucun outil nommé de "
                        + "la barre inférieure repéré. Aucun geste.");
                return;
            }
            if (swipes >= MAX_SWIPES) {
                callback.finished("Calque pas entièrement visible après deux "
                        + "glissements. Aucun toucher.");
                return;
            }

            int startX = screen.x * 79 / 100;
            int endX = screen.x * 24 / 100;
            int y = r.centerY();
            if (!XtoolLayerStripPolicy.safeSwipe(
                    startX, endX, y, screen.x, screen.y, r.top, r.bottom)) {
                callback.finished("Bande inférieure non validée. Aucun glissement.");
                return;
            }
            Path path = new Path();
            path.moveTo(startX, y);
            path.lineTo(endX, y);
            GestureDescription swipe = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(
                            path, 0L, SWIPE_DURATION_MS))
                    .build();
            boolean dispatched = service.dispatchGesture(swipe,
                    new AccessibilityService.GestureResultCallback() {
                        @Override public void onCompleted(GestureDescription unused) {
                            // Re-read the live label after the content finishes moving.
                            handler.postDelayed(() ->
                                    phase(service, handler, callback, swipes + 1), 650L);
                        }
                        @Override public void onCancelled(GestureDescription unused) {
                            callback.finished("Glissement annulé par Android. "
                                    + "Aucun toucher supplémentaire.");
                        }
                    }, handler);
            if (!dispatched) {
                callback.finished("Glissement refusé par Android. Aucun toucher.");
            }
        } catch (RuntimeException error) {
            callback.finished("Navigation Calques interrompue : "
                    + error.getClass().getSimpleName() + ". Aucun geste de secours.");
        } finally {
            if (root != null) root.recycle();
        }
    }

    private static boolean conceptionSheetVisible(AccessibilityNodeInfo node,
                                                    Point screen, int depth) {
        if (!isXtool(node) || depth > MAX_DEPTH) return false;
        Rect b = new Rect();
        node.getBoundsInScreen(b);
        boolean tab = XtoolConceptionTabPolicy.isConception(node.getText())
                || XtoolConceptionTabPolicy.isConception(
                        node.getContentDescription());
        if (tab && node.isVisibleToUser() && b.centerY() > screen.y * 55L / 100
                && b.centerY() < screen.y * 96L / 100) return true;
        for (int i = 0, n = Math.min(MAX_CHILDREN, node.getChildCount());
                i < n; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                if (conceptionSheetVisible(child, screen, depth + 1)) return true;
            } finally {
                child.recycle();
            }
        }
        return false;
    }

    private static void collect(AccessibilityNodeInfo node, Point screen,
                                int depth, Matches matches) {
        if (!isXtool(node) || depth > MAX_DEPTH || matches.visited++ > 250) return;
        if (XtoolLayersPolicy.exactLayersLabel(node.getText())
                || XtoolLayersPolicy.exactLayersLabel(node.getContentDescription())) {
            matches.count++;
            Rect bounds = new Rect();
            node.getBoundsInScreen(bounds);
            if (node.isVisibleToUser() && node.isEnabled()
                    && XtoolLayersPolicy.validNamedIconTile(
                    bounds.left, bounds.top, bounds.right, bounds.bottom,
                    screen.x, screen.y)) {
                matches.area = new Rect(bounds);
            }
        }
        if (toolbarAnchor(node.getText()) || toolbarAnchor(
                node.getContentDescription())) {
            Rect anchor = new Rect();
            node.getBoundsInScreen(anchor);
            if (node.isVisibleToUser() && node.isEnabled()
                    && XtoolLayerStripPolicy.validToolbarAnchor(
                    anchor.left, anchor.top, anchor.right, anchor.bottom,
                    screen.x, screen.y)) {
                // Prefer the central item, keeping the swipe fully within the strip.
                if (matches.anchor == null
                        || Math.abs(anchor.centerX() - screen.x / 2)
                        < Math.abs(matches.anchor.centerX() - screen.x / 2)) {
                    matches.anchor = new Rect(anchor);
                }
            }
        }
        int count = Math.min(MAX_CHILDREN, node.getChildCount());
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                collect(child, screen, depth + 1, matches);
            } finally {
                child.recycle();
            }
        }
    }

    private static boolean toolbarAnchor(CharSequence label) {
        if (label == null) return false;
        String word = label.toString().trim().toLowerCase(
                java.util.Locale.ROOT);
        return "forme".equals(word) || "graffiti".equals(word)
                || "toile".equals(word) || "image".equals(word)
                || "texte".equals(word);
    }

    private static Point screen(AccessibilityService service) {
        WindowManager wm = (WindowManager) service.getSystemService(
                Context.WINDOW_SERVICE);
        if (wm == null) return null;
        Point point = new Point();
        wm.getDefaultDisplay().getRealSize(point);
        return point.x > 0 && point.y > 0 ? point : null;
    }

    private static boolean isXtool(AccessibilityNodeInfo node) {
        return node != null && node.getPackageName() != null
                && EasySetForegroundScanPolicy.XCS_PACKAGE.contentEquals(
                        node.getPackageName());
    }

    private static final class Matches {
        int count;
        int visited;
        Rect area;
        Rect anchor;
    }
}
