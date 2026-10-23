package com.afkalerts;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(AfkAlertsConfig.GROUP)
public interface AfkAlertsConfig extends Config
{
    String GROUP = "afkalerts";

    @ConfigItem(keyName="prayer", name="Prayer alert", description="Speak when prayer falls to or below your threshold.", position=0)
    default boolean prayer() { return true; }

    @Range(min=0, max=99)
    @ConfigItem(keyName="prayerThreshold", name="Prayer threshold", description="Prayer points, inclusive. Zero alerts only when prayer runs out. Restore above this value to rearm.", position=1)
    default int prayerThreshold() { return 10; }

    @Range(min=0, max=120)
    @ConfigItem(keyName="prayerRepeat", name="Prayer reminder (seconds)", description="Repeat while prayer stays low. Zero means one alert per threshold crossing.", position=2)
    default int prayerRepeat() { return 0; }

    @ConfigItem(keyName="aggression", name="Aggression timer alert", description="Uses the built-in NPC Aggression Timer. Enable and calibrate that plugin; configure its NPC names or Always active.", position=3)
    default boolean aggression() { return true; }

    @ConfigItem(keyName="divineCombat", name="Divine super combat expiry", description="Speak when the observed divine super combat timer ends.", position=4)
    default boolean divineCombat() { return true; }

    @ConfigItem(keyName="divineRanging", name="Divine ranging expiry", description="Speak when divine ranging ends.", position=5)
    default boolean divineRanging() { return false; }

    @ConfigItem(keyName="divineMagic", name="Divine magic expiry", description="Speak when divine magic ends.", position=6)
    default boolean divineMagic() { return false; }

    @ConfigItem(keyName="stamina", name="Stamina expiry", description="Speak when stamina potion duration ends.", position=7)
    default boolean stamina() { return false; }

    @ConfigItem(keyName="goading", name="Goading potion expiry", description="Separate from natural NPC aggression. Speak when the goading potion timer ends.", position=8)
    default boolean goading() { return false; }

    @ConfigItem(keyName="antifire", name="Antifire expiry", description="Speak when both regular and super antifire timers are inactive.", position=9)
    default boolean antifire() { return false; }

    @Range(min=0, max=100)
    @ConfigItem(keyName="volume", name="Voice volume", description="0 mutes speech; 100 is full volume. Independent of in-game sound settings.", position=10)
    default int volume() { return 80; }

    @ConfigItem(keyName="backgroundOnly", name="Only when unfocused", description="Speak only when RuneLite is unfocused or minimized. Off allows alerts while focused too.", position=11)
    default boolean backgroundOnly() { return false; }

    @ConfigItem(keyName="slayerComplete", name="Slayer task completed", description="Speak when the game announces your task completion. Skipping a task does not trigger it.", position=12)
    default boolean slayerComplete() { return true; }

    @ConfigItem(keyName="superior", name="Superior foe appeared", description="Speak when the game announces a superior Slayer monster. Does not require the Slayer plugin.", position=13)
    default boolean superior() { return true; }

    @ConfigItem(keyName="pace", name="Speaking pace", description="Pitch-preserving Relaxed, Natural or Brisk speech. Preview before saving.", position=-1)
    default SpeakingPace pace() { return SpeakingPace.RELAXED; }
}
