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

/** Bulle Jarvis 0.8 : toucher pour agrandir puis ouvrir ChatGPT ; glisser pour déplacer. */
public final class JarvisBubbleService extends Service {
    public static final String ACTION_STOP = "ca.jarvis.bridge.STOP_BUBBLE";
    private static final String CHANNEL = "jarvis_bubble_foreground";
    private static final int NOTIFICATION_ID = 808;
    private static final String PREFS = "jarvis_bubble";
    private static final int SMALL_DP = 54;
    private static final int LARGE_DP = 110;
    private static final long LONG_PRESS_MS = 2000L;

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
        params.x = prefs.getInt("x", Math.max(0, display.x - dp(80)));
        params.y = prefs.getInt("y", Math.max(dp(100), display.y / 3));
        clampPosition();
        bubble = new BubbleView(this);
        bubble.setOnTouchListener((v, event) -> handleTouch(event));
        wm.addView(bubble, params);
    }

    private void clampPosition() {
        Point display = new Point();
        wm.getDefaultDisplay().getRealSize(display);
        params.x = Math.max(0, Math.min(params.x, display.x - params.width));
        params.y = Math.max(0, Math.min(params.y, display.y - params.height));
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
                return true;

            default: return false;
        }
    }

    private void showMenu() {
        if (wm == null || bubble == null || menuView != null) return;

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
        options.y = Math.max(0, Math.min(params.y, display.y - dp(220)));

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
    }

    private void setExpanded(boolean next) {
        expanded = next;
        int side = dp(next ? LARGE_DP : SMALL_DP);
        params.width = side;
        params.height = side;
        clampPosition();
        bubble.invalidate();
        wm.updateViewLayout(bubble, params);
    }

    private void openChatGPT() {
        if (!JarvisConversationLink.open(this)) {
            Toast.makeText(this, "Impossible d'ouvrir ChatGPT.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onDestroy() {
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

    /** Façade provisoire, centre libre pour les informations futures. */
    private final class BubbleView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        BubbleView(Context ctx) {
            super(ctx);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        @Override protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), s = Math.min(w, h);
            float inset = dp(expanded ? 9 : 6);
            r.set(inset, inset, w - inset, h - inset);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.argb(200, 12, 28, 54));
            canvas.drawRoundRect(r, s * .26f, s * .26f, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(expanded ? 3 : 2));
            p.setColor(Color.rgb(57, 209, 255));
            canvas.drawRoundRect(r, s * .26f, s * .26f, p);
            float inner = inset + dp(6);
            r.set(inner, inner, w - inner, h - inner);
            p.setStrokeWidth(dp(1.2f));
            p.setColor(Color.argb(180, 130, 240, 255));
            canvas.drawRoundRect(r, s * .20f, s * .20f, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(88, 244, 228));
            canvas.drawCircle(w / 2f, h - inset - dp(4), dp(expanded ? 4 : 2), p);
            if (expanded) {
                p.setColor(Color.WHITE);
                p.setTextAlign(Paint.Align.CENTER);
                p.setTextSize(dp(12));
                p.setFakeBoldText(true);
                canvas.drawText("JARVIS", w/2f, h/2f + dp(4), p);
                p.setFakeBoldText(false);
            }
        }
    }
}
