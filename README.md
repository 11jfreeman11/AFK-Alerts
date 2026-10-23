# AFK Alerts

Offline audio notifications for prayer, aggression timer expiry, potion effects,
Slayer task completion and superior creature spawns.

## Configuration

Enable AFK Alerts in RuneLite plugin settings. Choose the prayer threshold,
alert types, volume and speaking pace. One bundled voice is provided with
Relaxed, Natural and Brisk pace options.

Keep RuneLite running and your computer awake. Audio can play while minimized.
The optional Only when unfocused setting restricts alerts to an unfocused client.

## Alerts

- Prayer at or below your chosen threshold. Restore above it to rearm.
- NPC Aggression Timer expiry. Enable and calibrate RuneLite's NPC Aggression
  Timer plugin. This is a timer estimate, not proof a monster will attack.
- Divine super combat expiry, with optional divine ranging, divine magic,
  stamina, goading and antifire expiry.
- Slayer task completion and superior creature announcements.

Optional prayer reminders and individual alert toggles are available. Login
transitions, world hops and death clear pending alerts.

## Development and validation

Use Java 11 and the Gradle wrapper: `./gradlew test build`.
For a development client, use `./gradlew run`.

The 24 automated tests cover alert state transitions, server message filtering
and all 33 PCM WAV resources (11 alerts at three paces). In-game triggers and
speaker output require manual testing.

## Distribution

Plugin Hub submission: https://github.com/runelite/plugin-hub/pull/19276

The plugin uses standard-build conventions. Bundled recordings play locally;
no TTS engine, model, API key or runtime speech-generation requests are required.
See VOICE-LICENSE.md for recording provenance and licensing.
