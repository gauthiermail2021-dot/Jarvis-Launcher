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

/**
 * One explicitly consented projection for one Jarvis session.
 *
 * Never captures, processes or persists images while idle. Only an explicit
 * bubble scan or an SVG handoff to xTool activates its ImageReader surface.
 * The projection's single VirtualDisplay is reused for all scans.
 * No setting changes, gestures, machine controls or laser start actions.
 */
public final class EasySetSessionService extends Service {
    static final String ACTION_START = "ca.jarvis.bridge.EASYSET_SESSION_START";
    static final String ACTION_SCAN = "ca.jarvis.bridge.EASYSET_SESSION_SCAN";
    static final String ACTION_SVG_OPEN = "ca.jarvis.bridge.EASYSET_SESSION_SVG";
    static final String ACTION_XTOOL_FOREGROUND = "ca.jarvis.bridge.EASYSET_XTOOL_FOREGROUND";
    static final String ACTION_PANEL_TAP = "ca.jarvis.bridge.EASYSET_PANEL_TAP";
    static final String ACTION_STOP = "ca.jarvis.bridge.EASYSET_SESSION_STOP";
    static final String RESULT_CODE = "projection_code";
    static final String RESULT_INTENT = "projection_intent";

    private static final int NOTIFICATION_ID = 935;
    private static final String CHANNEL = "jarvis_easyset_session_126";
    private static volatile boolean sessionActive;
    // True only after an explicitly authorized screenshot proves the
    // "Volet EasySet" settings row is CLOSED. Accessibility uses it solely
    // as a gate for the next xTool UI change; no screen polling.
    private static volatile boolean awaitingPanelOpen;
    // Only a screenshot that actually sees the three EasySet labels makes
    // this true. An unlabeled xTool click can rescan at most once per 8s.
    private static volatile boolean lastPanelWasRecognized;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader reader;
    private boolean closing;
    private boolean scanPending;
    private boolean analyzing;
    private boolean autoScan;
    private boolean autoRecognizedEasySet;
    private long scanReadyAt;
    private int captureWidth;
    private int captureHeight;
    private Runnable scanTimeout;
    private Runnable autoScanTrigger;

    static boolean isActive() {
        return sessionActive;
    }

    static boolean isAwaitingPanelOpen() {
        return sessionActive && awaitingPanelOpen;
    }

    static boolean needsPanelInspection() {
        return sessionActive && (!lastPanelWasRecognized || awaitingPanelOpen);
    }

    static void requestScan(Context context, boolean fromSvg) {
        if (!isActive()) return;
        Intent intent = new Intent(context, EasySetSessionService.class);
        intent.setAction(fromSvg ? ACTION_SVG_OPEN : ACTION_SCAN);
        try { context.startService(intent); }
        catch (RuntimeException ignored) { }
    }

    /**
     * Detect an already open xTool document: no SVG handoff is required.
     * This cannot start a new projection or bypass Android consent.
     */
    static void requestExistingXtoolScan(Context context) {
        if (!isActive()) return;
        Intent intent = new Intent(context, EasySetSessionService.class);
        intent.setAction(ACTION_XTOOL_FOREGROUND);
        try { context.startService(intent); }
        catch (RuntimeException ignored) { }
    }

    /**
     * React to one actual xTool tap event. This is NOT a touch simulation.
     * The capture is restricted to an existing Android-consented session.
     */
    static void requestPanelTapScan(Context context) {
        if (!isActive()) return;
        Intent intent = new Intent(context, EasySetSessionService.class);
        intent.setAction(ACTION_PANEL_TAP);
        try { context.startService(intent); }
        catch (RuntimeException ignored) { }
    }

