package ca.jarvis.bridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.SystemClock;
import java.util.ArrayDeque;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Bulle Jarvis 1.0 : informations locales, signaux lumineux et réduction automatique. */
public final class JarvisBubbleService extends Service {
    public static final String ACTION_STOP = "ca.jarvis.bridge.STOP_BUBBLE";
    private static final String CHANNEL = "jarvis_bubble_foreground";
    private static final int NOTIFICATION_ID = 808;
    private static final String PREFS = "jarvis_bubble";
    private static final int SMALL_DP = 54;
    private static final int LARGE_DP = 110;
    private static final long LONG_PRESS_MS = 1000L;
    private static final long IDLE_COLLAPSE_MS = 5000L;
    private static final long OPENING_SPIN_MS = 850L;
    private static volatile JarvisBubbleService activeService;

    static boolean isBubbleActive() {
        JarvisBubbleService current = activeService;
        return current != null && current.bubble != null
                && current.bubble.isAttachedToWindow();
    }

    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private BubbleView bubble;
    // Independent, movable SVG test overlay; no change to the main bubble UX.
    private JarvisTestBubbleOverlay svgTestBubble;
    private boolean expanded = false;
    private boolean dragging = false;
    private float downX, downY;
    private int startX, startY;
    private final Handler gestureHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingLongPress;
    private boolean fingerDown = false;
    private boolean longPressHandled = false;
    private View menuView;
    private boolean consentAttemptedForThisBubble;
    private View easySetBanner;
    private Runnable easySetBannerDismiss;
    private Runnable collapseRunnable;
    private ValueAnimator openingAnimator;
    private long openingSpinUntil = 0L;
    private boolean animationsEnabled = true;
    private boolean soundsEnabled = true;
    private JarvisXylophone xylophone;
    private static final int STATE_READY = 0;
    private static final int STATE_GMAIL = 1;
    private static final int STATE_WORKING = 2;
    private static final int STATE_SUCCESS = 3;
    private static final int STATE_ERROR = 4;
    private static final int STATE_XTOOL = 5;
    private int visualState = STATE_READY;
    private String visualTitle = "JARVIS";
    private String visualDetail = "PRÊT";
    private long visualUntil = 0L;
    // Gmail notification, search, SVG reception and xTool handoff can all
    // happen faster than a human can read. Only the OVERLAY labels are
    // serialized; the real Gmail/Android processing remains asynchronous.
    private final ArrayDeque<VisualStage> stageQueue = new ArrayDeque<>();
    private boolean stageHolding;
    private Runnable stageAdvance;

    private static final class VisualStage {
        final int state;
        final String title, detail;

        VisualStage(int state, String title, String detail) {
            this.state = state;
            this.title = title;
            this.detail = detail;
        }

