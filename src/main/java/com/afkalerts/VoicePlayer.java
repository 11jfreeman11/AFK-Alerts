package com.afkalerts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.runelite.client.audio.AudioPlayer;
import org.slf4j.LoggerFactory;

/** Serial playback through RuneLite's audio manager, off the game thread. */
final class VoicePlayer implements AutoCloseable
{
    private final AudioPlayer audioPlayer;
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(12), task -> {
            Thread thread = new Thread(task, "afk-audio");
            thread.setDaemon(true);
            return thread;
        }, new ThreadPoolExecutor.DiscardPolicy());
    private volatile boolean closed;
    private volatile long generation;

    VoicePlayer(AudioPlayer audioPlayer) { this.audioPlayer = audioPlayer; }

    void speak(VoiceLine line, int volume, SpeakingPace pace)
    {
        if (closed || volume <= 0) { return; }
        final long queued = System.nanoTime();
        final long requestedGeneration = generation;
        worker.execute(() -> {
            if (closed || requestedGeneration != generation
                || System.nanoTime() - queued > TimeUnit.SECONDS.toNanos(30)) { return; }
            try (InputStream input = VoicePlayer.class.getResourceAsStream(line.resource(pace)))
            {
                if (input == null) { throw new IOException("Missing voice recording"); }
                byte[] data = input.readAllBytes();
                long duration = durationMillis(data);
                float gain = (float) (20.0 * Math.log10(Math.min(100, volume) / 100.0));
                if (closed || requestedGeneration != generation) { return; }
                audioPlayer.play(new ByteArrayInputStream(data), gain);
                // AudioPlayer owns playback and cleanup. Wait only on our worker so
                // simultaneous alerts remain sequential. A started phrase finishes.
                Thread.sleep(duration + 100);
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

    static long durationMillis(byte[] wav) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN);
        if (wav.length < 12 || buffer.getInt(0) != 0x46464952
            || buffer.getInt(8) != 0x45564157) { throw new IOException("Invalid WAV header"); }
        long byteRate = 0;
        long dataSize = -1;
        int offset = 12;
        while (offset <= wav.length - 8)
        {
            int kind = buffer.getInt(offset);
            long size = Integer.toUnsignedLong(buffer.getInt(offset + 4));
            long end = offset + 8L + size;
            if (end > wav.length) { throw new IOException("Truncated WAV chunk"); }
            if (kind == 0x20746d66 && size >= 16)
            {
                byteRate = Integer.toUnsignedLong(buffer.getInt(offset + 16));
            }
            if (kind == 0x61746164) { dataSize = size; }
            long next = end + (size & 1);
            if (next > wav.length) { break; }
            offset = (int) next;
        }
        if (byteRate == 0 || dataSize < 0) { throw new IOException("Missing WAV audio data"); }
        return (dataSize * 1000 + byteRate - 1) / byteRate;
    }

    void clear()
    {
        generation++;
        worker.getQueue().clear();
    }

    @Override public void close()
    {
        closed = true;
        generation++;
        worker.shutdownNow();
    }
}
