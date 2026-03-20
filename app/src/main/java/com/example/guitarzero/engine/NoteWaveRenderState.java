package com.example.guitarzero.engine;

public class NoteWaveRenderState {
    public final int stringIndex;
    public final float centerXNormalized;
    public final float laneWidthNormalized;
    public final float headYNormalized;
    public final float tailYNormalized;
    public final float intensity;

    public NoteWaveRenderState(
            int stringIndex,
            float centerXNormalized,
            float laneWidthNormalized,
            float headYNormalized,
            float tailYNormalized,
            float intensity
    ) {
        this.stringIndex = stringIndex;
        this.centerXNormalized = centerXNormalized;
        this.laneWidthNormalized = laneWidthNormalized;
        this.headYNormalized = headYNormalized;
        this.tailYNormalized = tailYNormalized;
        this.intensity = intensity;
    }
}
