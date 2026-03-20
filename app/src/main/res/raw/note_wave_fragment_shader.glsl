precision mediump float;

uniform float uHeadYNormalized;
uniform float uTailYNormalized;
uniform float uWaveIntensity;
uniform vec3 uGlowColor;

varying vec2 vTexCoord;

void main() {
    float segmentStart = min(uHeadYNormalized, uTailYNormalized);
    float segmentEnd = max(uHeadYNormalized, uTailYNormalized);
    float edgeSoftness = 0.03;

    float segmentMask = smoothstep(segmentStart - edgeSoftness, segmentStart + edgeSoftness, vTexCoord.y)
        * (1.0 - smoothstep(segmentEnd - edgeSoftness, segmentEnd + edgeSoftness, vTexCoord.y));

    float headDistance = (vTexCoord.y - segmentEnd) / edgeSoftness;
    float tailDistance = (vTexCoord.y - segmentStart) / edgeSoftness;
    float headGlow = exp(-(headDistance * headDistance) * 6.0);
    float tailGlow = exp(-(tailDistance * tailDistance) * 6.0);
    float bodyGlow = max(segmentMask, max(headGlow, tailGlow));
    float horizontalGlow = 1.0 - smoothstep(0.2, 0.5, abs(vTexCoord.x - 0.5));
    float alpha = bodyGlow * horizontalGlow * uWaveIntensity * 0.85;
    gl_FragColor = vec4(uGlowColor * alpha, alpha);
}
