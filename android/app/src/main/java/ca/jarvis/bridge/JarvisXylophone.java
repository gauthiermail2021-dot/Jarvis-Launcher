package ca.jarvis.bridge;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;

/**
 * Notes brèves de xylophone synthétisées localement, sans dépendance réseau.
 * Do5 (523.25 Hz), Ré5 (587.33 Hz), Mi5 (659.25 Hz), Sol5 (783.99 Hz).
 *
 * Une seule note par changement d'état réel; la déduplication est assurée
 * par JarvisBubbleService.showActivity(). Aucun son au démarrage/au repos.
 */
final class JarvisXylophone {
    static final int DO5 = 0;
    static final int RE5 = 1;
    static final int MI5 = 2;
    static final int SOL5 = 3;
    private static final double[] FREQUENCIES = {523.25, 587.33, 659.25, 783.99};
    private static final int SAMPLE_RATE = 24000;
    private static final double DURATION_SECONDS = 0.43;
    private static final double TWO_PI = 2.0 * Math.PI;

    private JarvisXylophone() {}

    static void play(Context context, int note) {
        if (note < 0 || note >= FREQUENCIES.length) return;
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (manager == null || manager.getRingerMode() == AudioManager.RINGER_MODE_SILENT
                || manager.getStreamVolume(AudioManager.STREAM_MUSIC) <= 0) return;
        final double frequency = FREQUENCIES[note];
        // Ne jamais bloquer le thread d'affichage de la bulle.
        new Thread(() -> {
            AudioTrack track = null;
            try {
                final int count = (int) (SAMPLE_RATE * DURATION_SECONDS);
                short[] samples = new short[count];
                for (int i = 0; i < count; i++) {
                    double t = (double) i / SAMPLE_RATE;
                    // Attaque de maillet de 5 ms, décroissance naturelle.
                    double attack = Math.min(1.0, t / 0.005);
                    double fade = Math.min(1.0, (DURATION_SECONDS - t) / 0.065);
                    double envelope = attack * Math.exp(-10.5 * t) * Math.max(0.0, fade);
                    // Partiels inharmoniques : couleur métallique et boisée.
                    double struckBar =
                            0.83 * Math.sin(TWO_PI * frequency * t)
                            + 0.30 * Math.exp(-15.0 * t) * Math.sin(TWO_PI * frequency * 2.74 * t)
                            + 0.12 * Math.exp(-24.0 * t) * Math.sin(TWO_PI * frequency * 5.12 * t);
                    samples[i] = (short) (Short.MAX_VALUE * 0.23 * envelope * struckBar);
                }
                int minBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                if (minBytes <= 0) return;
                track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(Math.max(minBytes, samples.length * 2))
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();
                if (track.getState() != AudioTrack.STATE_INITIALIZED) return;
                if (track.write(samples, 0, samples.length, AudioTrack.WRITE_BLOCKING)
                        != samples.length) return;
                track.play();
                // MODE_STATIC doit rester vivant jusqu'à la fin de la lecture.
                Thread.sleep((long) (DURATION_SECONDS * 1000) + 35L);
            } catch (Exception ignored) {
                // Le son reste purement facultatif : jamais bloquer l'ouverture SVG.
            } finally {
                if (track != null) {
                    try { track.stop(); } catch (IllegalStateException ignored) {}
                    track.release();
                }
            }
        }, "Jarvis-Xylophone-" + note).start();
    }
}
