package com.afkalerts;

import net.runelite.api.ChatMessageType;
import org.junit.Test;
import static org.junit.Assert.*;

public class SlayerAlertsTest
{
    @Test public void matchesSuperiorAnnouncementWithFormatting()
    {
        assertEquals(VoiceLine.SUPERIOR, SlayerAlerts.classify(ChatMessageType.GAMEMESSAGE, "<col=ff0000>A superior foe has appeared...</col>"));
    }
    @Test public void acceptsCompletionVariants()
    {
        assertEquals(VoiceLine.SLAYER_COMPLETE, SlayerAlerts.classify(ChatMessageType.GAMEMESSAGE, "You've completed your task! You can return to a Slayer master."));
        assertEquals(VoiceLine.SLAYER_COMPLETE, SlayerAlerts.classify(ChatMessageType.SPAM, "You have completed 10 tasks and received 30 points, giving you a total of 100."));
        assertEquals(VoiceLine.SLAYER_COMPLETE, SlayerAlerts.classify(ChatMessageType.GAMEMESSAGE, "You've completed 1 task and received 0 points."));
    }
    @Test public void ignoresPlayerChatSkipsAndUnrelatedTasks()
    {
        assertNull(SlayerAlerts.classify(ChatMessageType.PUBLICCHAT, "A superior foe has appeared..."));
        assertNull(SlayerAlerts.classify(ChatMessageType.PUBLICCHAT, "You've completed your task!"));
        assertNull(SlayerAlerts.classify(ChatMessageType.GAMEMESSAGE, "Your task has been cancelled."));
        assertNull(SlayerAlerts.classify(ChatMessageType.GAMEMESSAGE, "You have completed 10 tasks in the league."));
    }
    @Test public void deduplicatesAndAllowsLaterSuperior()
    {
        SlayerAlerts alerts = new SlayerAlerts();
        alerts.record(VoiceLine.SUPERIOR, 1000);
        assertTrue(alerts.take(VoiceLine.SUPERIOR, 1000, true, true));
        alerts.record(VoiceLine.SUPERIOR, 1100);
        assertFalse(alerts.take(VoiceLine.SUPERIOR, 1100, true, true));
        alerts.record(VoiceLine.SUPERIOR, 4000);
        assertTrue(alerts.take(VoiceLine.SUPERIOR, 4000, true, true));
    }
    @Test public void respectsToggleAndExpiresDeferredAnnouncements()
    {
        SlayerAlerts alerts = new SlayerAlerts();
        alerts.record(VoiceLine.SLAYER_COMPLETE, 1000);
        assertFalse(alerts.take(VoiceLine.SLAYER_COMPLETE, 2000, true, false));
        assertTrue(alerts.take(VoiceLine.SLAYER_COMPLETE, 3000, true, true));
        alerts.record(VoiceLine.SUPERIOR, 1000);
        assertFalse(alerts.take(VoiceLine.SUPERIOR, 32000, true, true));
        alerts.record(VoiceLine.SUPERIOR, 40000);
        assertFalse(alerts.take(VoiceLine.SUPERIOR, 40000, false, true));
    }
}
