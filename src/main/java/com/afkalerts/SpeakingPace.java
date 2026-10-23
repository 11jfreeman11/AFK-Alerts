package com.afkalerts;

public enum SpeakingPace
{
    RELAXED("relaxed", "Relaxed"),
    NATURAL("natural", "Natural"),
    BRISK("brisk", "Brisk");
    final String folder;
    private final String label;
    SpeakingPace(String folder, String label) { this.folder = folder; this.label = label; }
    @Override public String toString() { return label; }
}
