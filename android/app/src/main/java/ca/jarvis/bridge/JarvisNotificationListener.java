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
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class JarvisNotificationListener extends NotificationListenerService {
    static final int NOTIFICATION_ID = 707;
    private static final String CHANNEL_ID = "jarvis_svg_alerts";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pending;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || !"com.google.android.gm".equals(sbn.getPackageName())) return;
        // Le signal concerne Gmail en général, pas nécessairement un fichier SVG.
        JarvisBubbleService.onGmailNotificationReceived();

        // Les deux notifications Gmail (boîte + étiquette) deviennent un signal unique.
        if (pending != null) handler.removeCallbacks(pending);
        pending = () -> {
            if (!MainActivity.handleGmailNoticeIfVisible()) {
                showJarvisAlert();
            }
        };
        handler.postDelayed(pending, 2000L); // Anti-doublon, pas une interrogation périodique.
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
        super.onDestroy();
    }
}
