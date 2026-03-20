package com.example.guitarzero.render.canvas;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

public class GameThread extends Thread {
    private static final long FRAME_DELAY_MS = 16L;
    private static final float DEFAULT_DELTA_TIME_SECONDS = FRAME_DELAY_MS / 1000f;

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
        long previousFrameTimeNanos = System.nanoTime();

        while (running) {
            long frameStartTimeNanos = System.nanoTime();
            float deltaTimeSeconds =
                    (frameStartTimeNanos - previousFrameTimeNanos) / 1_000_000_000f;
            if (deltaTimeSeconds <= 0f) {
                deltaTimeSeconds = DEFAULT_DELTA_TIME_SECONDS;
            }
            previousFrameTimeNanos = frameStartTimeNanos;

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
                long frameDurationMs = (System.nanoTime() - frameStartTimeNanos) / 1_000_000L;
                long remainingDelayMs = FRAME_DELAY_MS - frameDurationMs;
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
