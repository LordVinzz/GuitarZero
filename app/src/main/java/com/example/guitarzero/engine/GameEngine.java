package com.example.guitarzero.engine;

import android.content.res.Resources;
import android.graphics.Canvas;

import com.example.guitarzero.engine.map.MapFile;

public class GameEngine {
    private static final float COUNTDOWN_STEP_DURATION_MS = 1000f;
    private static final float COUNTDOWN_TOTAL_DURATION_MS = 4000f;

    private final StringRack stringRack;
    private final GameplaySession gameplaySession;
    private final CanvasGameRenderer canvasGameRenderer;
    private MapFile mapFile;

    private boolean wasInGame;
    private float countdownRemainingMs;
    private boolean backgroundAudioStarted;
    private float hitAudioPitchMultiplier = 1f;

    public GameEngine(int stringCount, MapFile mapFile) {
        this.mapFile = mapFile;
        stringRack = new StringRack(stringCount);
        gameplaySession = new GameplaySession(mapFile, stringCount);
        canvasGameRenderer = new CanvasGameRenderer();
    }

    public void update(float deltaTimeSeconds, boolean inGame) {
        if (!inGame) {
            if (wasInGame) {
                resetRuntimeState();
            }
            wasInGame = false;
            return;
        }

        if (!wasInGame) {
            beginGameplay();
        }

        if (countdownRemainingMs > 0f) {
            updateCountdown(deltaTimeSeconds);
            return;
        }

        stringRack.update(deltaTimeSeconds);
        gameplaySession.update(deltaTimeSeconds);
    }

    public void onSurfaceChanged(Resources resources, int width, int height) {
        canvasGameRenderer.onSurfaceChanged(resources, width, height);
        stringRack.setLayout(width, height);
    }

    public void onSurfaceDestroyed() {
        canvasGameRenderer.release();
    }

    public boolean handleTouch(float touchX, float touchY, boolean inGame) {
        if (!inGame) {
            return false;
        }

        if (countdownRemainingMs > 0f) {
            return true;
        }

        int touchedStringIndex = stringRack.handleTouch(touchX, touchY);
        if (touchedStringIndex < 0) {
            return true;
        }

        Note hitNote = gameplaySession.registerStringHit(touchedStringIndex);
        if (hitNote != null) {
            mapFile.playHitAudio(hitNote);
        }
        return true;
    }

    public void draw(Canvas canvas, boolean inGame) {
        canvasGameRenderer.draw(canvas, inGame, gameplaySession);
    }

    public double getScore() {
        return gameplaySession.getScore();
    }

    public GuitarString.RenderState[] getGuitarStringRenderStates(boolean visible) {
        float[] highlightStrengths = visible ? gameplaySession.getStringHighlightStrengths() : null;
        int comboTokens = visible ? gameplaySession.getComboTokens() : 0;
        return stringRack.getRenderStates(visible, highlightStrengths, comboTokens);
    }

    public NoteWaveRenderState[] getNoteWaveRenderStates(boolean visible) {
        if (!visible) {
            return new NoteWaveRenderState[0];
        }

        GameplaySession.NoteWaveState[] waveStates = gameplaySession.getNoteWaveStates();
        NoteWaveRenderState[] renderStates = new NoteWaveRenderState[waveStates.length];
        float laneWidthNormalized = stringRack.getLaneWidthNormalized();

        for (int index = 0; index < waveStates.length; index++) {
            GameplaySession.NoteWaveState waveState = waveStates[index];
            renderStates[index] = new NoteWaveRenderState(
                    waveState.stringIndex,
                    stringRack.getCenterXNormalized(waveState.stringIndex),
                    laneWidthNormalized,
                    waveState.headYNormalized,
                    waveState.tailYNormalized,
                    waveState.intensity
            );
        }

        return renderStates;
    }

    public void resetRuntimeState() {
        gameplaySession.reset();
        stringRack.resetOscillations();
        countdownRemainingMs = COUNTDOWN_TOTAL_DURATION_MS;
        backgroundAudioStarted = false;
    }

    public void beginGameplay() {
        resetRuntimeState();
        wasInGame = true;
    }

    public void setMapFile(MapFile mapFile) {
        if (this.mapFile == mapFile) {
            mapFile.setHitAudioPitch(hitAudioPitchMultiplier);
            return;
        }

        stopMapAudio();
        this.mapFile = mapFile;
        this.mapFile.setHitAudioPitch(hitAudioPitchMultiplier);
        gameplaySession.setMapFile(mapFile);
        stringRack.resetOscillations();
        countdownRemainingMs = 0f;
    }

    public void startMapAudio() {
        if (backgroundAudioStarted) {
            return;
        }

        mapFile.startBackgroundAudio(gameplaySession.getPreviewStartTimeMs());
        backgroundAudioStarted = true;
    }

    public void stopMapAudio() {
        mapFile.stopAllAudio();
        backgroundAudioStarted = false;
    }

    public void setHitAudioPitch(float pitch) {
        hitAudioPitchMultiplier = pitch;
        mapFile.setHitAudioPitch(pitch);
    }

    public boolean isCountdownActive() {
        return countdownRemainingMs > 0f;
    }

    public String getCountdownLabel() {
        if (!isCountdownActive()) {
            return "";
        }

        if (countdownRemainingMs > (COUNTDOWN_STEP_DURATION_MS * 3f)) {
            return "3";
        }
        if (countdownRemainingMs > (COUNTDOWN_STEP_DURATION_MS * 2f)) {
            return "2";
        }
        if (countdownRemainingMs > COUNTDOWN_STEP_DURATION_MS) {
            return "1";
        }
        return "GO";
    }

    private void updateCountdown(float deltaTimeSeconds) {
        countdownRemainingMs = Math.max(
                0f,
                countdownRemainingMs - (deltaTimeSeconds * 1000f)
        );

        if (countdownRemainingMs <= 0f) {
            startMapAudio();
        }
    }
}
