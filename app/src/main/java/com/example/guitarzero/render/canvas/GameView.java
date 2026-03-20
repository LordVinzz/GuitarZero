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

import com.example.guitarzero.Hitbox;
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
    private final List<Hitbox> hitboxes = new ArrayList<>();
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
        initHitboxes(width, height);
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

    private void initHitboxes(int width, int height) {
        hitboxes.clear();
        if (width <= 0 || height <= 0) {
            return;
        }

        final float hitboxWidth = width / 4f;
        final float hitboxHeight = height;

        hitboxes.add(new Hitbox(0, 0, 0, hitboxWidth, hitboxHeight));
        hitboxes.add(new Hitbox(1, hitboxWidth, 0, hitboxWidth * 2, hitboxHeight));
        hitboxes.add(new Hitbox(2, hitboxWidth * 2, 0, hitboxWidth * 3, hitboxHeight));
        hitboxes.add(new Hitbox(3, hitboxWidth * 3, 0, hitboxWidth * 4, hitboxHeight));
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
        notes.add(new Note(0, 2000, 1));
        notes.add(new Note(1, 4000, 1));
        notes.add(new Note(2, 6000, 1));
        notes.add(new Note(3, 8000, 1));
    }

    private boolean onTouchHitboxes(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                int pointerIndex = event.getActionIndex();
                float touchX = event.getX(pointerIndex);
                float touchY = event.getY(pointerIndex);
                for (Hitbox hitbox : hitboxes) {
                    hitbox.handleTouch(touchX, touchY);
                }
                break;
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameState.getCurrentScreen() != GameState.ScreenState.IN_GAME) {
            return false;
        }

        onTouchHitboxes(event);

        int action = event.getActionMasked();
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_POINTER_DOWN) {
            return true;
        }

        if (absoluteTime == 0) {
            return true;
        }

        Note bestNote = null;
        double bestScore = -1;

        for (Note note : notes) {
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
}
