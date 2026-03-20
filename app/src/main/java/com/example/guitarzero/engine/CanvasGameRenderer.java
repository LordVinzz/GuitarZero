package com.example.guitarzero.engine;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.guitarzero.R;

public class CanvasGameRenderer {
    private Bitmap backgroundBitmap;
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public CanvasGameRenderer() {
        hudPaint.setTextSize(50f);
        hudPaint.setColor(Color.RED);
    }

    public void onSurfaceChanged(Resources resources, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }

        Bitmap decodedBitmap = BitmapFactory.decodeResource(resources, R.drawable.background);
        if (decodedBitmap == null) {
            backgroundBitmap = null;
            return;
        }

        release();
        backgroundBitmap = Bitmap.createScaledBitmap(decodedBitmap, width, height, true);
        if (decodedBitmap != backgroundBitmap) {
            decodedBitmap.recycle();
        }
    }

    public void draw(Canvas canvas, boolean inGame, GameplaySession gameplaySession) {
        canvas.drawColor(Color.WHITE);
        if (!inGame) {
            return;
        }

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
    }

    public void release() {
        if (backgroundBitmap != null && !backgroundBitmap.isRecycled()) {
            backgroundBitmap.recycle();
        }
        backgroundBitmap = null;
    }
}
