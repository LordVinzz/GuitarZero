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
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    private static final int RECTANGLE_SIZE = 100;
    private static final int X_MODULO = 300;

    private GameThread thread;
    private int x = 0;
    private final int y;

    private Bitmap backgroundBitmap;
    private List<Hitbox> hitboxes = new ArrayList<>();


    public GameView(Context context, int initialY) {
        super(context);
        y = initialY;
        getHolder().addCallback(this);
        thread = new GameThread(getHolder(), this);
        setFocusable(true);
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
    }


    /**
     * Update render
     *
     * @param canvas Update view canvas
     */
    @Override
    public void draw(Canvas canvas) {
        if (canvas == null) return;

        super.draw(canvas);
        if (backgroundBitmap != null)
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);

        Paint paint = new Paint();
        paint.setColor(Color.rgb(250, 0, 0));
        canvas.drawRect(x, y, x + RECTANGLE_SIZE, y + RECTANGLE_SIZE, paint);
    }

    /**
     * Touch listener for hitboxes
     *
     * @param event Finger touch even
     * @return A boolean
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        if (event.getAction() == MotionEvent.ACTION_DOWN) {
//            for (Hitbox hitbox : hitboxes) {
//                hitbox.handleTouch(event.getX(), event.getY());
//            }
//        }
//        return true;
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
}
