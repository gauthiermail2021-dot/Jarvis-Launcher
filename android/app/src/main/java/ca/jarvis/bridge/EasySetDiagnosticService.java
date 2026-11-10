package ca.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

/**
 * Experimental, user-triggered, READ ONLY diagnostic.
 * Never clicks, types, performs gestures or records entered values.
 * Only detects the presence of EasySet headings for 120 seconds after arming.
 */
public final class EasySetDiagnosticService extends AccessibilityService {
    static final String PREFS = "jarvis_easyset_diagnostic";
    static final String KEY_ARM_UNTIL = "arm_until_elapsed";
    static final String KEY_RESULT = "result";
    private long lastInspection;

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        long now = SystemClock.elapsedRealtime();
        long until = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_ARM_UNTIL, 0L);
        if (until <= now || now - lastInspection < 500L) return;
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        String pkg = event.getPackageName().toString().toLowerCase(Locale.ROOT);
        // Only inspect the intended xTool application, and nothing else.
        if (!pkg.contains("xtool") && !pkg.contains("makeblock")) return;
        lastInspection = now;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            save("xTool détecté, mais contenu inaccessible à l'accessibilité.");
            return;
        }
        try {
            Flags flags = new Flags();
            inspect(root, flags, 0);
            if (flags.easyset) {
                String result = "Volet EasySet détecté. Puissance : "
                        + (flags.power ? "visible" : "non détectée")
                        + " ; vitesse : " + (flags.speed ? "visible" : "non détectée")
                        + " ; passages : " + (flags.passes ? "visible" : "non détectés")
                        + ". Lecture seule : aucune valeur modifiée.";
                save(result);
            } else {
                save("xTool détecté, volet EasySet non visible dans l'arbre Android.");
            }
        } finally {
            root.recycle();
        }
    }

    static final class Flags {
        boolean easyset, power, speed, passes;
    }

    private void inspect(AccessibilityNodeInfo node, Flags flags, int depth) {
        if (node == null || depth > 20) return;
        CharSequence content = node.getText();
        CharSequence description = node.getContentDescription();
        String normalized = ((content == null ? "" : content.toString()) + " "
                + (description == null ? "" : description.toString()))
                .toLowerCase(Locale.ROOT);
        if (normalized.contains("easyset")) flags.easyset = true;
        if (normalized.contains("puissance") || normalized.contains("power"))
            flags.power = true;
        if (normalized.contains("vitesse") || normalized.contains("speed"))
            flags.speed = true;
        if (normalized.contains("passer") || normalized.contains("passes"))
            flags.passes = true;
        int count = Math.min(node.getChildCount(), 100);
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                inspect(child, flags, depth + 1);
                child.recycle();
            }
        }
    }

    private void save(String value) {
        // Save only a bounded diagnostic description; never save user-entered
        // settings, screen content, screenshots or values.
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_RESULT, value).apply();
    }

    @Override public void onInterrupt() { }
}
