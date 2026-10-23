# Voice 3 only validation

Prayer, aggression, buff and Slayer detection logic is retained. Playback is fixed to
voice 3 and reads one of 33 PCM recordings (11 alerts times three paces). The sidebar,
preview event handlers, voice-selection configuration and other voice assets are absent.
Resource tests verify every alert resolves to voices/3 and decodes as mono PCM16 WAV.
Package checks verify no other voice folders or preview WAVs/classes are distributed.
Existing recordings are checked against their earlier SHA-256 manifest.

In-game trigger and speaker-output checks remain manual. This project is not yet
published or submitted to Plugin Hub.
