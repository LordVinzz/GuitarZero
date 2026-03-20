uniform mat4 uMvpMatrix;
uniform float uOscillationTimeSeconds;
uniform float uOscillationAmplitude;
uniform float uOscillationAngularFrequency;
uniform float uOscillationDamping;
attribute vec4 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;

void main() {
    float anchorProfile = sin(aTexCoord.y * 3.14159265);
    float timeWave = sin(uOscillationTimeSeconds * uOscillationAngularFrequency);
    float dampingEnvelope = exp(-uOscillationDamping * uOscillationTimeSeconds);

    vec4 displacedPosition = aPosition;
    displacedPosition.x += anchorProfile * timeWave * dampingEnvelope * uOscillationAmplitude;

    gl_Position = uMvpMatrix * displacedPosition;
    vTexCoord = aTexCoord;
}
