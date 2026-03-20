package com.example.guitarzero.engine;

import android.content.res.Resources;
import android.graphics.Canvas;

public class GameEngine {
    private final StringRack stringRack;
    private final GameplaySession gameplaySession;
    private final CanvasGameRenderer canvasGameRenderer;

    private boolean wasInGame;

    public GameEngine(int stringCount) {
        stringRack = new StringRack(stringCount);
        gameplaySession = new GameplaySession(stringCount);
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
        return true;
    }

    public void draw(Canvas canvas, boolean inGame) {
        canvasGameRenderer.draw(canvas, inGame, gameplaySession);
    }

    public GuitarString.RenderState[] getGuitarStringRenderStates(boolean visible) {
        return stringRack.getRenderStates(visible);
    }

    public void resetRuntimeState() {
        gameplaySession.reset();
        stringRack.resetOscillations();
    }
}
