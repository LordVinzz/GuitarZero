package com.example.guitarzero;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

public class Hitbox {
    private int stringIndex;
    private final RectF rect;
    private final Runnable onHit;

    public Hitbox(int stringIndex, float left, float top, float right, float bottom) {
        this.stringIndex = stringIndex;
        this.rect = new RectF(left, top, right, bottom);
        this.onHit = () -> Log.d("Hitbox", "String " + stringIndex + " hit");
    }

    public boolean handleTouch(float x, float y) {
        if (rect.contains(x, y)) {
            if (onHit != null) onHit.run();
            return true;
        }
        return false;
    }

    public void drawDebug(Canvas canvas, Paint paint) {
        canvas.drawRect(rect, paint);
    }
}