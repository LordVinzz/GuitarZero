precision mediump float;

uniform float uWaveYNormalized;
uniform float uWaveRadius;
uniform float uWaveIntensity;
uniform vec3 uGlowColor;

varying vec2 vTexCoord;

void main() {
    float verticalDistance = (vTexCoord.y - uWaveYNormalized) / max(uWaveRadius, 0.0001);
    float waveBand = exp(-(verticalDistance * verticalDistance) * 10.0);
    float horizontalGlow = 1.0 - smoothstep(0.35, 0.5, abs(vTexCoord.x - 0.5));
    float alpha = waveBand * horizontalGlow * uWaveIntensity * 0.85;
    gl_FragColor = vec4(uGlowColor * alpha, alpha);
}
