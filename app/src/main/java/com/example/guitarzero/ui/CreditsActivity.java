package com.example.guitarzero.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.guitarzero.R;

public class CreditsActivity extends Activity implements SensorEventListener {
    private static final float MAX_ROTATION_SPEED = 6f;
    private static final float MAX_ANGULAR_ACCELERATION = 20f;
    private static final float SMOOTHING_FACTOR = 0.18f;

    private View creditsRootView;
    private View creditsCardView;
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private float smoothedMotionIntensity;
    private float previousRotationSpeed;
    private long previousTimestampNs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_credits);
        creditsRootView = findViewById(R.id.credits_root);
        creditsCardView = findViewById(R.id.credits_card);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }

        bindLink(R.id.text_loan_link);
        bindLink(R.id.text_vincent_link);
        bindLink(R.id.text_nabil_link);
        findViewById(R.id.button_back_from_credits).setOnClickListener(view -> finish());
        updateBackgroundPalette(0f, 0f, 0f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onPause();
    }

    private void bindLink(int textViewId) {
        TextView textView = findViewById(textViewId);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_GYROSCOPE) {
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float rotationSpeed = (float) Math.sqrt((x * x) + (y * y) + (z * z));

        float angularAcceleration = 0f;
        if (previousTimestampNs != 0L) {
            float deltaTimeSeconds = (event.timestamp - previousTimestampNs) / 1_000_000_000f;
            if (deltaTimeSeconds > 0f) {
                angularAcceleration = Math.abs(rotationSpeed - previousRotationSpeed) / deltaTimeSeconds;
            }
        }

        previousRotationSpeed = rotationSpeed;
        previousTimestampNs = event.timestamp;

        float normalizedSpeed = clamp(rotationSpeed / MAX_ROTATION_SPEED);
        float normalizedAcceleration = clamp(angularAcceleration / MAX_ANGULAR_ACCELERATION);
        float targetIntensity = clamp((normalizedSpeed * 0.45f) + (normalizedAcceleration * 0.55f));
        smoothedMotionIntensity += (targetIntensity - smoothedMotionIntensity) * SMOOTHING_FACTOR;

        updateBackgroundPalette(x, y, z);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void updateBackgroundPalette(float x, float y, float z) {
        if (creditsRootView == null) {
            return;
        }

        float hue = wrapHue(210f + (x * 42f) + (y * 28f) + (z * 18f));
        float accentHue = wrapHue(hue + 55f + (z * 14f));
        float topSaturation = 0.10f + (smoothedMotionIntensity * 0.48f);
        float bottomSaturation = 0.18f + (smoothedMotionIntensity * 0.58f);
        float topValue = 1.0f;
        float bottomValue = 0.92f - (smoothedMotionIntensity * 0.12f);

        int topColor = Color.HSVToColor(new float[] {hue, topSaturation, topValue});
        int bottomColor = Color.HSVToColor(new float[] {accentHue, bottomSaturation, bottomValue});
        GradientDrawable backgroundDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] {topColor, bottomColor}
        );
        creditsRootView.setBackground(backgroundDrawable);

        if (creditsCardView != null) {
            int cardColor = Color.HSVToColor(new float[] {
                    hue,
                    0.04f + (smoothedMotionIntensity * 0.12f),
                    1.0f
            });
            creditsCardView.setBackgroundColor(cardColor);
        }
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(value, 1f));
    }

    private float wrapHue(float hue) {
        float wrappedHue = hue % 360f;
        if (wrappedHue < 0f) {
            wrappedHue += 360f;
        }
        return wrappedHue;
    }
}
