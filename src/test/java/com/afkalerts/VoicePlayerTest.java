package com.afkalerts;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.runelite.client.audio.AudioPlayer;
import org.junit.Test;
import static org.junit.Assert.*;

public class VoicePlayerTest
{
    @Test public void measuredDurationsMatchDecodedResources() throws Exception
    {
        for (SpeakingPace pace : SpeakingPace.values()) for (VoiceLine line : VoiceLine.values())
        {
            try (InputStream input = VoicePlayer.class.getResourceAsStream(line.resource(pace)))
            {
                byte[] data = input.readAllBytes();
                try (javax.sound.sampled.AudioInputStream decoded = javax.sound.sampled.AudioSystem
                    .getAudioInputStream(new java.io.ByteArrayInputStream(data)))
                {
                    long expected = (long) Math.ceil(decoded.getFrameLength() * 1000.0
                        / decoded.getFormat().getFrameRate());
                    assertEquals(expected, VoicePlayer.durationMillis(data));
                }
            }
        }
    }

    @Test(expected = IOException.class) public void malformedRecordingIsRejected() throws Exception
    {
        VoicePlayer.durationMillis(new byte[12]);
    }

    @Test public void playbackUsesRuneLiteAndConvertsVolumeToDecibels() throws Exception
    {
        ManualScheduler scheduler = new ManualScheduler();
        RecordingAudio audio = new RecordingAudio();
        try (VoicePlayer player = new VoicePlayer(audio, scheduler))
        {
            player.speak(VoiceLine.PRAYER, 50, SpeakingPace.NATURAL);
            assertEquals(0, audio.calls);
            scheduler.runNext();
            assertEquals(1, audio.calls);
            assertEquals(-6.0206f, audio.gain, 0.001f);
            assertTrue(audio.length > 44);
            assertTrue(scheduler.tasks.peek().delay > 500);
        }
        assertFalse(scheduler.isShutdown());
    }

    @Test public void queuedAlertsWaitForScheduledCompletion()
    {
        ManualScheduler scheduler = new ManualScheduler();
        RecordingAudio audio = new RecordingAudio();
        try (VoicePlayer player = new VoicePlayer(audio, scheduler))
        {
            player.speak(VoiceLine.PRAYER, 100, SpeakingPace.NATURAL);
            player.speak(VoiceLine.SUPERIOR, 100, SpeakingPace.NATURAL);
            assertEquals(1, scheduler.tasks.size());
            scheduler.runNext();
            assertEquals(1, audio.calls);
            scheduler.runNext();
            assertEquals(2, audio.calls);
        }
    }

    @Test public void clearDropsPendingAnnouncements()
    {
        ManualScheduler scheduler = new ManualScheduler();
        RecordingAudio audio = new RecordingAudio();
        try (VoicePlayer player = new VoicePlayer(audio, scheduler))
        {
            player.speak(VoiceLine.PRAYER, 100, SpeakingPace.NATURAL);
            player.speak(VoiceLine.SUPERIOR, 100, SpeakingPace.NATURAL);
            scheduler.runNext();
            player.clear();
            scheduler.runNext();
            assertEquals(1, audio.calls);
        }
    }

    @Test public void closeCancelsWithoutInterruptingOrShuttingDownSharedScheduler()
    {
        ManualScheduler scheduler = new ManualScheduler();
        RecordingAudio audio = new RecordingAudio();
        VoicePlayer player = new VoicePlayer(audio, scheduler);
        player.speak(VoiceLine.PRAYER, 100, SpeakingPace.NATURAL);
        Task task = scheduler.tasks.peek();
        player.close();
        scheduler.runNext();
        assertEquals(0, audio.calls);
        assertTrue(task.isCancelled());
        assertFalse(task.interruptRequested);
        assertFalse(scheduler.isShutdown());
        player.speak(VoiceLine.PRAYER, 100, SpeakingPace.NATURAL);
        assertTrue(scheduler.tasks.isEmpty());
    }

    @Test public void mutedAndExcessAnnouncementsAreDropped()
    {
        ManualScheduler scheduler = new ManualScheduler();
        RecordingAudio audio = new RecordingAudio();
        try (VoicePlayer player = new VoicePlayer(audio, scheduler))
        {
            player.speak(VoiceLine.PRAYER, 0, SpeakingPace.NATURAL);
            assertTrue(scheduler.tasks.isEmpty());
            for (int i = 0; i < 20; i++)
            {
                player.speak(VoiceLine.PRAYER, 100, SpeakingPace.NATURAL);
            }
            while (!scheduler.tasks.isEmpty()) { scheduler.runNext(); }
            assertEquals(12, audio.calls);
        }
    }

    private static class RecordingAudio extends AudioPlayer
    {
        int calls;
        int length;
        float gain;
        @Override public void play(InputStream input, float value) throws IOException
        {
            length = input.readAllBytes().length;
            gain = value;
            calls++;
        }
    }

    private static class ManualScheduler extends ScheduledThreadPoolExecutor
    {
        final Deque<Task> tasks = new ArrayDeque<>();
        ManualScheduler() { super(1); }
        @Override public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
        {
            Task task = new Task(command, unit.toMillis(delay));
            tasks.addLast(task);
            return task;
        }
        void runNext() { tasks.removeFirst().run(); }
    }

    private static class Task extends FutureTask<Void> implements ScheduledFuture<Void>
    {
        final long delay;
        boolean interruptRequested;
        Task(Runnable runnable, long delay) { super(runnable, null); this.delay = delay; }
        @Override public boolean cancel(boolean mayInterruptIfRunning)
        {
            interruptRequested = mayInterruptIfRunning;
            return super.cancel(mayInterruptIfRunning);
        }
        @Override public long getDelay(TimeUnit unit) { return unit.convert(delay, TimeUnit.MILLISECONDS); }
        @Override public int compareTo(Delayed other)
        {
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}