    static void requestStop(Context context) {
        Intent intent = new Intent(context, EasySetSessionService.class);
        intent.setAction(ACTION_STOP);
        // Stopping a service is allowed even when Android blocks background starts.
        // The service's onDestroy() closes MediaProjection and clears its Surface.
        try { context.stopService(intent); }
        catch (RuntimeException ignored) { }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            closeSession();
            return START_NOT_STICKY;
        }
        if (ACTION_START.equals(action)) {
            if (!sessionActive && projection == null) initialize(intent);
            return START_NOT_STICKY;
        }
        if (!sessionActive || closing || virtualDisplay == null) return START_NOT_STICKY;
        if (ACTION_SCAN.equals(action)) {
            cancelPendingAutoScan();
            beginScan(false);
        } else if (ACTION_XTOOL_FOREGROUND.equals(action)) {
            // One passive scan on an xTool UI transition. An existing drawing
            // may coexist with an old XtoolSessionManager flag; do not reject.
            // An incoming SVG already has its own pending OCR trigger.
            if (autoRecognizedEasySet || autoScanTrigger != null
                    || scanPending || analyzing) return START_NOT_STICKY;
            autoScanTrigger = () -> {
                autoScanTrigger = null;
                if (sessionActive && !closing && !autoRecognizedEasySet) {
                    beginScan(true);
                }
            };
            handler.postDelayed(autoScanTrigger, 1500L);
        } else if (ACTION_PANEL_TAP.equals(action)) {
            // xTool's custom EasySet row is not reliably a distinct Android
            // window. Its click can happen without any preceding CLOSED OCR.
            // Honor this user gesture even after a previous successful scan.
            // Never interrupt a screenshot/OCR already in progress.
            if (scanPending || analyzing || autoScanTrigger != null) return START_NOT_STICKY;
            autoRecognizedEasySet = false;
            awaitingPanelOpen = false;
            autoScanTrigger = () -> {
                autoScanTrigger = null;
                if (sessionActive && !closing) beginScan(true);
            };
            handler.postDelayed(autoScanTrigger, 900L);
        } else if (ACTION_SVG_OPEN.equals(action)) {
            autoRecognizedEasySet = false;
            awaitingPanelOpen = false;
            lastPanelWasRecognized = false;
            cancelPendingAutoScan();
            // xTool needs time to render the newly opened SVG.
            // This is intentionally one read-only diagnostic, not a polling loop.
            autoScanTrigger = () -> {
                autoScanTrigger = null;
                if (sessionActive && XtoolSessionManager.isActive(this)) beginScan(true);
            };
            handler.postDelayed(autoScanTrigger, 6500L);
        }
        return START_NOT_STICKY;
    }

    private void initialize(Intent grant) {
        awaitingPanelOpen = false;
        lastPanelWasRecognized = false;
        createNotification();
        try {
            int code = grant.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED);
            Intent data = grant.getParcelableExtra(RESULT_INTENT);
            if (code != Activity.RESULT_OK || data == null)
                throw new IllegalArgumentException("Consent missing");
            MediaProjectionManager manager = (MediaProjectionManager)
                    getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (manager == null) throw new IllegalStateException("Projection unavailable");
            projection = manager.getMediaProjection(code, data);
            if (projection == null) throw new IllegalStateException("Projection refused");
            projection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() {
                    // Android revokes capture on lock, another sharing request,
                    // user action, or service/process shutdown.
                    handler.post(() -> closeSession());
                }
            }, handler);

            WindowManager managerWindow = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (managerWindow == null) throw new IllegalStateException("No display");
            DisplayMetrics dm = new DisplayMetrics();
            managerWindow.getDefaultDisplay().getRealMetrics(dm);
            captureWidth = dm.widthPixels;
            captureHeight = dm.heightPixels;
            if (captureWidth <= 0 || captureHeight <= 0)
                throw new IllegalStateException("Invalid display");
            // Create the VirtualDisplay exactly once per consent. An inactive
            // null Surface prevents frame reading until a scan is requested.
            virtualDisplay = projection.createVirtualDisplay(
                    "Jarvis-EasySet-session", captureWidth, captureHeight, dm.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, null, null, handler);
            if (virtualDisplay == null) throw new IllegalStateException("No virtual display");
            sessionActive = true;
            updateNotification();
        } catch (RuntimeException e) {
            EasySetVisionAnalyzer.save(this, "Session de capture indisponible : "
                    + e.getClass().getSimpleName());
            closeSession();
        }
    }

    private void beginScan(boolean automatically) {
        if (!sessionActive || virtualDisplay == null || scanPending || analyzing) return;
        scanPending = true;
        autoScan = automatically;
        JarvisBubbleService.prepareEasySetCapture();
        // Small delay lets the Jarvis menu disappear before attaching the surface.
        handler.postDelayed(() -> {
            if (!scanPending || virtualDisplay == null || closing) return;
            try {
                // Avoid storing any frames or raw OCR from the earlier ChatGPT session.
                reader = ImageReader.newInstance(captureWidth, captureHeight,
                        PixelFormat.RGBA_8888, 2);
                scanReadyAt = SystemClock.elapsedRealtime() + 550L;
                reader.setOnImageAvailableListener(this::onImageAvailable, handler);
                virtualDisplay.setSurface(reader.getSurface());
                scanTimeout = () -> {
                    if (scanPending) finishScan("Aucune image EasySet disponible.", false);
                };
                handler.postDelayed(scanTimeout, 6500L);
            } catch (RuntimeException e) {
                finishScan("Capture temporaire impossible : "
                        + e.getClass().getSimpleName(), false);
            }
        }, 800L);
    }

    private void onImageAvailable(ImageReader source) {
        if (!scanPending || closing) return;
        Image image = null;
        try {
            image = source.acquireLatestImage();
            if (image == null) return;
            if (SystemClock.elapsedRealtime() < scanReadyAt) return;
            scanPending = false;
            analyzing = true;
            cancelScanTimeout();
            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            int width = image.getWidth();
            int height = image.getHeight();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int paddedWidth = width + (rowStride - width * pixelStride) / pixelStride;
            Bitmap padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888);
            buffer.rewind();
            padded.copyPixelsFromBuffer(buffer);
            Bitmap bitmap = Bitmap.createBitmap(padded, 0, 0, width, height);
            if (bitmap != padded) padded.recycle();
            // Release the acquired frame before closing the ImageReader.
            image.close();
            image = null;
            detachReader();
            EasySetVisionAnalyzer.analyze(this, bitmap, summary ->
                    finishScan(summary, true));
        } catch (RuntimeException exception) {
            finishScan("Image non lisible : " + exception.getClass().getSimpleName(), false);
        } finally {
            if (image != null) {
                try { image.close(); } catch (RuntimeException ignored) { }
            }
        }
    }

    private void finishScan(String summary, boolean analyzed) {
        cancelScanTimeout();
        detachReader();
        boolean automatic = autoScan;
        scanPending = false;
        analyzing = false;
        autoScan = false;
        EasySetVisionAnalyzer.save(this, summary);

        boolean closed = analyzed && summary != null
                && summary.startsWith("Volet EasySet fermé.");
        boolean recognized = analyzed && summary != null
                && (summary.startsWith("EasySet reconnu")
                    || summary.startsWith("EasySet probable"));
        if (closed) {
            // A previous successful scan must not block the next panel-open
            // scan. The user may have closed EasySet after reading it.
            autoRecognizedEasySet = false;
            // Allow the next relevant xTool UI-change event to request
            // another one-shot capture. Jarvis never clicks in xTool.
            awaitingPanelOpen = true;
            lastPanelWasRecognized = false;
        } else if (recognized) {
            awaitingPanelOpen = false;
            lastPanelWasRecognized = true;
            autoRecognizedEasySet = true;
        }

        if (!closing && sessionActive) {
            // CLOSED is an actionable hint, not an OCR error.
            // Unknown screens remain quiet on automatic scans.
            if (!automatic || recognized || closed) {
                JarvisBubbleService.showEasySetResult(summary);
            } else {
                JarvisBubbleService.restoreEasySetBubble();
            }
        }
    }

    private void detachReader() {
        if (virtualDisplay != null) {
            try { virtualDisplay.setSurface(null); } catch (RuntimeException ignored) { }
        }
        if (reader != null) {
            try { reader.close(); } catch (RuntimeException ignored) { }
            reader = null;
        }
    }

    private void cancelScanTimeout() {
        if (scanTimeout != null) handler.removeCallbacks(scanTimeout);
        scanTimeout = null;
    }

    private void cancelPendingAutoScan() {
        if (autoScanTrigger != null) handler.removeCallbacks(autoScanTrigger);
        autoScanTrigger = null;
    }

    private void closeSession() {
        if (closing) return;
        closing = true;
        sessionActive = false;
        cancelPendingAutoScan();
        cancelScanTimeout();
        scanPending = false;
        analyzing = false;
        autoRecognizedEasySet = false;
        awaitingPanelOpen = false;
        lastPanelWasRecognized = false;
        detachReader();
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (projection != null) {
            MediaProjection old = projection;
            projection = null;
            try { old.stop(); } catch (RuntimeException ignored) { }
        }
        JarvisBubbleService.restoreEasySetBubble();
        stopForeground(true);
        stopSelf();
    }

    private void createNotification() {
        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && manager != null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                    "Partage d'écran Jarvis", NotificationManager.IMPORTANCE_LOW));
        }
        startForegroundWithNotification();
    }

    private void updateNotification() {
        startForegroundWithNotification();
    }

    private void startForegroundWithNotification() {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, EasySetSessionService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 1, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        Notification notice = builder
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setContentTitle("Jarvis • capture autorisée pour cette session")
                .setContentText("Aucune analyse continue. Lecture uniquement à la demande ou avec un SVG xTool.")
                .setStyle(new Notification.BigTextStyle().bigText(
                        "Partage Android actif. Jarvis analyse seulement sur commande "
                                + "ou après ouverture d'un SVG dans xTool."))
                .setContentIntent(pending)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel,
                        "Arrêter la capture", stopAction)
                .build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notice,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(NOTIFICATION_ID, notice);
        }
    }

    @Override public void onDestroy() {
        closeSession();
        super.onDestroy();
    }
}
