package com.example.guitarzero.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameplaySession {
    private static final float MAX_NOTE_SCORE = 1000f;
    private static final long APPROACH_TIME_MS = 2000L;
    private static final float NOTE_HIT_Y_NORMALIZED = 0.75f;
    private static final double PERFECT_SCORE_EPSILON = 0.001;

    public static final class NoteWaveState {
        public final int stringIndex;
        public final float waveYNormalized;
        public final float intensity;

        public NoteWaveState(int stringIndex, float waveYNormalized, float intensity) {
            this.stringIndex = stringIndex;
            this.waveYNormalized = waveYNormalized;
            this.intensity = intensity;
        }
    }

    private final int stringCount;
    private final List<Note> notes = new ArrayList<Note>();
    private final ComboState comboState = new ComboState();

    private long gameTimeMs;
    private double score;

    public GameplaySession(int stringCount) {
        this.stringCount = stringCount;
        reset();
    }

    public void reset() {
        gameTimeMs = 0L;
        score = 0d;
        comboState.reset();
        notes.clear();

        for (int stringIndex = 0; stringIndex < stringCount; stringIndex++) {
            notes.add(new Note(stringIndex, 2000L * (stringIndex + 1), 1000, 0));
        }

        for (int stringIndex = 0; stringIndex < stringCount; stringIndex++) {
            notes.add(new Note(stringIndex, 8000 + 2000L * (stringIndex + 1), 1000, 0));
        }

        for (int stringIndex = 0; stringIndex < stringCount; stringIndex++) {
            notes.add(new Note(stringIndex, 16000 + 2000L * (stringIndex + 1), 1000, 0));
        }
    }

    public void update(float deltaTimeSeconds) {
        gameTimeMs += Math.round(deltaTimeSeconds * 1000f);

        Iterator<Note> iterator = notes.iterator();
        while (iterator.hasNext()) {
            Note note = iterator.next();
            if (note.isExpired(gameTimeMs)) {
                comboState.registerMiss();
                iterator.remove();
            }
        }
    }

    public HitResult registerStringHit(int stringIndex) {
        Note bestNote = null;
        double bestScore = -1d;

        for (Note note : notes) {
            if (note.string != stringIndex) {
                continue;
            }

            double currentScore = note.evalScore(gameTimeMs);
            if (currentScore > bestScore) {
                bestScore = currentScore;
                bestNote = note;
            }
        }

        // Update score
        score += bestScore * comboState.getMultiplier();
        notes.remove(bestNote);

        // Process hit result
        if (bestNote == null || bestScore <= 0d) {
            comboState.registerMiss();
            return HitResult.MISS;
        } else if (isPerfectScore(bestScore)) {
            comboState.registerPerfect();
            return HitResult.PERFECT;
        } else return HitResult.GOOD;

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

    public int getComboMultiplier() {
        return comboState.getMultiplier();
    }

    public int getComboTokens() {
        return comboState.getTokens();
    }

    public float[] getStringHighlightStrengths() {
        float[] highlightStrengths = new float[stringCount];

        for (Note note : notes) {
            float normalizedScore = (float) (note.evalScore(gameTimeMs) / MAX_NOTE_SCORE);
            normalizedScore = clamp(normalizedScore);
            if (normalizedScore > highlightStrengths[note.string]) {
                highlightStrengths[note.string] = normalizedScore;
            }
        }

        return highlightStrengths;
    }

    public NoteWaveState[] getNoteWaveStates() {
        List<NoteWaveState> waveStates = new ArrayList<NoteWaveState>();

        for (Note note : notes) {
            NoteWaveState waveState = createNoteWaveState(note);
            if (waveState != null) {
                waveStates.add(waveState);
            }
        }

        return waveStates.toArray(new NoteWaveState[waveStates.size()]);
    }

    private NoteWaveState createNoteWaveState(Note note) {
        long visibleStartTimeMs = note.absoluteTime - APPROACH_TIME_MS;
        long visibleEndTimeMs = note.absoluteTime + Math.max(note.duration, 1L);
        if (gameTimeMs < visibleStartTimeMs || gameTimeMs > visibleEndTimeMs) {
            return null;
        }

        float waveYNormalized;
        if (gameTimeMs <= note.absoluteTime) {
            float preHitProgress =
                    (gameTimeMs - visibleStartTimeMs) / (float) APPROACH_TIME_MS;
            waveYNormalized = clamp(preHitProgress * NOTE_HIT_Y_NORMALIZED);
        } else {
            float postHitProgress =
                    (gameTimeMs - note.absoluteTime) / (float) Math.max(note.duration, 1L);
            waveYNormalized = clamp(
                    NOTE_HIT_Y_NORMALIZED + (postHitProgress * (1f - NOTE_HIT_Y_NORMALIZED))
            );
        }

        return new NoteWaveState(note.string, waveYNormalized, 1f);
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(value, 1f));
    }

    private boolean isPerfectScore(double scoreValue) {
        return scoreValue >= (MAX_NOTE_SCORE - PERFECT_SCORE_EPSILON);
    }
}
