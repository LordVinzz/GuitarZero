package com.example.guitarzero.engine;

public class GuitarString {
    private static final float DEFAULT_SCALE_X = 0.25f;
    private static final float DEFAULT_SCALE_Y = 1f;
    private static final float BASE_DISPLACEMENT = 1.0f;
    private static final float ANGULAR_FREQUENCY_RADIANS = 28f;
    private static final float DAMPING = 4.5f;
    private static final float STOP_ENVELOPE_THRESHOLD = 0.01f;

    public static final class RenderState {
        public final boolean visible;
        public final float centerXNormalized;
        public final float scaleX;
        public final float scaleY;
        public final float oscillationTimeSeconds;
        public final float displacementAmplitude;
        public final float oscillationAngularFrequency;
        public final float oscillationDamping;

        public RenderState(
                boolean visible,
                float centerXNormalized,
                float scaleX,
                float scaleY,
                float oscillationTimeSeconds,
                float displacementAmplitude,
                float oscillationAngularFrequency,
                float oscillationDamping
        ) {
            this.visible = visible;
            this.centerXNormalized = centerXNormalized;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.oscillationTimeSeconds = oscillationTimeSeconds;
            this.displacementAmplitude = displacementAmplitude;
            this.oscillationAngularFrequency = oscillationAngularFrequency;
            this.oscillationDamping = oscillationDamping;
        }
    }

    private final int stringIndex;
    private final Hitbox hitbox;

    private float centerXNormalized = 0.5f;
    private float oscillationTimeSeconds = 0f;
    private boolean oscillating = false;

    public GuitarString(int stringIndex) {
        this.stringIndex = stringIndex;
        this.hitbox = new Hitbox(stringIndex, 0f, 0f, 0f, 0f);
    }

    public int getStringIndex() {
        return stringIndex;
    }

    public void setHitboxBounds(float left, float top, float right, float bottom, float surfaceWidth) {
        hitbox.setBounds(left, top, right, bottom);
        if (surfaceWidth > 0f) {
            centerXNormalized = hitbox.getCenterX() / surfaceWidth;
        }
    }

    public boolean handleTouch(float x, float y) {
        if (!hitbox.contains(x, y)) {
            return false;
        }

        pluck();
        return true;
    }

    public void update(float deltaTimeSeconds) {
        if (!oscillating) {
            return;
        }

        oscillationTimeSeconds += deltaTimeSeconds;
        float envelope = (float) Math.exp(-DAMPING * oscillationTimeSeconds);
        if (envelope <= STOP_ENVELOPE_THRESHOLD) {
            oscillating = false;
            oscillationTimeSeconds = 0f;
        }
    }

    public void resetOscillation() {
        oscillating = false;
        oscillationTimeSeconds = 0f;
    }

    public RenderState getRenderState(boolean visible) {
        return new RenderState(
                visible,
                centerXNormalized,
                DEFAULT_SCALE_X,
                DEFAULT_SCALE_Y,
                oscillationTimeSeconds,
                oscillating ? BASE_DISPLACEMENT : 0f,
                ANGULAR_FREQUENCY_RADIANS,
                DAMPING
        );
    }

    private void pluck() {
        oscillating = true;
        oscillationTimeSeconds = 0f;
    }
}
