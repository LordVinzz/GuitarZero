package com.example.guitarzero.engine;

public class FrameTimer {
    private final long targetFrameDelayMs;
    private final float defaultDeltaTimeSeconds;
    private long previousFrameTimeNanos;

    public FrameTimer(long targetFrameDelayMs) {
        this.targetFrameDelayMs = targetFrameDelayMs;
        this.defaultDeltaTimeSeconds = targetFrameDelayMs / 1000f;
        this.previousFrameTimeNanos = 0L;
    }

    public float getDeltaTimeSeconds(long frameStartTimeNanos) {
        if (previousFrameTimeNanos == 0L) {
            previousFrameTimeNanos = frameStartTimeNanos;
            return defaultDeltaTimeSeconds;
        }

        float deltaTimeSeconds =
                (frameStartTimeNanos - previousFrameTimeNanos) / 1_000_000_000f;
        previousFrameTimeNanos = frameStartTimeNanos;

        if (deltaTimeSeconds <= 0f) {
            return defaultDeltaTimeSeconds;
        }

        return deltaTimeSeconds;
    }

    public long getRemainingDelayMs(long frameStartTimeNanos) {
        long frameDurationMs = (System.nanoTime() - frameStartTimeNanos) / 1_000_000L;
        long remainingDelayMs = targetFrameDelayMs - frameDurationMs;
        return Math.max(remainingDelayMs, 0L);
    }
}
