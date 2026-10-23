package com.afkalerts;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
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
        CountDownLatch played = new CountDownLatch(1);
        float[] gain = new float[1];
        int[] length = new int[1];
        AudioPlayer audio = new AudioPlayer()
        {
            @Override public void play(InputStream input, float value) throws IOException
            {
                gain[0] = value;
                length[0] = input.readAllBytes().length;
                played.countDown();
            }
        };
        try (VoicePlayer player = new VoicePlayer(audio))
        {
            player.speak(VoiceLine.PRAYER, 50, SpeakingPace.NATURAL);
            assertTrue(played.await(5, TimeUnit.SECONDS));
            assertEquals(-6.0206f, gain[0], 0.001f);
            assertTrue(length[0] > 44);
        }
    }
}
