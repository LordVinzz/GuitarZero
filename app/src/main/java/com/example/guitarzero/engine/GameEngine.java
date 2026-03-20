package com.example.guitarzero.engine;

import android.content.res.Resources;
import android.graphics.Canvas;

import com.example.guitarzero.engine.map.MapFile;

public class GameEngine {
    private final StringRack stringRack;
    private final GameplaySession gameplaySession;
    private final CanvasGameRenderer canvasGameRenderer;
    private final MapFile mapFile;

    private boolean wasInGame;

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
            resetRuntimeState();
            wasInGame = true;
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

        int touchedStringIndex = stringRack.handleTouch(touchX, touchY);
        if (touchedStringIndex < 0) {
            return true;
        }

        gameplaySession.registerStringHit(touchedStringIndex);
        mapFile.playHitAudio();
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
    }

    public void startMapAudio() {
        mapFile.startBackgroundAudio();
    }

    public void stopMapAudio() {
        mapFile.stopAllAudio();
    }

    public void setHitAudioPitch(float pitch) {
        mapFile.setHitAudioPitch(pitch);
    }
}
