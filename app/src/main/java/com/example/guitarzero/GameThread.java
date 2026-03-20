package com.example.guitarzero;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread {
    private static final long FRAME_DELAY_MS = 16L;

    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private volatile boolean running;

    public GameThread(SurfaceHolder surfaceHolder, GameView gameView) {
        super();
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
    }

    public void setRunning(boolean isRunning) {
        running = isRunning;
    }

    @Override
    public void run() {
        while (running) {
            Canvas canvas = null;

            try {
                canvas = surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    gameView.update();
                    gameView.draw(canvas);
                }
            } catch (Exception ignored) {
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                // Keep the loop close to 60 refreshes per second.
                Thread.sleep(FRAME_DELAY_MS);
            } catch (InterruptedException e) {
                interrupt();
                return;
            }
        }
    }
}
