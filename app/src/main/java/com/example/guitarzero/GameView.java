package com.example.guitarzero;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private static final int RECTANGLE_SIZE = 100;
    private static final int X_MODULO = 300;
    private static final double MIN_SCORE_TO_HIT = 0.01;

    private GameThread thread;
    private int x = 0;
    private final int y;
    private long absoluteTime = 0;
    private long gameTime = 0;

    private List<Note> notes = new ArrayList<>();
    private double score = 0;

    private Bitmap backgroundBitmap;
    private List<Hitbox> hitboxes = new ArrayList<>();


    public GameView(Context context, int initialY) {
        super(context);
        y = initialY;
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);

        notes.add(new Note(0, 2000, 1));
        notes.add(new Note(1, 4000, 1));
        notes.add(new Note(2, 6000, 1));
        notes.add(new Note(3, 8000, 1));
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    private void initBackground() {
        backgroundBitmap = Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(getResources(), R.drawable.background),
                getWidth(),
                getHeight(),
                true
        );
    }

    private void initHitboxes() {
        // Add hitboxes
        final float hitboxWidth = getWidth() / 4f;
        final float hitboxHeight = getHeight();

        hitboxes.add(new Hitbox(0, 0, 0, hitboxWidth, hitboxHeight));
        hitboxes.add(new Hitbox(1, hitboxWidth, 0, hitboxWidth * 2, hitboxHeight));
        hitboxes.add(new Hitbox(2, hitboxWidth * 2, 0, hitboxWidth * 3, hitboxHeight));
        hitboxes.add(new Hitbox(3, hitboxWidth * 3, 0, hitboxWidth * 4, hitboxHeight));

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        initBackground();
        initHitboxes();

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

    /**
     * Update logic
     */
    public void update() {
        x = (x + 1) % X_MODULO;

        if (absoluteTime == 0) {
            return;
        }

        gameTime = (System.nanoTime() - absoluteTime) / 1_000_000;

        Iterator<Note> iterator = notes.iterator();
        while (iterator.hasNext()) {
            Note note = iterator.next();
            if (note.isExpired(gameTime)) {
                iterator.remove();
            }
        }
    }

    // ======== INPUT ========

      /**
     * Touch listener for hitboxes
     *
     * @param event Finger touch even
     * @return A boolean
     */
    private boolean onTouchHitboxes(MotionEvent event){
      int action = event.getActionMasked(); // not getAction for multitouch

        switch (action) {
            case MotionEvent.ACTION_DOWN: // first touch
            case MotionEvent.ACTION_POINTER_DOWN: // other touches
                int pointerIndex = event.getActionIndex();
                float x = event.getX(pointerIndex);
                float y = event.getY(pointerIndex);
                for (Hitbox hitbox : hitboxes) {
                    hitbox.handleTouch(x, y);
                }
                break;
        }
        return true;    
    }
  
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) {
            return true;
        }

        float touchX = event.getX();
        float touchY = event.getY();

        if (absoluteTime == 0 &&
                touchX >= 300 && touchX <= 600 &&
                touchY >= 600 && touchY <= 800) {

            absoluteTime = System.nanoTime();
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
        if (canvas == null) return;

        super.draw(canvas);
        if (backgroundBitmap != null)
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);

        Paint paint = new Paint();
        paint.setTextSize(50);
        paint.setColor(Color.RED);

        canvas.drawText("Time: " + gameTime, 50, 100, paint);
        canvas.drawText("Score: " + score, 50, 180, paint);
        canvas.drawText("Notes restantes: " + notes.size(), 50, 260, paint);

        // Bouton START si pas encore lancé
        if (absoluteTime == 0) {
            Paint buttonPaint = new Paint();
            buttonPaint.setColor(Color.BLUE);
            canvas.drawRect(300, 600, 600, 800, buttonPaint);

            buttonPaint.setColor(Color.RED);
            buttonPaint.setTextSize(50);
            canvas.drawText("START", 350, 720, buttonPaint);
        }
    }
}
