package com.example.guitarzero.engine;

public class Note {
    public int string;
    public long absoluteTime;
    public long duration;

    private float frequency; // frequency in Hertz
    private static float frequencyMultiplier; // frequency multiplier based on light sensor


    public Note(int string, long absoluteTime, long duration, float frequency) {
        this.string = string;
        this.absoluteTime = absoluteTime;
        this.duration = duration;
        this.frequency = frequency;
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

    public float getCurrentFrequency() {
        return frequency * frequencyMultiplier;
    }

    public static void setFrequencyMultiplier(float frequencyMultiplier) {
        Note.frequencyMultiplier = frequencyMultiplier;
    }
}
