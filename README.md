# AFK Alerts — Voice 3

Only voice 3 is included. The sidebar, voice selector, preview controls and preview
recordings have been removed. All alert triggers remain available.

## Start on Windows

1. Close the older test client.
2. Extract All from the new ZIP and open Start-Plugin.cmd in the extracted folder.
3. Enable AFK Alerts in RuneLite plugin settings.
4. Configure prayer threshold, alert toggles, Voice volume and Speaking pace there.

The launcher reuses portable Java when available or downloads a verified Java 11 ZIP.
Keep RuneLite running and your computer awake. Speech plays while minimized. The
Only when unfocused setting is optional; leave it off to hear alerts while focused too.

## Alerts

- Prayer at or below your selected threshold; restore above it to rearm.
- NPC Aggression Timer expiry. Enable and calibrate that built-in plugin and set its
  NPC names or Always active. This is its timer estimate, not proof a monster will attack.
- Divine super combat expiry, with optional divine ranging/magic, stamina, goading
  and antifire expiry.
- Slayer task completion and superior foe announcements, each with its own toggle.

Volume, Relaxed/Natural/Brisk pace and optional prayer reminders remain configurable.
Unused effects and tasks you cancel do not trigger completion alerts. Login transitions,
world hops and death clear pending alerts. Alert detection logic is unchanged.

## Validation

Build with Java 11: ./gradlew test build. Automated tests cover alert state transitions,
server message filtering and all 33 PCM WAV files (11 alerts at three paces). The shipped
voice 3 recordings are unchanged from the complete pack. Check actual game triggers
and playback manually; there is no built-in preview now.

## Distribution

The project follows Plugin Hub standard-build conventions and has not been submitted
or published. Sources, Gradle wrapper and bundled audio are included. No TTS engine,
model, API key or runtime speech-generation network calls are required.
