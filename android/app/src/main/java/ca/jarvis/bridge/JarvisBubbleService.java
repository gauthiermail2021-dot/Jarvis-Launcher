package ca.jarvis.bridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.SystemClock;
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
import android.widget.Toast;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Bulle Jarvis 1.0 : informations locales, signaux lumineux et réduction automatique. */
public final class JarvisBubbleService extends Service {
    public static final String ACTION_STOP = "ca.jarvis.bridge.STOP_BUBBLE";
    private static final String CHANNEL = "jarvis_bubble_foreground";
    private static final int NOTIFICATION_ID = 808;
    private static final String PREFS = "jarvis_bubble";
    private static final int SMALL_DP = 54;
    private static final int LARGE_DP = 110;
    private static final long LONG_PRESS_MS = 1500L;
    private static final long IDLE_COLLAPSE_MS = 5000L;
    private static volatile JarvisBubbleService activeService;

    private WindowManager wm;
    private WindowManager.LayoutParams params;
    private BubbleView bubble;
    private boolean expanded = false;
    private boolean dragging = false;
    private float downX, downY;
    private int startX, startY;
    private final Handler gestureHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingLongPress;
    private boolean fingerDown = false;
    private boolean longPressHandled = false;
    private View menuView;
    private Runnable collapseRunnable;
    private boolean animationsEnabled = true;
    private static final int STATE_READY = 0;
    private static final int STATE_GMAIL = 1;
    private static final int STATE_WORKING = 2;
    private static final int STATE_SUCCESS = 3;
    private static final int STATE_ERROR = 4;
    private int visualState = STATE_READY;
    private String visualTitle = "JARVIS";
    private String visualDetail = "PRÊT";
    private long visualUntil = 0L;

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
        createNotification();
        if (bubble == null) {
            try { showBubble(); }
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
        params.x = prefs.getInt("x", Math.max(0, display.x - dp(80)));
        params.y = prefs.getInt("y", Math.max(dp(100), display.y / 3));
        clampPosition();
        bubble = new BubbleView(this);
        bubble.setOnTouchListener((v, event) -> handleTouch(event));
        wm.addView(bubble, params);
        activeService = this;
    }

    private void clampPosition() {
        Point display = new Point();
        wm.getDefaultDisplay().getRealSize(display);
        params.x = Math.max(0, Math.min(params.x, display.x - params.width));
        params.y = Math.max(0, Math.min(params.y, display.y - params.height));
    }


    /** Signaux exclusivement liés à des événements que Jarvis Bridge peut observer. */
    static void onGmailNotificationReceived() {
        JarvisBubbleService current = activeService;
        if (current != null) current.gestureHandler.post(() ->
                current.showActivity(STATE_GMAIL, "GMAIL", "NOTIFICATION"));
    }

    static void onSvgWorkStarted() {
        JarvisBubbleService current = activeService;
        if (current != null) current.gestureHandler.post(() ->
                current.showActivity(STATE_WORKING, "SVG", "TRAITEMENT"));
    }

    static void onGmailWorkStarted() {
        JarvisBubbleService current = activeService;
        if (current != null) current.gestureHandler.post(() ->
                current.showActivity(STATE_WORKING, "GMAIL", "RECHERCHE"));
    }

    static void onSvgOpenRequested() {
        JarvisBubbleService current = activeService;
        if (current != null) current.gestureHandler.post(() ->
                current.showActivity(STATE_SUCCESS, "SVG", "ENVOYÉ"));
    }

    static void onOperationFailed() {
        JarvisBubbleService current = activeService;
        if (current != null) current.gestureHandler.post(() ->
                current.showActivity(STATE_ERROR, "JARVIS", "ERREUR"));
    }

