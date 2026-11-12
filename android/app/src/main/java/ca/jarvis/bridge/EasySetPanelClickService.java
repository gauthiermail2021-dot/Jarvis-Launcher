package ca.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.Locale;

/**
 * Test EXPLICITE, une seule fois, du bouton qui ouvre le volet EasySet.
 * Aucun clic sur puissance, vitesse, passes, execution ou commande laser.
 * Ne lit l'arbre Android qu'au moment du test demande depuis la bulle.
 */
public final class EasySetPanelClickService extends AccessibilityService {
    private static volatile EasySetPanelClickService active;
    private static final String BUTTON = "Volet EasySet";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean inProgress;

    static void requestOnce(Context context) {
        EasySetPanelClickService service = active;
        if (service == null) {
            JarvisBubbleService.showEasySetResult(
                    "Test EasySet : service d'accessibilite inactif. "
                    + "Activer « Jarvis — test bouton EasySet » dans les reglages Android, "
                    + "puis relancer le test depuis la bulle.");
            Intent settings = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { context.startActivity(settings); }
            catch (RuntimeException ignored) { }
            return;
        }
        if (service.inProgress) {
            JarvisBubbleService.showEasySetResult("Test EasySet deja en cours.");
            return;
        }
        service.inProgress = true;
        // Laisse disparaitre le menu flottant et revenir le focus a xTool.
        service.handler.postDelayed(service::testClick, 600L);
    }

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        active = this;
    }

    @Override public void onDestroy() {
        if (active == this) active = null;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // Ecoute passive uniquement : aucun declenchement automatique.
    }

    @Override public void onInterrupt() { }

    private void testClick() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        try {
            if (root == null) {
                finish("Test EasySet : aucune fenetre Android accessible.");
                return;
            }
            if (!isXtool(root)) {
                finish("Test EasySet : xTool n'est pas la fenetre active. "
                        + "Ouvrir xTool avant de relancer le test.");
                return;
            }
            if (panelOpen(root)) {
                finish("Test EasySet : volet deja ouvert (Puissance, Vitesse, Passer visibles). "
                        + "Aucun clic envoye.");
                return;
            }
            AccessibilityNodeInfo button = findExactButton(root, 0);
            if (button == null) {
                finish("Test EasySet : bouton « Volet EasySet » introuvable.");
                return;
            }
            boolean accepted;
            try {
                accepted = button.isClickable()
                        && button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            } finally {
                button.recycle();
            }
            if (!accepted) {
                finish("Test EasySet : bouton trouve, mais ACTION_CLICK refusee.");
                return;
            }
            // Le retour true signale l'acceptation Android, pas l'ouverture xTool.
            handler.postDelayed(() -> {
                AccessibilityNodeInfo after = getRootInActiveWindow();
                try {
                    boolean opened = after != null && isXtool(after) && panelOpen(after);
                    finish("Test EasySet\nBouton : trouve\nCLICK Android : accepte\n"
                            + "Volet ouvert : " + (opened ? "oui" : "non confirme")
                            + "\nAucun parametre ni commande laser modifie.");
                } finally {
                    if (after != null) after.recycle();
                }
            }, 850L);
        } catch (RuntimeException error) {
            finish("Test EasySet : erreur Android " + error.getClass().getSimpleName());
        } finally {
            if (root != null) root.recycle();
        }
    }

    private void finish(String result) {
        inProgress = false;
        JarvisBubbleService.showEasySetResult(result);
    }

    private boolean isXtool(AccessibilityNodeInfo root) {
        CharSequence name = root.getPackageName();
        if (name == null) return false;
        String pkg = name.toString();
        if (pkg.toLowerCase(Locale.ROOT).contains("xtool")) return true;
        try {
            String label = String.valueOf(getPackageManager().getApplicationLabel(
                    getPackageManager().getApplicationInfo(pkg, 0)));
            return label.toLowerCase(Locale.ROOT).contains("xtool");
        } catch (PackageManager.NameNotFoundException | RuntimeException error) {
            return false;
        }
    }

    private static boolean panelOpen(AccessibilityNodeInfo root) {
        return hasText(root, "Puissance", 0) && hasText(root, "Vitesse", 0)
                && hasText(root, "Passer", 0);
    }

    private static boolean hasText(AccessibilityNodeInfo node, String needle, int depth) {
        if (node == null || depth > 20) return false;
        CharSequence text = node.getText();
        CharSequence description = node.getContentDescription();
        if ((text != null && text.toString().toLowerCase(Locale.ROOT)
                .contains(needle.toLowerCase(Locale.ROOT)))
                || (description != null && description.toString().toLowerCase(Locale.ROOT)
                .contains(needle.toLowerCase(Locale.ROOT)))) return true;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try { if (hasText(child, needle, depth + 1)) return true; }
            finally { child.recycle(); }
        }
        return false;
    }

    private static AccessibilityNodeInfo findExactButton(
            AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 20) return null;
        CharSequence label = node.getText();
        if (label == null) label = node.getContentDescription();
        if (label != null && BUTTON.equalsIgnoreCase(label.toString().trim())
                && node.isClickable()) return AccessibilityNodeInfo.obtain(node);
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child == null) continue;
            try {
                AccessibilityNodeInfo found = findExactButton(child, depth + 1);
                if (found != null) return found;
            } finally {
                child.recycle();
            }
        }
        return null;
    }
}
