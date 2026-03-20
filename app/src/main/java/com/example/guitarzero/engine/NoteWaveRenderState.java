package com.example.guitarzero.engine;

public class NoteWaveRenderState {
    public final int stringIndex;
    public final float centerXNormalized;
    public final float laneWidthNormalized;
    public final float waveYNormalized;
    public final float intensity;

    public NoteWaveRenderState(
            int stringIndex,
            float centerXNormalized,
            float laneWidthNormalized,
            float waveYNormalized,
            float intensity
    ) {
        this.stringIndex = stringIndex;
        this.centerXNormalized = centerXNormalized;
        this.laneWidthNormalized = laneWidthNormalized;
        this.waveYNormalized = waveYNormalized;
        this.intensity = intensity;
    }
}
