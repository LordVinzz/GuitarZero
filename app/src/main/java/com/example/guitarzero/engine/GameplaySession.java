package com.example.guitarzero.engine;

import com.example.guitarzero.engine.map.MapFile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameplaySession {
    private static final float MAX_NOTE_SCORE = 1000f;
    public static final long APPROACH_TIME_MS = 2000L;
    private static final float NOTE_HIT_Y_NORMALIZED = 0.75f;
    private static final double PERFECT_SCORE_EPSILON = 0.001;

    public static final class NoteWaveState {
        public final int stringIndex;
        public final float headYNormalized;
        public final float tailYNormalized;
        public final float intensity;

        public NoteWaveState(
                int stringIndex,
                float headYNormalized,
                float tailYNormalized,
                float intensity
        ) {
            this.stringIndex = stringIndex;
            this.headYNormalized = headYNormalized;
            this.tailYNormalized = tailYNormalized;
            this.intensity = intensity;
        }
    }

    private final MapFile mapFile;
    private final int stringCount;
    private final List<Note> notes = new ArrayList<Note>();
    private final ComboState comboState = new ComboState();

    private long gameTimeMs;
    private double score;

    public GameplaySession(MapFile mapFile, int stringCount) {
        this.mapFile = mapFile;
        this.stringCount = stringCount;
        reset();
    }

    public void reset() {
        gameTimeMs = 0L;
        score = 0d;
        comboState.reset();
        notes.clear();
        notes.addAll(mapFile.createRuntimeNotes());
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

    public Note registerStringHit(int stringIndex) {
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

        if (bestNote == null || bestScore <= 0d) {
            comboState.registerMiss();
            return null;
        }

        score += bestScore * comboState.getMultiplier();
        notes.remove(bestNote);

        if (isPerfectScore(bestScore)) {
            comboState.registerPerfect();
        }

        return bestNote;
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

    public float getCurrentTempoBpm() {
        return mapFile.getTempoBpmAtTimeMs(gameTimeMs);
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
        long travelDurationMs = Math.max(note.duration, 1L);
        long noteOffTimeMs = note.absoluteTime + travelDurationMs;
        long visibleStartTimeMs = note.absoluteTime - APPROACH_TIME_MS;
        long visibleEndTimeMs = noteOffTimeMs + travelDurationMs;
        if (gameTimeMs < visibleStartTimeMs || gameTimeMs > visibleEndTimeMs) {
            return null;
        }

        float headYNormalized = computeEventYNormalized(note.absoluteTime, travelDurationMs);
        float tailYNormalized = computeEventYNormalized(noteOffTimeMs, travelDurationMs);

        return new NoteWaveState(note.string, headYNormalized, tailYNormalized, 1f);
    }

    private float computeEventYNormalized(long eventTimeMs, long travelDurationMs) {
        long visibleStartTimeMs = eventTimeMs - APPROACH_TIME_MS;

        if (gameTimeMs <= eventTimeMs) {
            float preHitProgress =
                    (gameTimeMs - visibleStartTimeMs) / (float) APPROACH_TIME_MS;
            return clamp(preHitProgress * NOTE_HIT_Y_NORMALIZED);
        }

        float postHitProgress =
                (gameTimeMs - eventTimeMs) / (float) Math.max(travelDurationMs, 1L);
        return clamp(NOTE_HIT_Y_NORMALIZED + (postHitProgress * (1f - NOTE_HIT_Y_NORMALIZED)));
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(value, 1f));
    }

    private boolean isPerfectScore(double scoreValue) {
        return scoreValue >= (MAX_NOTE_SCORE - PERFECT_SCORE_EPSILON);
    }
}
