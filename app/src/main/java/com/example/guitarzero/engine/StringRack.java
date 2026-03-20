package com.example.guitarzero.engine;

import java.util.ArrayList;
import java.util.List;

public class StringRack {
    private final List<GuitarString> guitarStrings;

    public StringRack(int stringCount) {
        guitarStrings = new ArrayList<GuitarString>(stringCount);
        for (int stringIndex = 0; stringIndex < stringCount; stringIndex++) {
            guitarStrings.add(new GuitarString(stringIndex));
        }
    }

    public void setLayout(int surfaceWidth, int surfaceHeight) {
        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            return;
        }

        float hitboxWidth = surfaceWidth / (float) guitarStrings.size();
        for (int stringIndex = 0; stringIndex < guitarStrings.size(); stringIndex++) {
            float hitboxLeft = stringIndex * hitboxWidth;
            float hitboxRight = hitboxLeft + hitboxWidth;
            guitarStrings.get(stringIndex).setHitboxBounds(
                    hitboxLeft,
                    0f,
                    hitboxRight,
                    surfaceHeight,
                    surfaceWidth
            );
        }
    }

    public void update(float deltaTimeSeconds) {
        for (GuitarString guitarString : guitarStrings) {
            guitarString.update(deltaTimeSeconds);
        }
    }

    public int handleTouch(float touchX, float touchY) {
        for (GuitarString guitarString : guitarStrings) {
            if (guitarString.handleTouch(touchX, touchY)) {
                return guitarString.getStringIndex();
            }
        }

        return -1;
    }

    public GuitarString.RenderState[] getRenderStates(boolean visible, float[] highlightStrengths) {
        GuitarString.RenderState[] renderStates = new GuitarString.RenderState[guitarStrings.size()];
        for (int stringIndex = 0; stringIndex < guitarStrings.size(); stringIndex++) {
            float highlightStrength = 0f;
            if (highlightStrengths != null && stringIndex < highlightStrengths.length) {
                highlightStrength = highlightStrengths[stringIndex];
            }
            renderStates[stringIndex] = guitarStrings.get(stringIndex).getRenderState(
                    visible,
                    highlightStrength
            );
        }
        return renderStates;
    }

    public float getCenterXNormalized(int stringIndex) {
        return guitarStrings.get(stringIndex).getCenterXNormalized();
    }

    public float getLaneWidthNormalized() {
        if (guitarStrings.isEmpty()) {
            return 0f;
        }

        return 1f / guitarStrings.size();
    }

    public void resetOscillations() {
        for (GuitarString guitarString : guitarStrings) {
            guitarString.resetOscillation();
        }
    }
}
