package com.afkalerts;

import com.google.inject.Provides;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.npcunaggroarea.NpcAggroAreaPlugin;
import net.runelite.client.ui.ClientUI;

@PluginDescriptor(name="AFK Alerts",
    description="Offline voice alerts for prayer, aggression, potions and Slayer tasks",
    tags={"audio", "voice", "afk", "prayer", "aggro", "divine", "notifications"})
public class AfkAlertsPlugin extends Plugin
{
    @Inject private Client client;
    @Inject private AudioPlayer audioPlayer;
    @Inject private ScheduledExecutorService scheduler;
    @Inject private ClientThread clientThread;
    @Inject private AfkAlertsConfig config;
    @Inject private PluginManager pluginManager;
    @Inject private ClientUI clientUI;
    private AlertTracker tracker = new AlertTracker();
    private SlayerAlerts slayer = new SlayerAlerts();
    private final Map<VoiceLine, AlertTracker.Effect> effects = new EnumMap<>(VoiceLine.class);
    private volatile VoicePlayer voice;

    @Provides AfkAlertsConfig provideConfig(ConfigManager manager)
    {
        return manager.getConfig(AfkAlertsConfig.class);
    }

    @Override protected void startUp() throws Exception
    {
        reset();
        voice = new VoicePlayer(audioPlayer, scheduler);
    }

    @Override protected void shutDown()
    {
        VoicePlayer old = voice;
        voice = null;
        if (old != null) { old.close(); }
        reset();
    }

    private void reset()
    {
        VoicePlayer player = voice;
        if (player != null) { player.clear(); }
        tracker = new AlertTracker();
        slayer = new SlayerAlerts();
        effects.clear();
    }

    @Subscribe public void onConfigChanged(ConfigChanged event)
    {
        if (AfkAlertsConfig.GROUP.equals(event.getGroup())
            && "pace".equals(event.getKey()))
        {
            VoicePlayer player = voice;
            if (player != null) { player.clear(); }
            return;
        }
        if (AfkAlertsConfig.GROUP.equals(event.getGroup())
            && !"volume".equals(event.getKey()) && !"backgroundOnly".equals(event.getKey()))
        {
            clientThread.invoke(this::reset);
        }
    }

    @Subscribe public void onGameStateChanged(GameStateChanged event)
    {
        GameState state = event.getGameState();
        if (state == GameState.LOGIN_SCREEN || state == GameState.HOPPING
            || state == GameState.CONNECTION_LOST || state == GameState.LOGGING_IN)
        {
            reset();
        }
    }

    @Subscribe public void onActorDeath(ActorDeath event)
    {
        if (event.getActor() == client.getLocalPlayer()) { reset(); }
    }

    @Subscribe(priority=-10) public void onGameTick(GameTick event)
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null) { return; }
        if (client.getBoostedSkillLevel(Skill.HITPOINTS) <= 0) { reset(); return; }
        long now = System.currentTimeMillis();
        boolean audible = voice != null && config.volume() > 0 && (!config.backgroundOnly() || !clientUI.isFocused());
        if (slayer.take(VoiceLine.SLAYER_COMPLETE, now, config.slayerComplete(), audible)) { speak(VoiceLine.SLAYER_COMPLETE); }
        if (slayer.take(VoiceLine.SUPERIOR, now, config.superior(), audible)) { speak(VoiceLine.SUPERIOR); }
        int points = client.getBoostedSkillLevel(Skill.PRAYER);
        if (config.prayer() && tracker.prayer(points, config.prayerThreshold(), config.prayerRepeat(), now, audible))
        {
            speak(points == 0 ? VoiceLine.EMPTY_PRAYER : VoiceLine.PRAYER);
        }

        NpcAggroAreaPlugin aggression = null;
        for (Plugin plugin : pluginManager.getPlugins())
        {
            if (plugin instanceof NpcAggroAreaPlugin && pluginManager.isPluginActive(plugin))
            {
                aggression = (NpcAggroAreaPlugin) plugin;
                break;
            }
        }
        Instant end = aggression != null && aggression.isActive() ? aggression.getEndTime() : null;
        Long deadline = config.aggression() && end != null ? end.toEpochMilli() : null;
        if (tracker.aggression(deadline, now, audible)) { speak(VoiceLine.AGGRESSION); }

        effect(VoiceLine.DIVINE_COMBAT, config.divineCombat(), audible, VarbitID.DIVINECOMBAT_POTION_TIME);
        effect(VoiceLine.DIVINE_RANGING, config.divineRanging(), audible, VarbitID.DIVINERANGE_POTION_TIME);
        effect(VoiceLine.DIVINE_MAGIC, config.divineMagic(), audible, VarbitID.DIVINEMAGIC_POTION_TIME);
        effect(VoiceLine.STAMINA, config.stamina(), audible, VarbitID.STAMINA_DURATION);
        effect(VoiceLine.GOADING, config.goading(), audible, VarbitID.GOADING_POTION_TIMER);
        effect(VoiceLine.ANTIFIRE, config.antifire(), audible, VarbitID.ANTIFIRE_POTION, VarbitID.SUPER_ANTIFIRE_POTION);

    }

    @Subscribe public void onChatMessage(ChatMessage event)
    {
        if (client.getGameState() != GameState.LOGGED_IN) { return; }
        VoiceLine line = SlayerAlerts.classify(event.getType(), event.getMessage());
        if (line == VoiceLine.SLAYER_COMPLETE && config.slayerComplete()
            || line == VoiceLine.SUPERIOR && config.superior())
        {
            slayer.record(line, System.currentTimeMillis());
        }
    }

    private void effect(VoiceLine line, boolean enabled, boolean audible, int... varbits)
    {
        if (!enabled) { effects.remove(line); return; }
        boolean active = false;
        for (int varbit : varbits) { active |= client.getVarbitValue(varbit) > 0; }
        if (effects.computeIfAbsent(line, key -> new AlertTracker.Effect()).update(active, audible)) { speak(line); }
    }

    private void speak(VoiceLine line)
    {
        VoicePlayer player = voice;
        if (player != null) { player.speak(line, config.volume(), config.pace()); }
    }

}
