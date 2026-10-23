package com.afkalerts;
import java.io.BufferedInputStream;
import java.io.InputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.junit.Test;
import static org.junit.Assert.*;
public class VoiceResourcesTest {
 @Test public void allAlertsUseVoiceThreeAtEveryPace() throws Exception {
  assertEquals(11,VoiceLine.values().length);
  for(SpeakingPace pace:SpeakingPace.values()) for(VoiceLine line:VoiceLine.values()) {
   assertTrue(line.resource(pace).startsWith("voices/3/"));
   InputStream resource=VoicePlayer.class.getResourceAsStream(line.resource(pace));
   assertNotNull(line.resource(pace),resource);
   try(BufferedInputStream input=new BufferedInputStream(resource);AudioInputStream audio=AudioSystem.getAudioInputStream(input)) {
    assertEquals(16,audio.getFormat().getSampleSizeInBits());
    assertEquals(1,audio.getFormat().getChannels());
    assertFalse(audio.getFormat().isBigEndian());
    assertTrue(audio.getFrameLength()>audio.getFormat().getFrameRate()/2);
   }
  }
 }
}
