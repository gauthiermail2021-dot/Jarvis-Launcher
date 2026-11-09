package ca.jarvis.bridge;

import android.app.ActivityOptions;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.WindowManager;
import android.content.Intent;
import android.net.Uri;
import java.util.List;

/** Ouvre un lien de conversation privée ChatGPT stocké localement. */
final class JarvisConversationLink {
    static final String PREFS = "jarvis_conversation_preferences";
    static final String KEY_URL = "conversation_url";

    private JarvisConversationLink() { }

    /** Refuse les liens partagés (/share/) et les autres domaines. */
    static String normalize(String input) {
        if (input == null) return null;
        try {
            Uri uri = Uri.parse(input.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())) return null;
            String host = uri.getHost();
            if (!"chatgpt.com".equalsIgnoreCase(host)
                    && !"chat.openai.com".equalsIgnoreCase(host)) return null;
            List<String> segments = uri.getPathSegments();
            if (segments.size() != 2 || !"c".equals(segments.get(0))) return null;
            String conversationId = segments.get(1);
            if (!conversationId.matches("[A-Za-z0-9-]{10,128}")) return null;
            return "https://chatgpt.com/c/" + conversationId;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    /**
     * Demande un lancement dans une fenêtre plus petite. Android/Samsung peut
     * ignorer setLaunchBounds hors du mode multi-fenêtre : ce n'est pas une
     * permission de forcer une application tierce en fenêtre flottante.
     * Aucun accès au texte ni à l'interface interne de ChatGPT.
     */
    static boolean openPopup(Context context) {
        android.os.Bundle launchOptions = null;
        if (Build.VERSION.SDK_INT >= 26) {
            try {
                WindowManager manager = (WindowManager)
                        context.getSystemService(Context.WINDOW_SERVICE);
                Point screen = new Point();
                if (manager != null) manager.getDefaultDisplay().getRealSize(screen);
                if (screen.x > 0 && screen.y > 0) {
                    int w = Math.round(screen.x * 0.82f);
                    int h = Math.round(screen.y * 0.64f);
                    int left = Math.max(0, (screen.x - w) / 2);
                    int top = Math.max(0, (screen.y - h) / 4);
                    launchOptions = ActivityOptions.makeBasic()
                            .setLaunchBounds(new Rect(left, top, left + w, top + h))
                            .toBundle();
                }
            } catch (RuntimeException ignored) {
                // Repli vers le lancement normal.
            }
        }
        return openWithOptions(context, launchOptions);
    }

    static boolean open(Context context) {
        return openWithOptions(context, null);
    }

    private static void start(Context context, Intent intent,
                              android.os.Bundle options) {
        if (options != null) context.startActivity(intent, options);
        else context.startActivity(intent);
    }

    private static boolean openWithOptions(Context context, android.os.Bundle options) {
        String stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_URL, "");
        String link = normalize(stored);
        if (link != null) {
            Uri uri = Uri.parse(link);
            Intent app = new Intent(Intent.ACTION_VIEW, uri);
            app.addCategory(Intent.CATEGORY_BROWSABLE);
            app.setPackage("com.openai.chatgpt");
            app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                start(context, app, options);
                return true;
            } catch (Exception ignored) { }

            Intent browser = new Intent(Intent.ACTION_VIEW, uri);
            browser.addCategory(Intent.CATEGORY_BROWSABLE);
            browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                start(context, browser, options);
                return true;
            } catch (Exception ignored) { }
        }

        Intent fallback = context.getPackageManager()
                .getLaunchIntentForPackage("com.openai.chatgpt");
        if (fallback == null) {
            fallback = new Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/"));
        }
        fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            start(context, fallback, options);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
