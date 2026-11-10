package ca.jarvis.bridge;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.nio.ByteBuffer;

/** Single user-consented screenshot, temporary in-memory OCR, no controls or logging. */
public final class EasySetCaptureService extends Service {
    static final String RESULT_CODE = "projection_code";
    static final String RESULT_INTENT = "projection_intent";
    private static final int NOTIFICATION_ID = 923;
    private static final int RESULT_NOTIFICATION_ID = 924;
    private static final String CHANNEL = "jarvis_easyset_scan";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ImageReader reader;
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private boolean captured;
    private boolean finished;
    private long captureAt;
    private final Runnable timeout = () -> {
        if (!captured) finish("Capture indisponible. Essaie une capture d'écran "
                + "manuelle dans le second bouton.");
    };

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        createNotification();
        if (intent == null) {
            finish("Autorisation de capture manquante.");
            return START_NOT_STICKY;
        }
        try {
            MediaProjectionManager manager = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent data = intent.getParcelableExtra(RESULT_INTENT);
            int code = intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED);
            if (manager == null || code != Activity.RESULT_OK || data == null) {
                finish("Autorisation de capture absente.");
                return START_NOT_STICKY;
            }
            projection = manager.getMediaProjection(code, data);
            if (projection == null) {
                finish("Capture refusée par Android.");
                return START_NOT_STICKY;
            }
            projection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() {
                    if (!captured) finish("La capture a été interrompue.");
                }
            }, handler);

            WindowManager window = (WindowManager) getSystemService(WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            if (window == null) throw new IllegalStateException("No display");
            window.getDefaultDisplay().getRealMetrics(metrics);
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            if (width <= 0 || height <= 0) throw new IllegalStateException("Bad display size");
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3);
            // Wait for the diagnostic Activity / Android permission window
            // to disappear and for the prior xTool task to come to foreground.
            captureAt = SystemClock.elapsedRealtime() + 2600L;
            reader.setOnImageAvailableListener(this::onImage, handler);
            virtualDisplay = projection.createVirtualDisplay("Jarvis-EasySet-unique",
                    width, height, metrics.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.getSurface(), null, handler);
            handler.postDelayed(timeout, 10000L);
        } catch (RuntimeException e) {
            finish("Capture indisponible : " + e.getClass().getSimpleName());
        }
        return START_NOT_STICKY;
    }

    private void onImage(ImageReader available) {
        if (captured || finished) return;
        Image image = null;
        try {
            image = available.acquireLatestImage();
            if (image == null) return;
            if (SystemClock.elapsedRealtime() < captureAt) return;
            captured = true;
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer pixels = plane.getBuffer();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int width = image.getWidth(), height = image.getHeight();
            int paddedWidth = width + (rowStride - pixelStride * width) / pixelStride;
            Bitmap padded = Bitmap.createBitmap(
                    paddedWidth, height, Bitmap.Config.ARGB_8888);
            pixels.rewind();
            padded.copyPixelsFromBuffer(pixels);
            Bitmap cropped = Bitmap.createBitmap(padded, 0, 0, width, height);
            if (cropped != padded) padded.recycle();
            image.close();
            image = null;
            // The screenshot remains only in RAM for a single OCR pass.
            releaseProjection();
            EasySetVisionAnalyzer.analyze(this, cropped, this::finish);
        } catch (RuntimeException error) {
            finish("Image non lisible : " + error.getClass().getSimpleName());
        } finally {
            if (image != null) image.close();
        }
    }

    private void createNotification() {
        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && manager != null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                    "Diagnostic visuel EasySet", NotificationManager.IMPORTANCE_LOW));
        }
        Intent show = new Intent(this, EasySetVisionActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, show,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        Notification.Builder note = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Notification notification = note.setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Jarvis • analyse visuelle")
                .setContentText("Capture unique autorisée, aucune commande xTool.")
                .setContentIntent(pending)
                .setOngoing(true)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void releaseProjection() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (reader != null) {
            reader.close();
            reader = null;
        }
        if (projection != null) {
            MediaProjection old = projection;
            projection = null;
            old.stop();
        }
    }

    private void finish(String summary) {
        if (finished) return;
        finished = true;
        handler.removeCallbacks(timeout);
        releaseProjection();
        EasySetVisionAnalyzer.save(this, summary);
        // Restore Jarvis bubble and show a dismissible result overlay directly
        // above xTool; this runs only after the one-shot projection is stopped.
        JarvisBubbleService.showEasySetResult(summary);
        notifyResult(summary);
        stopForeground(true);
        stopSelf();
    }

    private void notifyResult(String summary) {
        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL + "_result", "Résultats EasySet",
                    NotificationManager.IMPORTANCE_DEFAULT));
        }
        Intent open = new Intent(this, EasySetVisionActivity.class);
        PendingIntent intent = PendingIntent.getActivity(this, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL + "_result")
                : new Notification.Builder(this);
        Notification note = builder.setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentTitle("Jarvis • résultat EasySet")
                .setContentText(summary)
                .setStyle(new Notification.BigTextStyle().bigText(summary))
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build();
        try {
            manager.notify(RESULT_NOTIFICATION_ID, note);
        } catch (SecurityException ignored) {
            // Résultat disponible via la bulle même si les notifications sont interdites.
        }
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(timeout);
        releaseProjection();
        super.onDestroy();
    }
}
