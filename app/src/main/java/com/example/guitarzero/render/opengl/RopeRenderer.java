package com.example.guitarzero.render.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.example.guitarzero.R;
import com.example.guitarzero.engine.GuitarString;
import com.example.guitarzero.engine.NoteWaveRenderState;
import com.example.guitarzero.game.GameState;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class RopeRenderer implements GLSurfaceView.Renderer {
    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int POSITION_COMPONENT_COUNT = 3;
    private static final int TEX_COORD_COMPONENT_COUNT = 2;
    private static final int STRIDE_BYTES =
            (POSITION_COMPONENT_COUNT + TEX_COORD_COMPONENT_COUNT) * FLOAT_SIZE_BYTES;
    private static final float FULL_SCREEN_HEIGHT_NDC = 2f;
    private static final int VERTICAL_SEGMENTS = 32;
    private static final float[][] STRING_COLORS = {
            {1.0f, 0.92f, 0.15f},
            {0.22f, 0.56f, 1.0f},
            {1.0f, 0.36f, 0.72f},
            {0.20f, 0.85f, 0.35f}
    };
    private static final float[] NOTE_WAVE_QUAD_VERTICES = {
            -0.5f, -0.5f, 0f, 0f, 1f,
             0.5f, -0.5f, 0f, 1f, 1f,
            -0.5f,  0.5f, 0f, 0f, 0f,
             0.5f,  0.5f, 0f, 1f, 0f
    };

    private final Context context;
    private final GameState gameState;
    private final ShaderPrograms shaderPrograms;
    private final FloatBuffer ropeVertexBuffer;
    private final FloatBuffer noteWaveVertexBuffer;
    private final float[] mvpMatrix = new float[16];
    private final int vertexCount;
    private final int noteWaveVertexCount;

    private int ropeProgram;
    private int ropePositionHandle;
    private int ropeTexCoordHandle;
    private int ropeMvpMatrixHandle;
    private int ropeTextureHandle;
    private int ropeTintColorHandle;
    private int ropeHighlightStrengthHandle;
    private int ropeOscillationTimeHandle;
    private int ropeOscillationAmplitudeHandle;
    private int ropeOscillationAngularFrequencyHandle;
    private int ropeOscillationDampingHandle;
    private int noteWaveProgram;
    private int noteWavePositionHandle;
    private int noteWaveTexCoordHandle;
    private int noteWaveMvpMatrixHandle;
    private int noteWaveHeadYHandle;
    private int noteWaveTailYHandle;
    private int noteWaveIntensityHandle;
    private int noteWaveColorHandle;
    private int textureId;
    private int viewWidth;
    private int viewHeight;
    private int textureWidth;
    private int textureHeight;

    public RopeRenderer(Context context, GameState gameState, ShaderPrograms shaderPrograms) {
        this.context = context;
        this.gameState = gameState;
        this.shaderPrograms = shaderPrograms;
        float[] vertices = createVertices();
        vertexCount = vertices.length / (POSITION_COMPONENT_COUNT + TEX_COORD_COMPONENT_COUNT);
        ropeVertexBuffer = ByteBuffer.allocateDirect(vertices.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        ropeVertexBuffer.put(vertices).position(0);

        noteWaveVertexCount =
                NOTE_WAVE_QUAD_VERTICES.length / (POSITION_COMPONENT_COUNT + TEX_COORD_COMPONENT_COUNT);
        noteWaveVertexBuffer = ByteBuffer.allocateDirect(
                NOTE_WAVE_QUAD_VERTICES.length * FLOAT_SIZE_BYTES
        )
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        noteWaveVertexBuffer.put(NOTE_WAVE_QUAD_VERTICES).position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glEnable(GLES20.GL_BLEND);

        shaderPrograms.reset();
        ropeProgram = shaderPrograms.getOrCreateProgram(
                "rope",
                R.raw.rope_vertex_shader,
                R.raw.rope_fragment_shader
        );
        ropePositionHandle = GLES20.glGetAttribLocation(ropeProgram, "aPosition");
        ropeTexCoordHandle = GLES20.glGetAttribLocation(ropeProgram, "aTexCoord");
        ropeMvpMatrixHandle = GLES20.glGetUniformLocation(ropeProgram, "uMvpMatrix");
        ropeTextureHandle = GLES20.glGetUniformLocation(ropeProgram, "uTexture");
        ropeTintColorHandle = GLES20.glGetUniformLocation(ropeProgram, "uTintColor");
        ropeHighlightStrengthHandle = GLES20.glGetUniformLocation(ropeProgram, "uHighlightStrength");
        ropeOscillationTimeHandle = GLES20.glGetUniformLocation(
                ropeProgram,
                "uOscillationTimeSeconds"
        );
        ropeOscillationAmplitudeHandle = GLES20.glGetUniformLocation(
                ropeProgram,
                "uOscillationAmplitude"
        );
        ropeOscillationAngularFrequencyHandle = GLES20.glGetUniformLocation(
                ropeProgram,
                "uOscillationAngularFrequency"
        );
        ropeOscillationDampingHandle = GLES20.glGetUniformLocation(
                ropeProgram,
                "uOscillationDamping"
        );

        noteWaveProgram = shaderPrograms.getOrCreateProgram(
                "noteWave",
                R.raw.note_wave_vertex_shader,
                R.raw.note_wave_fragment_shader
        );
        noteWavePositionHandle = GLES20.glGetAttribLocation(noteWaveProgram, "aPosition");
        noteWaveTexCoordHandle = GLES20.glGetAttribLocation(noteWaveProgram, "aTexCoord");
        noteWaveMvpMatrixHandle = GLES20.glGetUniformLocation(noteWaveProgram, "uMvpMatrix");
        noteWaveHeadYHandle = GLES20.glGetUniformLocation(noteWaveProgram, "uHeadYNormalized");
        noteWaveTailYHandle = GLES20.glGetUniformLocation(noteWaveProgram, "uTailYNormalized");
        noteWaveIntensityHandle = GLES20.glGetUniformLocation(noteWaveProgram, "uWaveIntensity");
        noteWaveColorHandle = GLES20.glGetUniformLocation(noteWaveProgram, "uGlowColor");
        textureId = loadTexture(context);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewWidth = width;
        viewHeight = height;
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (ropeProgram == 0 || textureId == 0 || viewWidth == 0 || viewHeight == 0) {
            return;
        }

        GuitarString.RenderState[] renderStates = gameState.getGuitarStringRenderStates();
        drawRopes(renderStates);
        drawNoteWaves(gameState.getNoteWaveRenderStates());
    }

    private void drawRopes(GuitarString.RenderState[] renderStates) {
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glUseProgram(ropeProgram);

        ropeVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(
                ropePositionHandle,
                POSITION_COMPONENT_COUNT,
                GLES20.GL_FLOAT,
                false,
                STRIDE_BYTES,
                ropeVertexBuffer
        );
        GLES20.glEnableVertexAttribArray(ropePositionHandle);

        ropeVertexBuffer.position(POSITION_COMPONENT_COUNT);
        GLES20.glVertexAttribPointer(
                ropeTexCoordHandle,
                TEX_COORD_COMPONENT_COUNT,
                GLES20.GL_FLOAT,
                false,
                STRIDE_BYTES,
                ropeVertexBuffer
        );
        GLES20.glEnableVertexAttribArray(ropeTexCoordHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(ropeTextureHandle, 0);

        for (GuitarString.RenderState renderState : renderStates) {
            if (!renderState.visible) {
                continue;
            }

            float ropeHeightNdc = FULL_SCREEN_HEIGHT_NDC * renderState.scaleY;
            float ropeWidthNdc = computeRopeWidthNdc(renderState, ropeHeightNdc);
            updateMvpMatrix(renderState, ropeWidthNdc, ropeHeightNdc);
            float[] color = getStringColor(renderState.stringIndex);
            GLES20.glUniformMatrix4fv(ropeMvpMatrixHandle, 1, false, mvpMatrix, 0);
            GLES20.glUniform3f(ropeTintColorHandle, color[0], color[1], color[2]);
            GLES20.glUniform1f(ropeHighlightStrengthHandle, renderState.highlightStrength);
            GLES20.glUniform1f(ropeOscillationTimeHandle, renderState.oscillationTimeSeconds);
            GLES20.glUniform1f(
                    ropeOscillationAmplitudeHandle,
                    renderState.displacementAmplitude
            );
            GLES20.glUniform1f(
                    ropeOscillationAngularFrequencyHandle,
                    renderState.oscillationAngularFrequency
            );
            GLES20.glUniform1f(ropeOscillationDampingHandle, renderState.oscillationDamping);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);
        }
    }

    private void drawNoteWaves(NoteWaveRenderState[] noteWaveStates) {
        if (noteWaveProgram == 0 || noteWaveStates.length == 0) {
            return;
        }

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE);
        GLES20.glUseProgram(noteWaveProgram);

        noteWaveVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(
                noteWavePositionHandle,
                POSITION_COMPONENT_COUNT,
                GLES20.GL_FLOAT,
                false,
                STRIDE_BYTES,
                noteWaveVertexBuffer
        );
        GLES20.glEnableVertexAttribArray(noteWavePositionHandle);

        noteWaveVertexBuffer.position(POSITION_COMPONENT_COUNT);
        GLES20.glVertexAttribPointer(
                noteWaveTexCoordHandle,
                TEX_COORD_COMPONENT_COUNT,
                GLES20.GL_FLOAT,
                false,
                STRIDE_BYTES,
                noteWaveVertexBuffer
        );
        GLES20.glEnableVertexAttribArray(noteWaveTexCoordHandle);

        for (NoteWaveRenderState noteWaveState : noteWaveStates) {
            updateNoteWaveMvpMatrix(noteWaveState);
            float[] color = getStringColor(noteWaveState.stringIndex);
            GLES20.glUniformMatrix4fv(noteWaveMvpMatrixHandle, 1, false, mvpMatrix, 0);
            GLES20.glUniform1f(noteWaveHeadYHandle, noteWaveState.headYNormalized);
            GLES20.glUniform1f(noteWaveTailYHandle, noteWaveState.tailYNormalized);
            GLES20.glUniform1f(noteWaveIntensityHandle, noteWaveState.intensity);
            GLES20.glUniform3f(noteWaveColorHandle, color[0], color[1], color[2]);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, noteWaveVertexCount);
        }

        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    private float computeRopeWidthNdc(GuitarString.RenderState renderState, float ropeHeightNdc) {
        float imageAspect = (float) textureWidth / (float) textureHeight;
        return ropeHeightNdc
                * imageAspect
                * ((float) viewHeight / (float) viewWidth)
                * renderState.scaleX;
    }

    private void updateMvpMatrix(GuitarString.RenderState renderState, float ropeWidthNdc, float ropeHeightNdc) {
        Matrix.setIdentityM(mvpMatrix, 0);

        float ropeCenterX = (renderState.centerXNormalized * 2f) - 1f;

        Matrix.translateM(
                mvpMatrix,
                0,
                ropeCenterX,
                0f,
                0f
        );
        Matrix.scaleM(mvpMatrix, 0, ropeWidthNdc, ropeHeightNdc, 1f);
    }

    private void updateNoteWaveMvpMatrix(NoteWaveRenderState noteWaveState) {
        Matrix.setIdentityM(mvpMatrix, 0);

        float laneCenterX = (noteWaveState.centerXNormalized * 2f) - 1f;
        float laneWidthNdc = noteWaveState.laneWidthNormalized * 2f;

        Matrix.translateM(mvpMatrix, 0, laneCenterX, 0f, 0f);
        Matrix.scaleM(mvpMatrix, 0, laneWidthNdc, FULL_SCREEN_HEIGHT_NDC, 1f);
    }

    private int loadTexture(Context context) {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        int generatedTextureId = textures[0];
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, generatedTextureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.corde, options);
        if (bitmap == null) {
            throw new IllegalStateException("Unable to decode corde.png.");
        }

        textureWidth = bitmap.getWidth();
        textureHeight = bitmap.getHeight();
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();

        return generatedTextureId;
    }

    private float[] createVertices() {
        int floatsPerVertex = POSITION_COMPONENT_COUNT + TEX_COORD_COMPONENT_COUNT;
        float[] vertices = new float[(VERTICAL_SEGMENTS + 1) * 2 * floatsPerVertex];
        int offset = 0;

        for (int row = 0; row <= VERTICAL_SEGMENTS; row++) {
            float t = row / (float) VERTICAL_SEGMENTS;
            float y = 0.5f - t;

            offset = writeVertex(vertices, offset, -0.5f, y, 0f, 0f, t);
            offset = writeVertex(vertices, offset, 0.5f, y, 0f, 1f, t);
        }

        return vertices;
    }

    private int writeVertex(float[] vertices, int offset, float x, float y, float z, float u, float v) {
        vertices[offset++] = x;
        vertices[offset++] = y;
        vertices[offset++] = z;
        vertices[offset++] = u;
        vertices[offset++] = v;
        return offset;
    }

    private float[] getStringColor(int stringIndex) {
        if (stringIndex < 0 || stringIndex >= STRING_COLORS.length) {
            return STRING_COLORS[0];
        }

        return STRING_COLORS[stringIndex];
    }
}
