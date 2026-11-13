package ca.jarvis.bridge;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Session xTool conservée entre les ouvertures de l'application.
 *
 * Une session commence uniquement après la demande d'ouverture d'un SVG
 * dans xTool. Elle ne se termine jamais sur un déplacement de la bulle,
 * un changement d'application ou une notification Gmail.
 */
final class XtoolSessionManager {
    private static final Object LOCK = new Object();
    private static final String PREFS = "jarvis_bridge";
    private static final String SESSION_ACTIVE = "xtool_session_active";
    private static final String SESSION_STARTED_AT = "xtool_session_started_at";
    private static final String LAST_GMAIL_TIMESTAMP = "last_gmail_timestamp";

    private XtoolSessionManager() { }

    static boolean isActive(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(SESSION_ACTIVE, false);
    }

    /** Token of the current protected handoff. A stale watcher must not
     * close a newer SVG session. Zero means no current session token. */
    static long startedAt(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!prefs.getBoolean(SESSION_ACTIVE, false)) return 0L;
        return prefs.getLong(SESSION_STARTED_AT, 0L);
    }

    /** Close only the exact observed session; a newer SVG is never affected. */
    static boolean finishIfStartedAt(Context context, long expectedStart) {
        synchronized (LOCK) {
            SharedPreferences prefs =
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (expectedStart <= 0L || !prefs.getBoolean(SESSION_ACTIVE, false)
                    || prefs.getLong(SESSION_STARTED_AT, 0L) != expectedStart)
                return false;
            return finish(context);
        }
    }

    static void begin(Context context) {
        synchronized (LOCK) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putBoolean(SESSION_ACTIVE, true)
                    .putLong(SESSION_STARTED_AT, System.currentTimeMillis())
                    .apply();
        }
    }

    /**
     * Réarme uniquement les futurs messages. Tous les courriels arrivés
     * pendant la session restent ignorés après sa fermeture.
     * Le dernier curseur Gmail ne recule jamais.
     */
    static boolean finish(Context context) {
        synchronized (LOCK) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (!prefs.getBoolean(SESSION_ACTIVE, false)) return false;
            long cutoff = skipQueuedMailUntil(
                    prefs.getLong(LAST_GMAIL_TIMESTAMP, 0L), System.currentTimeMillis());
            prefs.edit()
                    .putBoolean(SESSION_ACTIVE, false)
                    .putLong(LAST_GMAIL_TIMESTAMP, cutoff)
                    .apply();
            return true;
        }
    }

    static long skipQueuedMailUntil(long previousTimestamp, long finishedAt) {
        return Math.max(previousTimestamp, finishedAt);
    }
}
