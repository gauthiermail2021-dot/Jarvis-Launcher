package ca.jarvis.bridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Independent movable 48 dp SVG bubble on top of apps.
 *
 * This component is hosted by JarvisBubbleService and uses its already
 * authorized SYSTEM_ALERT_WINDOW permission. It never starts a laser:
 * a deliberate tap forwards to the existing versioned SVG test sender.
 * Dragging moves the overlay; it never sends a test.
 */
final class JarvisTestBubbleOverlay {
    private static final String PREFS = "jarvis_bubble";
    private static final int DIAMETER_DP = 48;

    private final Context context;
    private final WindowManager windowManager;
    private final Runnable onTap;
    private final WindowManager.LayoutParams layout;
    private TextView bubble;

    private float downRawX, downRawY;
    private int downLeft, downTop;
    private boolean moved;
    private long lastTapElapsed;

    JarvisTestBubbleOverlay(Context context, WindowManager manager,
                            int defaultX, int defaultY, Runnable onTap) {
        this.context = context;
        this.windowManager = manager;
        this.onTap = onTap;
        int size = dp(DIAMETER_DP);
        layout = new WindowManager.LayoutParams(
                size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        layout.gravity = Gravity.TOP | Gravity.LEFT;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        layout.x = prefs.getInt("test_svg_x", defaultX);
        layout.y = prefs.getInt("test_svg_y", defaultY);
        clamp();
    }

    void show() {
        if (bubble != null) return;
        TextView view = new TextView(context);
        view.setText("SVG");
        view.setTextColor(Color.WHITE);
        view.setTextSize(13);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setContentDescription("TEST SVG JARVIS : envoyer un nouveau SVG numerote");
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(Color.rgb(15, 35, 58));
        bg.setStroke(dp(2), Color.rgb(52, 214, 244));
        view.setBackground(bg);
        view.setElevation(dp(4));
        view.setOnTouchListener(this::handleTouch);
        windowManager.addView(view, layout);
        bubble = view;
    }

    void setVisible(boolean visible) {
        if (bubble != null)
            bubble.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
    }

    void destroy() {
        if (bubble == null) return;
        try {
            windowManager.removeView(bubble);
        } catch (IllegalArgumentException ignored) {
            // The service/window may have already been torn down by Android.
        }
        bubble = null;
    }

    private boolean handleTouch(View target, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                downLeft = layout.x;
                downTop = layout.y;
                moved = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!moved && Math.hypot(dx, dy) > dp(14)) moved = true;
                if (moved) {
                    layout.x = downLeft + Math.round(dx);
                    layout.y = downTop + Math.round(dy);
                    clamp();
                    try { windowManager.updateViewLayout(target, layout); }
                    catch (IllegalArgumentException ignored) { }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (moved) {
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit()
                            .putInt("test_svg_x", layout.x)
                            .putInt("test_svg_y", layout.y)
                            .apply();
                } else if (SystemClock.elapsedRealtime() - lastTapElapsed > 1200L) {
                    lastTapElapsed = SystemClock.elapsedRealtime();
                    // Immediate visual confirmation that Android accepted
                    // this tap. Gmail success is still reported separately.
                    target.setAlpha(0.65f);
                    target.postDelayed(() -> {
                        if (bubble == target) target.setAlpha(1f);
                    }, 550L);
                    onTap.run();
                }
                moved = false;
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                // A multitouch gesture is never a deliberate one-tap send.
                moved = true;
                return true;
            case MotionEvent.ACTION_CANCEL:
                moved = false;
                return true;
            default:
                return false;
        }
    }

    private void clamp() {
        Point screen = new Point();
        windowManager.getDefaultDisplay().getRealSize(screen);
        layout.x = Math.max(0, Math.min(layout.x, screen.x - layout.width));
        layout.y = Math.max(0, Math.min(layout.y, screen.y - layout.height));
    }

    private int dp(float value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
