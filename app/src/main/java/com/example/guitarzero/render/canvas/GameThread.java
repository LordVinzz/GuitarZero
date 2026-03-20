package com.example.guitarzero.render.canvas;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

import com.example.guitarzero.engine.FrameTimer;

public class GameThread extends Thread {
    private static final long FRAME_DELAY_MS = 16L;

    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private final FrameTimer frameTimer = new FrameTimer(FRAME_DELAY_MS);
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
            long frameStartTimeNanos = System.nanoTime();
            float deltaTimeSeconds = frameTimer.getDeltaTimeSeconds(frameStartTimeNanos);

            Canvas canvas = null;

            try {
                canvas = surfaceHolder.lockCanvas();
                synchronized (surfaceHolder) {
                    gameView.update(deltaTimeSeconds);
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
                long remainingDelayMs = frameTimer.getRemainingDelayMs(frameStartTimeNanos);
                if (remainingDelayMs > 0L) {
                    Thread.sleep(remainingDelayMs);
                }
            } catch (InterruptedException e) {
                interrupt();
                return;
            }
        }
    }
}
