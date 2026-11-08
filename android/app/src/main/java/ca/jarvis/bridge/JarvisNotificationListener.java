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
import java.util.Locale;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class JarvisNotificationListener extends NotificationListenerService {
    static final int NOTIFICATION_ID = 707;
    private static final String CHANNEL_ID = "jarvis_svg_alerts";
    // Gmail peut publier une notification par boîte de réception et par
    // libellé pour le même message. Un premier événement ouvre un court
    // regroupement, puis une période de protection ignore les répétitions.
    // Le contrôle des identifiants Gmail et des SHA-256 dans MainActivity
    // reste la protection définitive contre une double ouverture de SVG.
    private static final long GROUP_WINDOW_MS = 2000L;
    private static final long DUPLICATE_GUARD_MS = 8000L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pending;
    private boolean pendingMentionsJarvisSvg = false;
    private long lastDispatchElapsed = -DUPLICATE_GUARD_MS;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !"com.google.android.gm".equals(sbn.getPackageName()))
            return;
        final boolean labeled = mentionsJarvisSvg(sbn);
        // Le NotificationListenerService n'est pas tenu d'appeler ce callback
        // sur le thread UI : toute la gestion du délai reste sur un Handler.
        handler.post(() -> onGmailNotification(labeled));
    }

    private void onGmailNotification(boolean mentionsLabel) {
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastDispatchElapsed < DUPLICATE_GUARD_MS) {
            // Une deuxième alerte Gmail (compte + libellé) ne doit pas
            // lancer une deuxième Activity ni faire clignoter la bulle.
            return;
        }
        pendingMentionsJarvisSvg |= mentionsLabel;
        if (pending != null) return; // Premier signal programme déjà le lot.
        pending = () -> {
            pending = null;
            boolean labeled = pendingMentionsJarvisSvg;
            pendingMentionsJarvisSvg = false;
            lastDispatchElapsed = android.os.SystemClock.elapsedRealtime();
            JarvisBubbleService.onGmailNotificationReceived();
            if (MainActivity.handleGmailNoticeIfVisible()) return;
            boolean enabled = getSharedPreferences("jarvis_bridge", MODE_PRIVATE)
                    .getBoolean(MainActivity.AUTO_OPEN_ENABLED, false);
            if (enabled && labeled
                    && JarvisBubbleService.tryAutoOpenFromNotification(this)) return;
            // Conserver le repli manuel si Android bloque le démarrage.
            showJarvisAlert();
        };
        handler.postDelayed(pending, GROUP_WINDOW_MS);
    }

    private static boolean mentionsJarvisSvg(StatusBarNotification notice) {
        Notification notification = notice.getNotification();
        Bundle extras = notification == null ? null : notification.extras;
        if (extras == null) return false;
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        String combined = (String.valueOf(title) + " " + String.valueOf(text)
                + " " + String.valueOf(bigText)).toLowerCase(Locale.ROOT);
        return combined.contains("jarvis svg");
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
        pendingMentionsJarvisSvg = false;
        super.onDestroy();
    }
}
