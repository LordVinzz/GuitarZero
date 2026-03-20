package com.example.guitarzero.engine;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.guitarzero.R;

public class CanvasGameRenderer {
    private Bitmap[] backgroundBitmaps;
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CanvasGameRenderer() {
        hudPaint.setTextSize(50f);
        hudPaint.setColor(Color.RED);
    }

    public void onSurfaceChanged(Resources resources, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        release();
        backgroundBitmaps = new Bitmap[] {
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
}
