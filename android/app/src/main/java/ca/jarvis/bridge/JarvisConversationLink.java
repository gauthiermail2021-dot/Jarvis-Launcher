package ca.jarvis.bridge;

import android.content.Context;
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

    static boolean open(Context context) {
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
                context.startActivity(app);
                return true;
            } catch (Exception ignored) { }

            Intent browser = new Intent(Intent.ACTION_VIEW, uri);
            browser.addCategory(Intent.CATEGORY_BROWSABLE);
            browser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(browser);
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
            context.startActivity(fallback);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
