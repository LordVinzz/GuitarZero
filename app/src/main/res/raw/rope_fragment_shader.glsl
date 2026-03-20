precision mediump float;
uniform sampler2D uTexture;
uniform vec3 uTintColor;
uniform float uHighlightStrength;
varying vec2 vTexCoord;

void main() {
    vec4 baseColor = texture2D(uTexture, vTexCoord);
    float strength = clamp(uHighlightStrength, 0.0, 1.0);
    float tintMix = strength * 0.5;
    vec3 litColor = mix(baseColor.rgb, uTintColor, tintMix);
    gl_FragColor = vec4(litColor, baseColor.a);
}
