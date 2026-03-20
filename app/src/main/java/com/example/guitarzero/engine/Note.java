package com.example.guitarzero.engine;

public class Note {
    public int string;
    public long absoluteTime;
    public long duration;

    private final float samplePitchMultiplier;

    public Note(int string, long absoluteTime, long duration, float samplePitchMultiplier) {
        this.string = string;
        this.absoluteTime = absoluteTime;
        this.duration = duration;
        this.samplePitchMultiplier = samplePitchMultiplier;
    }

    public double evalScore(long currentGameTimeMs) {
        if (duration <= 0) {
            return 0.0;
        }
        double t = (currentGameTimeMs - absoluteTime) / (double) duration;
        double score = -Math.abs(t) - Math.abs(t - 1.0) + 2.0;
        return Math.max(0.0, score) * 1000;
    }

    public boolean isExpired(long currentGameTimeMs) {
        return currentGameTimeMs > absoluteTime + (3 * duration) / 2;
    }

    public float getSamplePitchMultiplier() {
        return samplePitchMultiplier;
    }
}
