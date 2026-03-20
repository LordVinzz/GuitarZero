package com.example.guitarzero.engine;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.example.guitarzero.R;

public class CanvasGameRenderer {
    private Bitmap[] backgroundBitmaps;
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private HitResult lastHitResult;
    private long hitResultTimestamp;
    private static final long HIT_DISPLAY_DURATION = 800;

    public CanvasGameRenderer() {
        hudPaint.setTextSize(50f);
        hudPaint.setColor(Color.RED);
    }

    public void onSurfaceChanged(Resources resources, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        release();
        backgroundBitmaps = new Bitmap[]{
                loadScaledBitmap(resources, R.drawable.background, width, height),
                loadScaledBitmap(resources, R.drawable.background_boost_1, width, height),
                loadScaledBitmap(resources, R.drawable.background_boost_2, width, height)
        };
    }

    public void draw(Canvas canvas, boolean inGame, GameplaySession gameplaySession) {
        canvas.drawColor(Color.WHITE);
        if (!inGame) {
            return;
        }

        Bitmap backgroundBitmap = getBackgroundBitmap(gameplaySession.getComboMultiplier());
        if (backgroundBitmap != null) {
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        }

        canvas.drawText("Time: " + gameplaySession.getGameTimeMs(), 50, 100, hudPaint);
        canvas.drawText("Score: " + gameplaySession.getScore(), 50, 180, hudPaint);
        canvas.drawText(
                "Notes restantes: " + gameplaySession.getRemainingNotesCount(),
                50,
                260,
                hudPaint
        );
        canvas.drawText("Combo: x" + gameplaySession.getComboMultiplier(), 50, 340, hudPaint);
        canvas.drawText("Jetons: " + gameplaySession.getComboTokens() + "/10", 50, 420, hudPaint);

        // draw hit accuracy text indicator
        if (lastHitResult != null) {
            if (System.currentTimeMillis() - hitResultTimestamp < HIT_DISPLAY_DURATION) {
                drawHitResult(canvas);
            } else lastHitResult = null;
        }
    }

    public void release() {
        if (backgroundBitmaps == null) {
            return;
        }

        for (Bitmap backgroundBitmap : backgroundBitmaps) {
            if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
                backgroundBitmap.recycle();
            }
        }
        backgroundBitmaps = null;
    }

    private Bitmap loadScaledBitmap(Resources resources, int drawableResId, int width, int height) {
        Bitmap decodedBitmap = BitmapFactory.decodeResource(resources, drawableResId);
        if (decodedBitmap == null) {
            return null;
        }

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(decodedBitmap, width, height, true);
        if (decodedBitmap != scaledBitmap) {
            decodedBitmap.recycle();
        }
        return scaledBitmap;
    }

    private Bitmap getBackgroundBitmap(int comboMultiplier) {
        if (backgroundBitmaps == null || backgroundBitmaps.length == 0) {
            return null;
        }

        if (comboMultiplier <= 1) {
            return backgroundBitmaps[0];
        }

        if (comboMultiplier == 2 && backgroundBitmaps.length > 1) {
            return backgroundBitmaps[1];
        }

        return backgroundBitmaps[Math.min(2, backgroundBitmaps.length - 1)];
    }


    public void showHitResult(HitResult result) {
        lastHitResult = result;
        hitResultTimestamp = System.currentTimeMillis();
    }

    private void drawHitResult(Canvas canvas) {
        if (lastHitResult == null) return;
        if (System.currentTimeMillis() - hitResultTimestamp > HIT_DISPLAY_DURATION) {
            lastHitResult = null;
            return;
        }

        // Compute opacity: fade out at the end
        float elapsed = System.currentTimeMillis() - hitResultTimestamp;
        float alpha = 1f - (elapsed / HIT_DISPLAY_DURATION);

        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setAntiAlias(true);

        String text;
        switch (lastHitResult) {
            case PERFECT:
                text = "PERFECT";
                paint.setTextSize(80f);
                paint.setColor(Color.argb((int) (alpha * 255), 255, 223, 0)); // doré
                // Glow effect
                paint.setShadowLayer(20f, 0f, 0f, Color.argb((int) (alpha * 200), 255, 200, 0));
                break;
            case GOOD:
                text = "GOOD";
                paint.setTextSize(70f);
                paint.setColor(Color.argb((int) (alpha * 255), 100, 220, 255)); // bleu clair
                paint.setShadowLayer(15f, 0f, 0f, Color.argb((int) (alpha * 200), 50, 180, 255));
                break;
            case MISS:
                text = "MISS";
                paint.setTextSize(70f);
                paint.setColor(Color.argb((int) (alpha * 255), 255, 80, 80)); // rouge
                paint.setShadowLayer(15f, 0f, 0f, Color.argb((int) (alpha * 200), 200, 0, 0));
                break;
            default:
                return;
        }

        float x = canvas.getWidth() / 2f;
        float y = canvas.getHeight() - 150f;

        canvas.drawText(text, x, y, paint);
    }
}
