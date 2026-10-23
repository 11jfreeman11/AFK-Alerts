package com.afkalerts;

import java.io.BufferedInputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import org.slf4j.LoggerFactory;

/** Bounded serial playback; never waits for sound on the game thread. */
final class VoicePlayer implements AutoCloseable
{
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(12), task -> {
            Thread thread = new Thread(task, "afk-audio");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.DiscardPolicy());
    private volatile Clip current;
    private volatile boolean closed;
    private volatile long generation;

    VoicePlayer() {}

    void speak(VoiceLine line, int volume, SpeakingPace pace)
    {
        if (closed || volume <= 0) { return; }
        final long queued = System.nanoTime();
        final long requestedGeneration = generation;
        worker.execute(() -> {
            if (closed || requestedGeneration != generation || System.nanoTime() - queued > TimeUnit.SECONDS.toNanos(30)) { return; }
            try (BufferedInputStream input = new BufferedInputStream(
                    VoicePlayer.class.getResourceAsStream(line.resource(pace)));
                 AudioInputStream audio = AudioSystem.getAudioInputStream(input))
            {
                // Resource files are signed 16-bit little-endian PCM. Scaling PCM works
                // even on mixers which do not support MASTER_GAIN.
                byte[] data = audio.readAllBytes();
                double gain = Math.min(100, volume) / 100.0;
                for (int i = 0; i + 1 < data.length; i += 2)
                {
                    short sample = (short) ((data[i] & 255) | (data[i + 1] << 8));
                    short scaled = (short) Math.round(sample * gain);
                    data[i] = (byte) scaled;
                    data[i + 1] = (byte) (scaled >> 8);
                }
                try (Clip clip = AudioSystem.getClip())
                {
                    current = clip;
                    if (closed || requestedGeneration != generation) { return; }
                    clip.open(audio.getFormat(), data, 0, data.length);
                    if (closed || requestedGeneration != generation) { return; }
                    clip.start();
                    long finish = System.nanoTime() + TimeUnit.MICROSECONDS.toNanos(clip.getMicrosecondLength());
                    while (!closed && requestedGeneration == generation && System.nanoTime() < finish) { Thread.sleep(50); }
                    clip.stop();
                }
                finally { current = null; }
            }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            catch (Exception e)
            {
                if (!closed && requestedGeneration == generation)
                {
                    LoggerFactory.getLogger(VoicePlayer.class).warn("Voice playback failed", e);
                }
            }
        });
    }

    void clear()
    {
        generation++;
        worker.getQueue().clear();
        Clip clip = current;
        if (clip != null) { clip.stop(); }
    }

    @Override public void close()
    {
        closed = true;
        worker.shutdownNow();
        Clip clip = current;
        if (clip != null) { clip.stop(); clip.close(); }
    }
}
