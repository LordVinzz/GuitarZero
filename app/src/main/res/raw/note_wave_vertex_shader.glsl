uniform mat4 uMvpMatrix;
attribute vec4 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;

void main() {
    gl_Position = uMvpMatrix * aPosition;
    vTexCoord = aTexCoord;
}
