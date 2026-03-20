package com.example.guitarzero.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameplaySession {
    private static final double MIN_SCORE_TO_HIT = 0.01;

    private final int stringCount;
    private final List<Note> notes = new ArrayList<Note>();

    private long gameTimeMs;
    private double score;

    public GameplaySession(int stringCount) {
        this.stringCount = stringCount;
        reset();
    }

    public void reset() {
        gameTimeMs = 0L;
        score = 0d;
        notes.clear();

        for (int stringIndex = 0; stringIndex < stringCount; stringIndex++) {
            notes.add(new Note(stringIndex, 2000L * (stringIndex + 1), 1000));
        }
    }

    public void update(float deltaTimeSeconds) {
        gameTimeMs += Math.round(deltaTimeSeconds * 1000f);

        Iterator<Note> iterator = notes.iterator();
        while (iterator.hasNext()) {
            Note note = iterator.next();
            if (note.isExpired(gameTimeMs)) {
                iterator.remove();
            }
        }
    }

    public void registerStringHit(int stringIndex) {
        Note bestNote = null;
        double bestScore = -1d;

        for (Note note : notes) {
            if (note.corde != stringIndex) {
                continue;
            }

            double currentScore = note.evalScore(gameTimeMs);
            if (currentScore > bestScore) {
                bestScore = currentScore;
                bestNote = note;
            }
        }

        if (bestNote != null && bestScore >= MIN_SCORE_TO_HIT) {
            score += bestScore;
            notes.remove(bestNote);
        }
    }

    public long getGameTimeMs() {
        return gameTimeMs;
    }

    public double getScore() {
        return score;
    }

    public int getRemainingNotesCount() {
        return notes.size();
    }
}
