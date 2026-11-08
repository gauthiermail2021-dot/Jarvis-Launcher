package ca.jarvis.bridge;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;

/**
 * Option facultative : observe seulement le passage de ChatGPT au premier plan.
 * Ne demande pas l'accès au contenu des fenêtres et ne lit aucun message.
 * Ce service n'est actif qu'après autorisation explicite dans Android Accessibilité.
 */
public final class ChatGptBubbleAccessibilityService extends AccessibilityService {
    private static final String CHATGPT_PACKAGE = "com.openai.chatgpt";
    private long lastOpenRequestMillis = 0L;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return;
        if (!CHATGPT_PACKAGE.contentEquals(event.getPackageName())) return;
        if (!Settings.canDrawOverlays(this) || JarvisBubbleService.isBubbleActive()) return;

        long now = SystemClock.elapsedRealtime();
        if (now - lastOpenRequestMillis < 2500L) return;
        lastOpenRequestMillis = now;
        try {
            Intent bubble = new Intent(this, JarvisBubbleService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(bubble);
            else startService(bubble);
        } catch (RuntimeException ignored) {
            // Certaines versions Android refusent encore les départs en arrière-plan.
            // L'ouverture manuelle dans Jarvis reste toujours disponible.
        }
    }

    @Override
    public void onInterrupt() {
        // Aucun traitement permanent à interrompre.
    }
}
