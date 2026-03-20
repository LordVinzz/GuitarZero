package com.example.guitarzero.render.canvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.guitarzero.Note;
import com.example.guitarzero.R;
import com.example.guitarzero.game.GameState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private static final double MIN_SCORE_TO_HIT = 0.01;

    private final GameState gameState;
    private GameThread thread;
    private long absoluteTime = 0;
    private long gameTime = 0;
    private GameState.ScreenState previousScreenState = GameState.ScreenState.MAIN_MENU;

    private final List<Note> notes = new ArrayList<>();
    private double score = 0;

    private Bitmap backgroundBitmap;
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameView(Context context, GameState gameState) {
        super(context);
        this.gameState = gameState;
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);
        hudPaint.setTextSize(50f);
        hudPaint.setColor(Color.RED);
        resetGameplayState();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        initBackground(width, height);
        gameState.setStringHitboxLayout(width, height);
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

        if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
            backgroundBitmap.recycle();
            backgroundBitmap = null;
        }

        thread = null;
    }

    public void update(float deltaTimeSeconds) {
        gameState.update(deltaTimeSeconds);
        GameState.ScreenState currentScreenState = gameState.getCurrentScreen();
        if (currentScreenState != GameState.ScreenState.IN_GAME) {
            if (previousScreenState == GameState.ScreenState.IN_GAME) {
                resetGameplayState();
            }
            previousScreenState = currentScreenState;
            return;
        }

        if (previousScreenState != GameState.ScreenState.IN_GAME || absoluteTime == 0L) {
            resetGameplayState();
            absoluteTime = System.nanoTime();
        }

        previousScreenState = currentScreenState;
        gameTime = (System.nanoTime() - absoluteTime) / 1_000_000;

        Iterator<Note> iterator = notes.iterator();
        while (iterator.hasNext()) {
            Note note = iterator.next();
            if (note.isExpired(gameTime)) {
                iterator.remove();
            }
        }
    }

    private void resetGameplayState() {
        absoluteTime = 0L;
        gameTime = 0L;
        score = 0d;
        notes.clear();
        for (int stringIndex = 0; stringIndex < GameState.STRING_COUNT; stringIndex++) {
            notes.add(new Note(stringIndex, 2000L * (stringIndex + 1), 1));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameState.getCurrentScreen() != GameState.ScreenState.IN_GAME) {
            return false;
        }

        int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_POINTER_DOWN) {
            return true;
        }

        int pointerIndex = event.getActionIndex();
        float touchX = event.getX(pointerIndex);
        float touchY = event.getY(pointerIndex);
        int touchedStringIndex = gameState.handleStringTouch(touchX, touchY);
        if (touchedStringIndex < 0 || absoluteTime == 0) {
            return true;
        }

        Note bestNote = null;
        double bestScore = -1;

        for (Note note : notes) {
            if (note.corde != touchedStringIndex) {
                continue;
            }

            double currentScore = note.evalScore(gameTime);

            if (currentScore > bestScore) {
                bestScore = currentScore;
                bestNote = note;
            }
        }

        if (bestNote != null && bestScore >= MIN_SCORE_TO_HIT) {
            score += bestScore;
            notes.remove(bestNote);
        }

        return true;
    }

    // ======== RENDER ========

    @Override
    public void draw(Canvas canvas) {
        if (canvas == null) {
            return;
        }

        super.draw(canvas);
        gameState.draw(canvas);
        if (gameState.getCurrentScreen() != GameState.ScreenState.IN_GAME) {
            return;
        }

        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }

        canvas.drawText("Time: " + gameTime, 50, 100, hudPaint);
        canvas.drawText("Score: " + score, 50, 180, hudPaint);
        canvas.drawText("Notes restantes: " + notes.size(), 50, 260, hudPaint);
    }

    private void initBackground(int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        Bitmap decodedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        if (decodedBitmap == null) {
            backgroundBitmap = null;
            return;
        }

        if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
            backgroundBitmap.recycle();
        }

        backgroundBitmap = Bitmap.createScaledBitmap(decodedBitmap, width, height, true);
        if (decodedBitmap != backgroundBitmap) {
            decodedBitmap.recycle();
        }
    }
}
