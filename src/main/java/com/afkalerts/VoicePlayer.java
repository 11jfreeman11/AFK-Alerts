package com.afkalerts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.runelite.client.audio.AudioPlayer;
import org.slf4j.LoggerFactory;

/** Serial playback through RuneLite's audio manager, off the game thread. */
final class VoicePlayer implements AutoCloseable
{
    private final AudioPlayer audioPlayer;
    private final ScheduledExecutorService scheduler;
    private final Deque<Announcement> queue = new ArrayDeque<>();
    private ScheduledFuture<?> pending;
    private boolean closed;

    VoicePlayer(AudioPlayer audioPlayer, ScheduledExecutorService scheduler)
    {
        this.audioPlayer = audioPlayer;
        this.scheduler = scheduler;
    }

    synchronized void speak(VoiceLine line, int volume, SpeakingPace pace)
    {
        if (closed || volume <= 0 || queue.size() >= 12) { return; }
        queue.addLast(new Announcement(line, volume, pace));
        if (pending == null)
        {
            pending = scheduler.schedule(this::playNext, 0, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized void playNext()
    {
        pending = null;
        if (closed) { return; }
        Announcement next;
        while ((next = queue.pollFirst()) != null)
        {
            if (System.nanoTime() - next.queued > TimeUnit.SECONDS.toNanos(30)) { continue; }
            try (InputStream input = VoicePlayer.class.getResourceAsStream(next.line.resource(next.pace)))
            {
                if (input == null) { throw new IOException("Missing voice recording"); }
                byte[] data = input.readAllBytes();
                long duration = durationMillis(data);
                float gain = (float) (20.0 * Math.log10(Math.min(100, next.volume) / 100.0));
                audioPlayer.play(new ByteArrayInputStream(data), gain);
                // RuneLite owns playback. Schedule the next phrase without blocking
                // a worker or interrupting an announcement already playing.
                pending = scheduler.schedule(this::playNext, duration + 100, TimeUnit.MILLISECONDS);
                return;
            }
            catch (Exception e)
            {
                LoggerFactory.getLogger(VoicePlayer.class).warn("Voice playback failed", e);
            }
        }
    }

    private static final class Announcement
    {
        final VoiceLine line;
        final int volume;
        final SpeakingPace pace;
        final long queued = System.nanoTime();
        Announcement(VoiceLine line, int volume, SpeakingPace pace)
        {
            this.line = line;
            this.volume = volume;
            this.pace = pace;
        }
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

    synchronized void clear()
    {
        queue.clear();
    }

    @Override public synchronized void close()
    {
        closed = true;
        queue.clear();
        if (pending != null)
        {
            pending.cancel(false);
            pending = null;
        }
        // The injected scheduler belongs to RuneLite; leave its lifecycle alone.
    }
}
