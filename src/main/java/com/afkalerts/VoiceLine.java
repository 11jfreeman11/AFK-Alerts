package com.afkalerts;

enum VoiceLine
{
    PRAYER("prayer", "Your prayer is low."),
    EMPTY_PRAYER("empty-prayer", "You have run out of prayer."),
    AGGRESSION("aggression", "Your monster aggression timer has expired."),
    DIVINE_COMBAT("divine-combat", "Your divine super combat has expired."),
    DIVINE_RANGING("divine-ranging", "Your divine ranging potion has expired."),
    DIVINE_MAGIC("divine-magic", "Your divine magic potion has expired."),
    STAMINA("stamina", "Your stamina potion has expired."),
    GOADING("goading", "Your goading potion has expired."),
    ANTIFIRE("antifire", "Your antifire protection has expired."),
    SLAYER_COMPLETE("slayer-complete", "Your Slayer task is complete."),
    SUPERIOR("superior", "A superior foe has appeared.");

    String resource(SpeakingPace pace) { return "voices/3/" + pace.folder + "/" + file + ".wav"; }

    final String file;
    final String text;
    VoiceLine(String file, String text) { this.file = file; this.text = text; }
}
