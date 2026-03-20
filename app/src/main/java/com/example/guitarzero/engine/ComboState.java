package com.example.guitarzero.engine;

public class ComboState {
    private static final int TOKENS_PER_MULTIPLIER = 10;

    private int multiplier = 1;
    private int tokens = 0;

    public void reset() {
        multiplier = 1;
        tokens = 0;
    }

    public void registerPerfect() {
        tokens += 1;
        if (tokens >= TOKENS_PER_MULTIPLIER) {
            tokens = 0;
            multiplier += 1;
        }
    }

    public void registerMiss() {
        if (multiplier > 1) {
            multiplier -= 1;
        }
        tokens = 0;
    }

    public int getMultiplier() {
        return multiplier;
    }

    public int getTokens() {
        return tokens;
    }
}
