package com.example.guitarzero.engine;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class Hitbox {
    private final int stringIndex;
    private final RectF rect;

    public Hitbox(int stringIndex, float left, float top, float right, float bottom) {
        this.stringIndex = stringIndex;
        this.rect = new RectF(left, top, right, bottom);
    }

    public int getStringIndex() {
        return stringIndex;
    }

    public void setBounds(float left, float top, float right, float bottom) {
        rect.set(left, top, right, bottom);
    }

    public boolean contains(float x, float y) {
        return rect.contains(x, y);
    }

    public float getCenterX() {
        return rect.centerX();
    }

    public float getCenterY() {
        return rect.centerY();
    }

    public void drawDebug(Canvas canvas, Paint paint) {
        canvas.drawRect(rect, paint);
    }
}
