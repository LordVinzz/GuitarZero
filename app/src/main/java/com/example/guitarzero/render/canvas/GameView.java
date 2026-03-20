package com.example.guitarzero.render.canvas;

import android.content.Context;
import android.graphics.Canvas;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.guitarzero.game.GameState;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private final GameState gameState;
    private GameThread thread;

    public GameView(Context context, GameState gameState) {
        super(context);
        this.gameState = gameState;
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (thread == null) {
            thread = new GameThread(getHolder(), this);
        }

        if (!thread.isAlive()) {
            thread.setRunning(true);
            thread.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (thread == null) {
            return;
        }

        boolean retry = true;
        thread.setRunning(false);

        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                retry = false;
            }
        }

        thread = null;
    }

    public void update(float deltaTimeSeconds) {
        gameState.update(deltaTimeSeconds);
    }

    @Override
    public void draw(Canvas canvas) {
        if (canvas == null) {
            return;
        }

        super.draw(canvas);
        gameState.draw(canvas);
    }
}
