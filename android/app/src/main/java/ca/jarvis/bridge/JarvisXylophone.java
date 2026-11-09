package ca.jarvis.bridge;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Lit des échantillons WAV préchargés plutôt que de créer un AudioTrack à
 * chaque événement. Initialiser et libérer avec le cycle de vie de la bulle.
 */
final class JarvisXylophone {
    static final int DO5 = 0, RE5 = 1, MI5 = 2, SOL5 = 3;
    private static final String TAG = "JarvisXylophone";
    private static final int[] WAV_RESOURCES = {
            R.raw.jarvis_do5, R.raw.jarvis_re5,
            R.raw.jarvis_mi5, R.raw.jarvis_sol5
    };

    private final Context context;
    private final SoundPool pool;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int[] soundIds = new int[4];
    private final boolean[] ready = new boolean[4];
    private final boolean[] queued = new boolean[4];
    private boolean released;

    JarvisXylophone(Context context) {
        this.context = context.getApplicationContext();
        pool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        pool.setOnLoadCompleteListener((soundPool, sampleId, status) ->
                handler.post(() -> {
                    if (released) return;
                    for (int i = 0; i < soundIds.length; i++) {
                        if (soundIds[i] != sampleId) continue;
                        ready[i] = status == 0;
                        if (status != 0) Log.w(TAG, "WAV load failed: " + i);
                        if (ready[i] && queued[i]) {
                            queued[i] = false;
                            play(i);
                        }
                        break;
                    }
                }));
        for (int i = 0; i < soundIds.length; i++) {
            soundIds[i] = pool.load(this.context, WAV_RESOURCES[i], 1);
        }
    }

    void play(int note) {
        if (released || note < 0 || note >= soundIds.length || !audioAllowed()) return;
        if (!ready[note]) {
            queued[note] = true;
            return;
        }
        int streamId = pool.play(soundIds[note], 0.70f, 0.70f, 1, 0, 1f);
        if (streamId == 0) Log.w(TAG, "SoundPool playback rejected: " + note);
    }

    void playSequence() {
        if (released) return;
        // Déclenché uniquement par une interaction volontaire du menu.
        for (int i = 0; i < 4; i++) {
            final int note = i;
            handler.postDelayed(() -> play(note), i * 650L);
        }
    }

    private boolean audioAllowed() {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return manager != null && manager.getStreamVolume(AudioManager.STREAM_MUSIC) > 0
                && manager.getRingerMode() != AudioManager.RINGER_MODE_SILENT;
    }

    void release() {
        if (released) return;
        released = true;
        handler.removeCallbacksAndMessages(null);
        pool.release();
    }
}
