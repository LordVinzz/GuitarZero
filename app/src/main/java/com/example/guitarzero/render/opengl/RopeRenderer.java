package com.example.guitarzero.render.opengl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import com.example.guitarzero.R;
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

    private static final float[] VERTICES = {
            -0.5f, -0.5f, 0f, 0f, 1f,
             0.5f, -0.5f, 0f, 1f, 1f,
            -0.5f,  0.5f, 0f, 0f, 0f,
             0.5f,  0.5f, 0f, 1f, 0f
    };

    private final Context context;
    private final GameState gameState;
    private final ShaderPrograms shaderPrograms;
    private final FloatBuffer vertexBuffer;
    private final float[] mvpMatrix = new float[16];

    private int program;
    private int positionHandle;
    private int texCoordHandle;
    private int mvpMatrixHandle;
    private int textureHandle;
    private int textureId;
    private int viewWidth;
    private int viewHeight;
    private int textureWidth;
    private int textureHeight;

    public RopeRenderer(Context context, GameState gameState, ShaderPrograms shaderPrograms) {
        this.context = context;
        this.gameState = gameState;
        this.shaderPrograms = shaderPrograms;
        vertexBuffer = ByteBuffer.allocateDirect(VERTICES.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(VERTICES).position(0);
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

        GameState.RopeRenderState renderState = gameState.getRopeRenderState();
        if (!renderState.visible) {
            return;
        }

        GLES20.glUseProgram(program);
        updateMvpMatrix(renderState);

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

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    }

    private void updateMvpMatrix(GameState.RopeRenderState renderState) {
        Matrix.setIdentityM(mvpMatrix, 0);

        float ndcHeight = FULL_SCREEN_HEIGHT_NDC * renderState.scaleY;
        float imageAspect = (float) textureWidth / (float) textureHeight;
        float ndcWidth = ndcHeight
                * imageAspect
                * ((float) viewHeight / (float) viewWidth)
                * renderState.scaleX;

        Matrix.translateM(mvpMatrix, 0, renderState.translateX, renderState.translateY, 0f);
        Matrix.scaleM(mvpMatrix, 0, ndcWidth, ndcHeight, 1f);
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
}
