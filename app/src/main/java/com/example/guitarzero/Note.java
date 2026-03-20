package com.example.guitarzero;

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

    private double f(double x) {
        return 1.0 / (1.0 + Math.pow(2, -8.0 * (x - 1.0)));
    }

    private double g(double x) {
        return Math.pow(2.0, -5.0 * (x - 3.0));
    }

    public double evalScore(long currentGameTimeMs) {
        double deltaSec = Math.abs(currentGameTimeMs - absoluteTime) / 1000.0;
        return Math.min(f(deltaSec), g(deltaSec)) * 1000;
    }

    public boolean isExpired(long currentGameTimeMs) {
        return currentGameTimeMs > absoluteTime + 4000;
    }
}