    private void showActivity(int state, String title, String detail) {
        if (bubble == null || wm == null) return;
        visualState = state;
        visualTitle = title;
        visualDetail = detail;
        visualUntil = SystemClock.uptimeMillis() + IDLE_COLLAPSE_MS;
        setExpanded(true);
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
                    setExpanded(true);
                } else {
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

        addMenuButton(menu, "Ouvrir Jarvis Bridge", () -> {
            Intent app = new Intent(this, MainActivity.class);
            app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(app);
        });
        addMenuButton(menu, animationsEnabled ? "Animations : activées" : "Animations : désactivées", () -> {
            animationsEnabled = !animationsEnabled;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putBoolean("animations", animationsEnabled).apply();
            bubble.invalidate();
        });
        addMenuButton(menu, "Fermer la bulle", this::stopSelf);
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
        options.y = Math.max(0, Math.min(params.y, display.y - dp(285)));

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

    private void setExpanded(boolean next) {
        if (bubble == null || wm == null) return;
        expanded = next;
        int side = dp(next ? LARGE_DP : SMALL_DP);
        params.width = side;
        params.height = side;
        clampPosition();
        bubble.invalidate();
        wm.updateViewLayout(bubble, params);
        if (next) scheduleCollapse();
        else {
            cancelCollapse();
            visualState = STATE_READY;
            visualUntil = 0L;
        }
    }

    private void openChatGPT() {
        if (!JarvisConversationLink.open(this)) {
            Toast.makeText(this, "Impossible d'ouvrir ChatGPT.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onDestroy() {
        if (activeService == this) activeService = null;
        cancelCollapse();
        cancelPendingLongPress();
        fingerDown = false;
        dismissMenu();
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
                default: return Color.rgb(55, 208, 245);
            }
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth(), h = getHeight();
            if (w < 1 || h < 1) return;
            long now = SystemClock.uptimeMillis();
            boolean eventVisible = expanded && visualState != STATE_READY
                    && now < visualUntil;
            boolean animating = eventVisible && animationsEnabled && isAttachedToWindow();
            int color = getAccent(eventVisible ? visualState : STATE_READY);
            float inset = dp(expanded ? 7 : 5);
            float rounded = Math.min(w, h) * .23f;
            rect.set(inset, inset, w - inset, h - inset);

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(230, 10, 25, 45));
            canvas.drawRoundRect(rect, rounded, rounded, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(expanded ? 2.5f : 2f));
            p.setColor(color);
            canvas.drawRoundRect(rect, rounded, rounded, p);

            float gap = inset + dp(expanded ? 6 : 5);
            RectF inside = new RectF(gap, gap, w - gap, h - gap);
            p.setStrokeWidth(dp(1));
            p.setColor(Color.argb(155, 118, 225, 244));
            canvas.drawRoundRect(inside, rounded * .76f, rounded * .76f, p);

            if (animating) {
                // Une lumière parcourt le contour uniquement durant une activité réelle.
                orbit.reset();
                orbit.addRoundRect(rect, rounded, rounded, Path.Direction.CW);
                orbitMeasure.setPath(orbit, false);
                float distance = (now % 1900L) / 1900f * orbitMeasure.getLength();
                if (orbitMeasure.getPosTan(distance, orbitPoint, null)) {
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(Color.argb(95, Color.red(color),
                            Color.green(color), Color.blue(color)));
                    canvas.drawCircle(orbitPoint[0], orbitPoint[1], dp(7), p);
                    p.setColor(Color.WHITE);
                    canvas.drawCircle(orbitPoint[0], orbitPoint[1], dp(2.6f), p);
                }
            }

            String clock = clockFormat.format(new Date());
            if (expanded) {
                String title = eventVisible ? visualTitle : "JARVIS";
                String detail = eventVisible ? visualDetail : "PRÊT";
                drawCentered(canvas, title, w / 2f, h * .36f, 12,
                        eventVisible ? color : Color.WHITE, true);
                drawCentered(canvas, detail, w / 2f, h * .56f, 11,
                        Color.rgb(191, 234, 245), true);
                drawCentered(canvas, clock, w / 2f, h * .76f, 15,
                        Color.rgb(155, 235, 255), false);
            } else {
                // Pas de J, pas de batterie : seulement l'heure, même en mode réduit.
                drawCentered(canvas, clock, w / 2f, h * .56f, 13,
                        Color.rgb(226, 250, 255), true);
            }

            if (animating) {
                postInvalidateDelayed(85L);
            } else {
                // Au repos, ne rafraîchir qu'au changement de minute.
                long millis = System.currentTimeMillis();
                postInvalidateDelayed(Math.max(100L, 60025L - millis % 60000L));
            }
        }
    }
}