        boolean sameAs(VisualStage other) {
            return other != null && state == other.state
                    && title.equals(other.title) && detail.equals(other.detail);
        }
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        // Ne pas republier la notification de premier plan à chaque appel du service.
        if (bubble == null) {
            createNotification();
            try {
                showBubble();
                // If ChatGPT started this floating bubble, request the one
                // Android consent for the Jarvis session once. A denied/blocked
                // start falls back to the existing bubble menu.
                if (!consentAttemptedForThisBubble) {
                    consentAttemptedForThisBubble = true;
                    gestureHandler.postDelayed(() -> {
                        if (bubble != null && !EasySetSessionService.isActive()) {
                            launchEasySetSessionConsent();
                        }
                    }, 1300L);
                }
            }
            catch (Exception error) {
                Toast.makeText(this, "Jarvis : " + error.getMessage(), Toast.LENGTH_LONG).show();
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        return START_STICKY;
    }

    private void createNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL, "Bulle Jarvis", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }
        Intent app = new Intent(this, MainActivity.class);
        app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open = PendingIntent.getActivity(this, 809, app,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stop = new Intent(this, JarvisBubbleService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 810, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification note = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Jarvis Bridge").setContentText("Bulle flottante active")
                .setContentIntent(open).setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Arrêter", stopAction)
                .build();
        startForeground(NOTIFICATION_ID, note);
    }

    private int dp(float size) {
        return Math.round(size * getResources().getDisplayMetrics().density);
    }

    private void showBubble() {
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) throw new IllegalStateException("WindowManager indisponible");
        int side = dp(SMALL_DP);
        params = new WindowManager.LayoutParams(side, side,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        Point display = new Point();
        wm.getDefaultDisplay().getRealSize(display);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        animationsEnabled = prefs.getBoolean("animations", true);
        soundsEnabled = prefs.getBoolean("sounds_enabled", true);
        params.x = prefs.getInt("x", Math.max(0, display.x - dp(80)));
        params.y = prefs.getInt("y", Math.max(dp(100), display.y / 3));
        clampPosition();
        bubble = new BubbleView(this);
        bubble.setOnTouchListener((v, event) -> handleTouch(event));
        wm.addView(bubble, params);
        activeService = this;
        xylophone = new JarvisXylophone(this);
        if (prefs.getBoolean("test_svg_bubble_enabled", true)) showSvgTestBubble();
        // Si Android relance la bulle pendant le travail, restaurer l'état.
        if (XtoolSessionManager.isActive(this)) {
            visualState = STATE_XTOOL;
            visualTitle = "xTool";
            visualDetail = "EN COURS";
            bubble.invalidate();
        }
    }

    private void clampPosition() {
        Point display = new Point();
        wm.getDefaultDisplay().getRealSize(display);
        params.x = Math.max(0, Math.min(params.x, display.x - params.width));
        params.y = Math.max(0, Math.min(params.y, display.y - params.height));
    }


    /** Small 48 dp floating SVG bubble, shown beside Jarvis by default. */
    private void showSvgTestBubble() {
        if (svgTestBubble != null || wm == null || bubble == null) return;
        // Default is adjacent but independent; the user may drag it anywhere.
        int left = params.x < dp(70) ? params.x + dp(65) : params.x - dp(55);
        int top = params.y + dp(60);
        JarvisTestBubbleOverlay overlay = new JarvisTestBubbleOverlay(
                this, wm, left, top, this::launchNumberedSvgTest);
        try {
            overlay.show();
            svgTestBubble = overlay;
        } catch (RuntimeException error) {
            overlay.destroy();
            // The long-press Jarvis menu retains the test-sender fallback.
            Toast.makeText(this, "Bulle SVG non disponible : "
                    + error.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
        }
    }

    /** Explicit user gesture only: run the existing Gmail-to-xTool test flow. */
    private void launchNumberedSvgTest() {
        if (!Settings.canDrawOverlays(this)) return;
        Intent send = new Intent(this, JarvisSilentSvgSendActivity.class);
        send.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_NO_ANIMATION
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        try {
            startActivity(send);
        } catch (RuntimeException error) {
            Toast.makeText(this, "TEST SVG : lancement indisponible ("
                    + error.getClass().getSimpleName() + ")",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Android autorise sous certaines versions le lancement d'activité depuis
     * une application avec permission de superposition et fenêtre visible.
     * Ne tente jamais cela lorsque la bulle n'est pas effectivement affichée.
     * Renvoie false pour conserver la notification manuelle en repli.
     */
    static boolean tryAutoOpenFromNotification(Context context) {
        if (XtoolSessionManager.isActive(context)) return true;
        JarvisBubbleService current = activeService;
        if (current == null || current.bubble == null || !current.bubble.isAttachedToWindow()
                || !Settings.canDrawOverlays(context)) return false;
        try {
            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(MainActivity.ACTION_CHECK_GMAIL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(intent);
            return true;
        } catch (RuntimeException blocked) {
            return false;
        }
    }

    /** Cacher brièvement la bulle pendant la capture consentie, puis la restaurer. */
    static void prepareEasySetCapture() {
        JarvisBubbleService current = activeService;
        if (current == null) return;
        current.gestureHandler.post(() -> {
            if (current.bubble == null) return;
            current.dismissEasySetBanner();
            current.bubble.setVisibility(View.INVISIBLE);
            if (current.svgTestBubble != null)
                current.svgTestBubble.setVisible(false);
            // Failsafe: restore even if MediaProjection fails or the OCR process dies.
            current.gestureHandler.postDelayed(() -> {
                if (current.bubble != null) current.bubble.setVisibility(View.VISIBLE);
                if (current.svgTestBubble != null)
                    current.svgTestBubble.setVisible(true);
            }, 14000L);
        });
    }

    /** Revenir à l'affichage normal après une lecture ou un échec. */
    static void restoreEasySetBubble() {
        JarvisBubbleService current = activeService;
        if (current != null) current.gestureHandler.post(() -> {
            if (current.bubble != null) current.bubble.setVisibility(View.VISIBLE);
            if (current.svgTestBubble != null)
                current.svgTestBubble.setVisible(true);
        });
    }

    /** Afficher le résultat SUR xTool, sans revenir dans les réglages Jarvis. */
    static void showEasySetResult(String summary) {
        JarvisBubbleService current = activeService;
        if (current != null) current.gestureHandler.post(() ->
                current.showEasySetBanner(summary));
    }

    private void dismissEasySetBanner() {
        if (easySetBannerDismiss != null)
            gestureHandler.removeCallbacks(easySetBannerDismiss);
        easySetBannerDismiss = null;
        if (wm != null && easySetBanner != null) {
            try { wm.removeView(easySetBanner); }
            catch (IllegalArgumentException ignored) { }
        }
        easySetBanner = null;
    }

    private void showEasySetBanner(String summary) {
        if (bubble != null) bubble.setVisibility(View.VISIBLE);
        if (svgTestBubble != null) svgTestBubble.setVisible(true);
        if (wm == null || !Settings.canDrawOverlays(this)) return;
        dismissEasySetBanner();

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(13), dp(12), dp(13), dp(10));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(12, 30, 48));
        background.setCornerRadius(dp(14));
        background.setStroke(dp(2), Color.rgb(75, 211, 235));
        panel.setBackground(background);

        TextView title = new TextView(this);
        title.setText(summary != null && summary.startsWith("DIAGNOSTIC EASYSET")
                ? "JARVIS • DIAGNOSTIC EASYSET" : "JARVIS • RÉSULTAT EASYSET");
        title.setTextColor(Color.rgb(85, 221, 249));
        title.setTextSize(14);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        panel.addView(title);

        TextView details = new TextView(this);
        details.setText(summary == null ? "Résultat indisponible." : summary);
        details.setTextColor(Color.WHITE);
        final boolean detailedEasySet = summary != null
                && summary.contains("DIAGNOSTIC PASSER (temporaire)");
        final boolean connectionDiagnostic = summary != null
                && summary.startsWith("DIAGNOSTIC EASYSET");
        details.setTextSize(detailedEasySet || connectionDiagnostic ? 12 : 15);
        details.setPadding(0, dp(8), 0, dp(8));
        if (connectionDiagnostic) {
            // Five exact accessibility labels need more lines than before.
            // Keep the dialog and its dismiss button on-screen; allow the
            // diagnostic to scroll without moving xTool or the SVG bubble.
            ScrollView scroll = new ScrollView(this);
            scroll.setFillViewport(false);
            scroll.addView(details);
            int maxDiagnosticHeight = Math.min(dp(330),
                    getResources().getDisplayMetrics().heightPixels / 2);
            panel.addView(scroll, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, maxDiagnosticHeight));
        } else {
            panel.addView(details);
        }

        Button dismiss = new Button(this);
        dismiss.setAllCaps(false);
        dismiss.setText(connectionDiagnostic
                ? "Fermer le diagnostic" : "Fermer le résultat");
        panel.addView(dismiss);
        dismiss.setOnClickListener(v -> dismissEasySetBanner());

        Point display = new Point();
        wm.getDefaultDisplay().getRealSize(display);
        int width = Math.min(dp(310), display.x - dp(24));
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                width, WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.x = Math.max(dp(12), Math.min(params != null ? params.x : dp(12),
                display.x - width - dp(12)));
        lp.y = Math.max(dp(65), Math.min(params != null
                ? params.y + params.height + dp(8) : dp(65), display.y - dp(285)));
        if (detailedEasySet || connectionDiagnostic) {
            // Keep the temporary OCR trace and connection diagnostic inside the phone screen instead
            // of letting a taller banner hang below the navigation bar.
            panel.measure(
                    View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(
                            Math.max(dp(150), display.y - dp(90)),
                            View.MeasureSpec.AT_MOST));
            lp.y = Math.max(dp(56),
                    Math.min(lp.y, display.y - panel.getMeasuredHeight() - dp(16)));
        }
        try {
            wm.addView(panel, lp);
            easySetBanner = panel;
            easySetBannerDismiss = this::dismissEasySetBanner;
            gestureHandler.postDelayed(easySetBannerDismiss,
                    detailedEasySet || connectionDiagnostic ? 40000L : 20000L);
        } catch (RuntimeException ignored) {
            easySetBanner = null;
            Toast.makeText(this, summary, Toast.LENGTH_LONG).show();
        }
    }

    /** Events actually observed by Jarvis: no simulated progress stages. */
    private static void showStageAfterObservedEvent(int state, String title,
                                                    String detail) {
        JarvisBubbleService current = activeService;
        if (current == null) return;

        // Validate the old xTool-session gate NOW, not after a posted UI
        // callback: by then the SVG handoff may already have activated xTool.
        // Events accepted earlier can finish their 500-ms visual sequence,
        // but stale new Gmail events cannot overwrite an ongoing xTool task.
        if (XtoolSessionManager.isActive(current) && state != STATE_XTOOL)
            return;
        current.gestureHandler.post(() ->
                current.enqueueVisualStage(state, title, detail));
    }

    static void onGmailNotificationReceived() {
        showStageAfterObservedEvent(STATE_GMAIL, "GMAIL", "NOTIFICATION");
    }

    static void onSvgWorkStarted() {
        showStageAfterObservedEvent(STATE_WORKING, "RÉCEPTION", ".SVG");
    }

    static void onGmailWorkStarted() {
        showStageAfterObservedEvent(STATE_WORKING, "GMAIL", "RECHERCHE");
    }

    static void onSvgOpenRequested(boolean xToolTarget) {
        if (xToolTarget) {
            // This is a REQUEST to Android, not proof of successful import.
            showStageAfterObservedEvent(STATE_XTOOL, "xTool", "EN COURS");
        } else {
            showStageAfterObservedEvent(STATE_SUCCESS, "SVG", "ENVOYÉ");
        }
    }

    static void onFileRejected(JarvisFileRouter.Kind kind) {
        String title = kind == JarvisFileRouter.Kind.CAD_3D ? "CAD"
                : kind == JarvisFileRouter.Kind.DXF_2D ? "DXF" : "FICHIER";
        showStageAfterObservedEvent(STATE_ERROR, title, "REFUSÉ");
    }

    static void onOperationFailed() {
        showStageAfterObservedEvent(STATE_ERROR, "JARVIS", "ERREUR");
    }

    private void showActivity(int state, String title, String detail) {
        if (bubble == null || wm == null) return;
        // The manual menu must obey the same session protection as before.
        if (XtoolSessionManager.isActive(this) && state != STATE_XTOOL) return;
        enqueueVisualStage(state, title, detail);
    }

    private void enqueueVisualStage(int state, String title, String detail) {
        if (bubble == null || wm == null) return;
        VisualStage next = new VisualStage(state, title, detail);

        if (!animationsEnabled) {
            clearVisualStages();
            displayVisualStage(next);
            return;
        }

        // Ignore duplicate notifications/redraws for the same stage.
        VisualStage last = stageQueue.peekLast();
        if ((stageHolding && stageQueue.isEmpty()
                && visualState == state && visualTitle.equals(title)
                && visualDetail.equals(detail))
                || next.sameAs(last)) return;

        // Bound backlog if the system delivers repeated notifications.
        // Always retain the last event (e.g. xTool handoff), rather than
        // allowing a delayed animation queue to grow without limit.
        while (stageQueue.size() >= 7) stageQueue.removeFirst();
        stageQueue.addLast(next);
        if (!stageHolding) displayNextQueuedStage();
    }

    private void displayNextQueuedStage() {
        VisualStage next = stageQueue.pollFirst();
        if (next == null) {
            stageHolding = false;
            stageAdvance = null;
            if (bubble != null) bubble.invalidate();
            return;
        }
        boolean expanding = !expanded;
        stageHolding = true;
        displayVisualStage(next);
        long hold = BubbleVisualStageTiming.holdMillis(
                animationsEnabled, expanding);
        stageAdvance = this::displayNextQueuedStage;
        gestureHandler.postDelayed(stageAdvance, hold);
    }

    private void clearVisualStages() {
        if (stageAdvance != null) gestureHandler.removeCallbacks(stageAdvance);
        stageAdvance = null;
        stageQueue.clear();
        stageHolding = false;
    }

    private void displayVisualStage(VisualStage stage) {
        int state = stage.state;
        String title = stage.title, detail = stage.detail;
        boolean changed = visualState != state || !visualTitle.equals(title)
                || !visualDetail.equals(detail);
        visualState = state;
        visualTitle = title;
        visualDetail = detail;
        visualUntil = SystemClock.uptimeMillis() + IDLE_COLLAPSE_MS;
        if (changed && soundsEnabled) {
            int note = -1;
            if (state == STATE_GMAIL) note = JarvisXylophone.DO5;
            else if (state == STATE_WORKING && "GMAIL".equals(title))
                note = JarvisXylophone.RE5;
            else if (state == STATE_WORKING && "RÉCEPTION".equals(title))
                note = JarvisXylophone.MI5;
            else if (state == STATE_SUCCESS) note = JarvisXylophone.SOL5;
            if (note >= 0 && xylophone != null) xylophone.play(note);
        }
        if (state == STATE_ERROR && "REFUSÉ".equals(detail)
                && soundsEnabled && xylophone != null) {
            xylophone.playRefusal();
        }
        setExpanded(true);
        // During an expansion animator, setExpanded() can return early to
        // avoid restarting the animation. Explicitly redraw the new label.
        bubble.invalidate();
    }

    private void cancelCollapse() {
        if (collapseRunnable != null) {
            gestureHandler.removeCallbacks(collapseRunnable);
            collapseRunnable = null;
        }
    }

    private void scheduleCollapse() {
        cancelCollapse();
        if (!expanded || bubble == null || menuView != null) return;
        collapseRunnable = () -> {
            collapseRunnable = null;
            if (fingerDown || dragging || menuView != null) {
                scheduleCollapse();
            } else {
                setExpanded(false);
            }
        };
        gestureHandler.postDelayed(collapseRunnable, IDLE_COLLAPSE_MS);
    }

    private void cancelPendingLongPress() {
        if (pendingLongPress != null) {
            gestureHandler.removeCallbacks(pendingLongPress);
            pendingLongPress = null;
        }
    }

    private boolean handleTouch(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cancelCollapse();
                cancelPendingLongPress();
                downX = e.getRawX();
                downY = e.getRawY();
                startX = params.x;
                startY = params.y;
                fingerDown = true;
                dragging = false;
                longPressHandled = false;
                if (menuView != null) {
                    dismissMenu();
                    longPressHandled = true; // ce toucher sert seulement à fermer le menu
                    return true;
                }
                pendingLongPress = () -> {
                    pendingLongPress = null;
                    if (!fingerDown || dragging || menuView != null) return;
                    longPressHandled = true;
                    showMenu();
                };
                gestureHandler.postDelayed(pendingLongPress, LONG_PRESS_MS);
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                if (Math.hypot(dx, dy) > dp(8)) {
                    dragging = true;
                    cancelPendingLongPress();
                }
                if (dragging && !longPressHandled) {
                    params.x = startX + Math.round(dx);
                    params.y = startY + Math.round(dy);
                    clampPosition();
                    wm.updateViewLayout(bubble, params);
                }
                return true;

            case MotionEvent.ACTION_UP:
                cancelPendingLongPress();
                fingerDown = false;
                if (longPressHandled) {
                    longPressHandled = false;
                    dragging = false;
                    return true;
                }
                if (dragging) {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                            .putInt("x", params.x).putInt("y", params.y).apply();
                    scheduleCollapse();
                } else if (!expanded) {
                    if (soundsEnabled && xylophone != null) xylophone.playMiniTap();
                    // Les deux sons sont distincts : tic immédiat, whoosh
                    // après un très court délai synchronisé à l'ouverture.
                    setExpanded(true);
                } else {
                    if (soundsEnabled && xylophone != null) xylophone.playMiniTap();
                    openChatGPT();
                    setExpanded(false);
                }
                dragging = false;
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                cancelPendingLongPress();
                dragging = true; // ignore les gestes à plusieurs doigts
                return true;

            case MotionEvent.ACTION_CANCEL:
                cancelPendingLongPress();
                fingerDown = false;
                dragging = false;
                scheduleCollapse();
                return true;

            default: return false;
        }
    }

    private void launchEasySetSessionConsent() {
        if (EasySetSessionService.isActive()) return;
        Intent consent = new Intent(this, EasySetConsentActivity.class);
        consent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        try { startActivity(consent); }
        catch (RuntimeException error) {
            Toast.makeText(this, "Appuie sur la bulle pour autoriser la capture Android.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void showMenu() {
        if (wm == null || bubble == null || menuView != null) return;
        cancelCollapse();

        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(12), dp(10), dp(12), dp(12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(19, 34, 53));
        background.setCornerRadius(dp(16));
        background.setStroke(dp(2), Color.rgb(57, 209, 255));
        menu.setBackground(background);

        TextView title = new TextView(this);
        title.setText("JARVIS · Menu");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setPadding(dp(7), dp(4), dp(7), dp(8));
        menu.addView(title);

        if (XtoolSessionManager.isActive(this)) {
            addMenuButton(menu, "Terminer la session xTool", () -> {
                if (XtoolSessionManager.finish(this)) {
                    showActivity(STATE_SUCCESS, "JARVIS", "PRÊT");
                    Toast.makeText(this,
                            "Session terminée. Jarvis attend le prochain SVG.",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        // One-tap diagnostic showing exactly which Android prerequisite or
        // accessible xTool heading detection failed, without recording app data.
        addMenuButton(menu, "Diagnostic EasySet (connexion)", () ->
                showEasySetBanner(
                        ChatGptBubbleAccessibilityService.easySetConnectionDiagnostic()));

        addMenuButton(menu, "Voir les mots détectés (xTool)", () ->
                showEasySetBanner(
                        ChatGptBubbleAccessibilityService.xToolDiscoveredWordsDiagnostic()));

        if (EasySetSessionService.isActive()) {
            addMenuButton(menu, "Analyser EasySet maintenant", () ->
                    EasySetSessionService.requestScan(this, false));
            addMenuButton(menu, "Arrêter le partage d'écran", () ->
                    EasySetSessionService.requestStop(this));
        } else {
            addMenuButton(menu, "Autoriser la capture pour la session", this::launchEasySetSessionConsent);
        }

        // Manual fallback and visibility control for the independent SVG bubble.
        boolean testBubbleEnabled = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean("test_svg_bubble_enabled", true);
        addMenuButton(menu, testBubbleEnabled
                ? "Bulle TEST SVG : masquer" : "Bulle TEST SVG : afficher", () -> {
            boolean enable = !testBubbleEnabled;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean("test_svg_bubble_enabled", enable).apply();
            if (enable) showSvgTestBubble();
            else if (svgTestBubble != null) {
                svgTestBubble.destroy();
                svgTestBubble = null;
            }
        });
        addMenuButton(menu, "Envoyer SVG test (+1)", this::launchNumberedSvgTest);

        addMenuButton(menu, "Ouvrir Jarvis Bridge", () -> {
            Intent app = new Intent(this, MainActivity.class);
            // Nouvelle fenêtre complète : ne pas réutiliser l'Activity
            // transparente créée par le lancement compact de l'icône.
            app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(app);
        });
        addMenuButton(menu, animationsEnabled ? "Animations : activées" : "Animations : désactivées", () -> {
            animationsEnabled = !animationsEnabled;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean("animations", animationsEnabled).apply();
            bubble.invalidate();
        });
        addMenuButton(menu, soundsEnabled ? "Son : activé" : "Son : désactivé", () -> {
            soundsEnabled = !soundsEnabled;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean("sounds_enabled", soundsEnabled).apply();
        });
        addMenuButton(menu, "Fermer la bulle", () -> {
            EasySetSessionService.requestStop(this);
            stopSelf();
        });
        addMenuButton(menu, "Annuler", () -> { });

        int menuWidth = dp(228);
        WindowManager.LayoutParams options = new WindowManager.LayoutParams(
                menuWidth,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        options.gravity = Gravity.TOP | Gravity.LEFT;
        Point display = new Point();
        wm.getDefaultDisplay().getRealSize(display);
        int left = params.x - menuWidth - dp(8);
        if (left < 0) left = params.x + params.width + dp(8);
        options.x = Math.max(0, Math.min(left, display.x - menuWidth));
        options.y = Math.max(0, Math.min(params.y, display.y - dp(455)));

        menuView = menu;
        try {
            wm.addView(menu, options);
        } catch (Exception error) {
            menuView = null;
            Toast.makeText(this, "Menu Jarvis indisponible : " + error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void addMenuButton(LinearLayout menu, String text, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        menu.addView(button, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        button.setOnClickListener(v -> {
            // Le son confirme le choix avant l'exécution (même « Son : désactivé »).
            if (soundsEnabled && xylophone != null) xylophone.playMenuSelect();
            dismissMenu();
            action.run();
        });
    }

    private void dismissMenu() {
        if (wm != null && menuView != null) {
            try { wm.removeView(menuView); }
            catch (IllegalArgumentException ignored) { }
        }
        menuView = null;
        if (bubble != null) scheduleCollapse();
    }

    /**
     * Ouverture dynamique : agrandissement rapide, léger rebond et halo rotatif.
     * Le dessin final de la bulle, les messages et les gestes ne changent pas.
     * Le réglage existant « Animations » permet de garder l'ouverture instantanée.
     */
    private void setExpanded(boolean next) {
        if (bubble == null || wm == null) return;

        // Plusieurs événements Gmail peuvent arriver pendant l'ouverture :
        // ne jamais relancer l'animation ni couper l'agrandissement en cours.
        if (next && expanded && openingAnimator != null) {
            scheduleCollapse();
            return;
        }

        if (openingAnimator != null) {
            openingAnimator.cancel();
            openingAnimator = null;
        }

        boolean opening = next && !expanded;
        expanded = next;
        int side = dp(next ? LARGE_DP : SMALL_DP);

        // Rotation lumineuse brève sur le cadre existant, indépendante des
        // animations de réception, y compris pendant une session xTool.
        openingSpinUntil = opening && animationsEnabled
                ? SystemClock.uptimeMillis() + OPENING_SPIN_MS : 0L;

        if (opening && soundsEnabled && xylophone != null) {
            // Une ouverture automatique joue uniquement le whoosh ;
            // une pression sur la miniature joue d'abord son tic propre.
            gestureHandler.postDelayed(() -> {
                if (expanded && soundsEnabled && xylophone != null)
                    xylophone.playOpen();
            }, 85L);
        }

        if (opening && animationsEnabled && params.width < side) {
            // 54 -> 110 -> 103 -> 110 dp : un rebond net sans jamais
            // dépasser la fenêtre cible ni modifier sa forme finale.
            ValueAnimator animator = ValueAnimator.ofInt(
                    params.width, side, dp(103), side);
            animator.setDuration(BubbleVisualStageTiming.OPENING_ANIMATION_MS);
            animator.setInterpolator(new DecelerateInterpolator(1.0f));
            openingAnimator = animator;
            animator.addUpdateListener(frame -> {
                if (openingAnimator != animator || bubble == null || wm == null) return;
                int current = (Integer) frame.getAnimatedValue();
                params.width = current;
                params.height = current;
                clampPosition();
                try {
                    wm.updateViewLayout(bubble, params);
                    bubble.invalidate();
                } catch (IllegalArgumentException ignored) {
                    // La fenêtre a pu être retirée lors de l'arrêt de Jarvis.
                    animator.cancel();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) {
                    if (openingAnimator == animator) openingAnimator = null;
                }
            });
            animator.start();
        } else {
            params.width = side;
            params.height = side;
            clampPosition();
            bubble.invalidate();
            wm.updateViewLayout(bubble, params);
        }

        if (next) scheduleCollapse();
        else {
            cancelCollapse();
            if (!XtoolSessionManager.isActive(this)) {
                visualState = STATE_READY;
                visualUntil = 0L;
            }
        }
    }

    private void openChatGPT() {
        if (!JarvisConversationLink.open(this)) {
            Toast.makeText(this, "Impossible d'ouvrir ChatGPT.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onDestroy() {
        EasySetSessionService.requestStop(this);
        if (activeService == this) activeService = null;
        if (openingAnimator != null) {
            openingAnimator.cancel();
            openingAnimator = null;
        }
        clearVisualStages();
        openingSpinUntil = 0L;
        if (xylophone != null) {
            xylophone.release();
            xylophone = null;
        }
        cancelCollapse();
        cancelPendingLongPress();
        fingerDown = false;
        dismissMenu();
        dismissEasySetBanner();
        if (svgTestBubble != null) {
            svgTestBubble.destroy();
            svgTestBubble = null;
        }
        if (wm != null && bubble != null) {
            try { wm.removeView(bubble); }
            catch (IllegalArgumentException ignored) { }
        }
        bubble = null;
        stopForeground(true);
        super.onDestroy();
    }

    /** Au repos, afficher seulement l'heure. Pendant une action réelle, animer le contour. */
    private final class BubbleView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Path orbit = new Path();
        private final PathMeasure orbitMeasure = new PathMeasure();
        private final float[] orbitPoint = new float[2];
        private final SimpleDateFormat clockFormat =
                new SimpleDateFormat("HH:mm", Locale.CANADA_FRENCH);

        BubbleView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        private void drawCentered(Canvas canvas, String value, float x, float y,
                                  int size, int color, boolean bold) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(color);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(dp(size));
            p.setFakeBoldText(bold);
            canvas.drawText(value, x, y, p);
            p.setFakeBoldText(false);
        }

        private int getAccent(int state) {
            switch (state) {
                case STATE_GMAIL: return Color.rgb(255, 190, 65);
                case STATE_WORKING: return Color.rgb(55, 220, 255);
                case STATE_SUCCESS: return Color.rgb(80, 235, 150);
                case STATE_ERROR: return Color.rgb(255, 100, 110);
                case STATE_XTOOL: return Color.rgb(255, 193, 80);
                default: return Color.rgb(55, 208, 245);
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            if (w < 1 || h < 1) return;
            long now = SystemClock.uptimeMillis();
            // Previously accepted Gmail/SVG labels must remain readable
            // for 500 ms even if xTool was launched immediately afterwards.
            boolean temporaryLabel = stageHolding
                    && visualState != STATE_XTOOL && visualState != STATE_READY;
            boolean xtoolActive = XtoolSessionManager.isActive(
                    JarvisBubbleService.this) && !temporaryLabel;
            // Ne montrer le texte grand format qu'une fois suffisamment de
            // place disponible pendant l'animation d'agrandissement.
            boolean expandedLayout = expanded && w >= dp(96);
            boolean eventVisible = expandedLayout && visualState != STATE_READY
                    && now < visualUntil;
            // Le message xTool reste visible jusqu'à la fin de session.
            // Aucune animation permanente coûteuse durant le travail.
            boolean openingSpin = animationsEnabled && expanded
                    && now < openingSpinUntil && isAttachedToWindow();
            boolean animating = eventVisible && !xtoolActive
                    && animationsEnabled && isAttachedToWindow();
            int color = getAccent(xtoolActive
                    ? STATE_XTOOL : eventVisible ? visualState : STATE_READY);
            float inset = dp(expandedLayout ? 7 : 5);
            float rounded = Math.min(w, h) * .23f;
            rect.set(inset, inset, w - inset, h - inset);

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(230, 10, 25, 45));
            canvas.drawRoundRect(rect, rounded, rounded, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(expandedLayout ? 2.5f : 2f));
            p.setColor(color);
            canvas.drawRoundRect(rect, rounded, rounded, p);

            float gap = inset + dp(expandedLayout ? 6 : 5);
            RectF inside = new RectF(gap, gap, w - gap, h - gap);
            p.setStrokeWidth(dp(1));
            p.setColor(Color.argb(155, 118, 225, 244));
            canvas.drawRoundRect(inside, rounded * .76f, rounded * .76f, p);

            if (openingSpin || animating) {
                orbit.reset();
                if (openingSpin) {
                    // Déplacer uniquement la trajectoire d'ouverture vers
                    // l'intérieur : un halo de rayon 12 dp reste entièrement
                    // visible, même aux coins et durant le grossissement.
                    float safeInset = dp(15);
                    RectF safeOrbit = new RectF(
                            safeInset, safeInset, w - safeInset, h - safeInset);
                    float safeRadius = Math.max(0f, rounded - dp(8));
                    orbit.addRoundRect(safeOrbit, safeRadius, safeRadius,
                            Path.Direction.CW);
                } else {
                    // Animation de réception SVG inchangée.
                    orbit.addRoundRect(rect, rounded, rounded, Path.Direction.CW);
                }
                orbitMeasure.setPath(orbit, false);
                float progress = openingSpin
                        ? (OPENING_SPIN_MS - (openingSpinUntil - now))
                                / (float) OPENING_SPIN_MS
                        : (now % 1900L) / 1900f;
                float length = orbitMeasure.getLength();
                float distance = progress * length; // une rotation, sans répétition
                if (openingSpin && length > 0f) {
                    // Trois touches de lumière derrière le point, dessinées
                    // avant le halo et uniquement pendant son ouverture.
                    for (int tail = 3; tail >= 1; tail--) {
                        float previous = (distance - dp(tail * 6f) + length) % length;
                        if (orbitMeasure.getPosTan(previous, orbitPoint, null)) {
                            p.setStyle(Paint.Style.FILL);
                            p.setColor(Color.argb(36 + (4 - tail) * 19,
                                    Color.red(color), Color.green(color),
                                    Color.blue(color)));
                            canvas.drawCircle(orbitPoint[0], orbitPoint[1],
                                    dp(2.5f + (4 - tail) * 1.0f), p);
                        }
                    }
                }
                if (orbitMeasure.getPosTan(Math.min(distance, length),
                        orbitPoint, null)) {
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(Color.argb(openingSpin ? 145 : 95,
                            Color.red(color), Color.green(color), Color.blue(color)));
                    canvas.drawCircle(orbitPoint[0], orbitPoint[1],
                            dp(openingSpin ? 12 : 7), p);
                    p.setColor(Color.WHITE);
                    canvas.drawCircle(orbitPoint[0], orbitPoint[1],
                            dp(openingSpin ? 3.2f : 2.6f), p);
                }
            }

            if (openingSpin) {
                // Flash discret sur le cadre uniquement : l'apparence
                // permanente, le texte et la palette restent inchangés.
                float age = 1f - (openingSpinUntil - now) / (float) OPENING_SPIN_MS;
                float flash = Math.max(0f, 1f - Math.abs(age - 0.62f) / 0.26f);
                if (flash > 0f) {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(dp(3));
                    p.setColor(Color.argb((int) (105 * flash),
                            Color.red(color), Color.green(color), Color.blue(color)));
                    canvas.drawRoundRect(rect, rounded, rounded, p);
                }
            }

            if (expandedLayout) {
                String title = xtoolActive ? "xTool"
                        : eventVisible ? visualTitle : "JARVIS";
                String detail = xtoolActive ? "EN COURS"
                        : eventVisible ? visualDetail : "PRÊT";
                drawCentered(canvas, title, w / 2f, h * .44f, 13,
                        xtoolActive || eventVisible ? color : Color.WHITE, true);
                drawCentered(canvas, detail, w / 2f, h * .66f, 12,
                        Color.rgb(191, 234, 245), true);
            } else if (xtoolActive) {
                // Même repliée à 54 dp, la bulle annonce la session active.
                drawCentered(canvas, "xTool", w / 2f, h * .46f, 9,
                        color, true);
                drawCentered(canvas, "EN COURS", w / 2f, h * .66f, 8,
                        Color.rgb(226, 250, 255), true);
            } else {
                // En dehors de xTool, la petite bulle conserve l'heure.
                String clock = clockFormat.format(new Date());
                drawCentered(canvas, clock, w / 2f, h * .56f, 13,
                        Color.rgb(226, 250, 255), true);
            }

            if (openingSpin) {
                postInvalidateDelayed(16L);
            } else if (animating) {
                postInvalidateDelayed(85L);
            } else {
                // Au repos, ne rafraîchir qu'au changement de minute.
                long millis = System.currentTimeMillis();
                postInvalidateDelayed(Math.max(100L, 60025L - millis % 60000L));
            }
        }
    }
}
