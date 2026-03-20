package com.example.guitarzero.engine;

public class Note {
    public int corde;
    public long absoluteTime;
    public long duration;

    private static int frequencyShift;

    public Note(int corde, long absoluteTime, long duration) {
        this.corde = corde;
        this.absoluteTime = absoluteTime;
        this.duration = duration;
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
}
