package com.afkalerts;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.runelite.api.ChatMessageType;

/** Accept server messages only, never player chat or task-counter resets. */
final class SlayerAlerts
{
    private static final Pattern COMPLETED = Pattern.compile(
        "^(?:You've|You have) completed (?:your (?:Slayer )?task[!.].*|(?:at least )?[\\d,]+ (?:Wilderness )?tasks?\\b.*(?:received|Slayer master|Slayer Master).*)$");
    private final Map<VoiceLine, Long> pending = new EnumMap<>(VoiceLine.class);
    private final Map<VoiceLine, Long> last = new EnumMap<>(VoiceLine.class);

    static VoiceLine classify(ChatMessageType type, String message)
    {
        if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM) { return null; }
        String text = message.replaceAll("<[^>]*>", "").replace('\u00a0', ' ').trim();
        if (text.equals("A superior foe has appeared...")) { return VoiceLine.SUPERIOR; }
        return COMPLETED.matcher(text).matches() ? VoiceLine.SLAYER_COMPLETE : null;
    }

    void record(VoiceLine line, long now)
    {
        Long previous = last.get(line);
        if (previous == null || now - previous >= 2000)
        {
            last.put(line, now);
            pending.put(line, now);
        }
    }

    boolean take(VoiceLine line, long now, boolean enabled, boolean audible)
    {
        Long recorded = pending.get(line);
        if (recorded == null) { return false; }
        if (!enabled || now - recorded > 30000) { pending.remove(line); return false; }
        if (!audible) { return false; }
        pending.remove(line);
        return true;
    }
}
