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

    private final Context context;
    private final GameState gameState;
    private final ShaderPrograms shaderPrograms;
    private final FloatBuffer vertexBuffer;
    private final float[] mvpMatrix = new float[16];
    private final int vertexCount;

    private int program;
    private int positionHandle;
    private int texCoordHandle;
    private int mvpMatrixHandle;
    private int textureHandle;
    private int oscillationTimeHandle;
    private int oscillationAmplitudeHandle;
    private int oscillationAngularFrequencyHandle;
    private int oscillationDampingHandle;
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
        vertexBuffer = ByteBuffer.allocateDirect(vertices.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(vertices).position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0f, 0f, 0f, 0f);
        GLES20.glDisable(GLES20.GL_BLEND);

        shaderPrograms.reset();
        program = shaderPrograms.getOrCreateProgram(
                "rope",
                R.raw.rope_vertex_shader,
                R.raw.rope_fragment_shader
        );
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition");
        texCoordHandle = GLES20.glGetAttribLocation(program, "aTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMvpMatrix");
        textureHandle = GLES20.glGetUniformLocation(program, "uTexture");
        oscillationTimeHandle = GLES20.glGetUniformLocation(program, "uOscillationTimeSeconds");
        oscillationAmplitudeHandle = GLES20.glGetUniformLocation(program, "uOscillationAmplitude");
        oscillationAngularFrequencyHandle = GLES20.glGetUniformLocation(
                program,
                "uOscillationAngularFrequency"
        );
        oscillationDampingHandle = GLES20.glGetUniformLocation(program, "uOscillationDamping");
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

        if (program == 0 || textureId == 0 || viewWidth == 0 || viewHeight == 0) {
            return;
        }

        GuitarString.RenderState[] renderStates = gameState.getGuitarStringRenderStates();
        if (renderStates.length == 0) {
            return;
        }

        GLES20.glUseProgram(program);

        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(
                positionHandle,
                POSITION_COMPONENT_COUNT,
                GLES20.GL_FLOAT,
                false,
                STRIDE_BYTES,
                vertexBuffer
        );
        GLES20.glEnableVertexAttribArray(positionHandle);

        vertexBuffer.position(POSITION_COMPONENT_COUNT);
        GLES20.glVertexAttribPointer(
                texCoordHandle,
                TEX_COORD_COMPONENT_COUNT,
                GLES20.GL_FLOAT,
                false,
                STRIDE_BYTES,
                vertexBuffer
        );
        GLES20.glEnableVertexAttribArray(texCoordHandle);

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glUniform1i(textureHandle, 0);

        for (GuitarString.RenderState renderState : renderStates) {
            if (!renderState.visible) {
                continue;
            }

            float ropeHeightNdc = FULL_SCREEN_HEIGHT_NDC * renderState.scaleY;
            float ropeWidthNdc = computeRopeWidthNdc(renderState, ropeHeightNdc);
            updateMvpMatrix(renderState, ropeWidthNdc, ropeHeightNdc);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
            GLES20.glUniform1f(oscillationTimeHandle, renderState.oscillationTimeSeconds);
            GLES20.glUniform1f(oscillationAmplitudeHandle, renderState.displacementAmplitude);
            GLES20.glUniform1f(
                    oscillationAngularFrequencyHandle,
                    renderState.oscillationAngularFrequency
            );
            GLES20.glUniform1f(oscillationDampingHandle, renderState.oscillationDamping);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexCount);
        }
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
}
