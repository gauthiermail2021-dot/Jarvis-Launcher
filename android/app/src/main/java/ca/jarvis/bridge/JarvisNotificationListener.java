package ca.jarvis.bridge;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class JarvisNotificationListener extends NotificationListenerService {
    static final int NOTIFICATION_ID = 707;
    private static final String CHANNEL_ID = "jarvis_svg_alerts";
    // Les alertes Gmail rapprochées sont regroupées, jamais jetées :
    // une alerte reçue pendant une recherche sera rejouée après celle-ci
    // par le verrou et le drapeau de reprise dans MainActivity.
    private static final long GROUP_WINDOW_MS = 1800L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pending;
    private boolean pendingLooksLikeJarvis = false;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !"com.google.android.gm".equals(sbn.getPackageName()))
            return;
        if (XtoolSessionManager.isActive(this)) return;
        Notification notice = sbn.getNotification();
        if (notice == null || (notice.flags & Notification.FLAG_GROUP_SUMMARY) != 0)
            return; // Les regroupements Gmail ne sont pas de nouveaux courriels.
        Bundle extras = notice.extras;
        String title = extras == null ? "" : String.valueOf(
                extras.getCharSequence(Notification.EXTRA_TITLE, ""));
        String detail = extras == null ? "" : String.valueOf(
                extras.getCharSequence(Notification.EXTRA_TEXT, ""));
        boolean jarvisHint = JarvisGmailNoticePolicy.isJarvisHint(title, detail);
        // Une notification Gmail ordinaire peut toujours déclencher la
        // vérification silencieuse du libellé; seul le repli manuel est filtré.
        handler.post(() -> onGmailNotification(jarvisHint));
    }

    private void onGmailNotification(boolean jarvisHint) {
        if (XtoolSessionManager.isActive(this)) return;
        pendingLooksLikeJarvis |= jarvisHint;
        if (pending != null) return; // Les alertes du même lot sont regroupées.
        pending = () -> {
            pending = null;
            boolean showManualFallback = pendingLooksLikeJarvis;
            pendingLooksLikeJarvis = false;
            // Ne pas lancer Jarvis si l'utilisateur est en session xTool,
            // même si la notification était déjà en attente.
            if (XtoolSessionManager.isActive(this)) return;
            // Notification brute ≠ nouveau courriel admissible. Ne réveiller
            // la bulle qu'après analyse d'une pièce jointe par MainActivity.
            if (MainActivity.handleGmailNoticeIfVisible()) return;
            boolean enabled = getSharedPreferences("jarvis_bridge", MODE_PRIVATE)
                    .getBoolean(MainActivity.AUTO_OPEN_ENABLED, false);
            // Les etiquettes Gmail ne figurent pas toujours dans le texte de la
            // notification. Seule la recherche Gmail via le libelle Jarvis SVG
            // decide si un nouveau SVG autorise doit etre ouvert.
            if (enabled && JarvisBubbleService.tryAutoOpenFromNotification(this)) return;
            // Ancien défaut : une alerte Jarvis apparaissait pour CHAQUE
            // notification Gmail, même sans SVG. Ne proposer un repli manuel
            // que si le titre ou texte évoque explicitement Jarvis / SVG.
            if (showManualFallback) showJarvisAlert();
        };
        handler.postDelayed(pending, GROUP_WINDOW_MS);
    }

    static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID, "Jarvis SVG", NotificationManager.IMPORTANCE_HIGH));
        }
    }

    private void showJarvisAlert() {
        ensureChannel(this);
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) return;

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(MainActivity.ACTION_CHECK_GMAIL);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent action = PendingIntent.getActivity(this, NOTIFICATION_ID, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification alert = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("Jarvis Bridge — nouveau message Gmail")
                .setContentText("Toucher pour récupérer le SVG de Jarvis SVG et ouvrir xTool")
                .setAutoCancel(true)
                .setContentIntent(action)
                .build();
        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.notify(NOTIFICATION_ID, alert);
    }

    @Override
    public void onDestroy() {
        if (pending != null) handler.removeCallbacks(pending);
        pending = null;
        pendingLooksLikeJarvis = false;
        super.onDestroy();
    }
}
