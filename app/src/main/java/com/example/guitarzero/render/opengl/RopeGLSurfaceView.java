package com.example.guitarzero.render.opengl;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;

import com.example.guitarzero.game.GameState;

public class RopeGLSurfaceView extends GLSurfaceView {
    public RopeGLSurfaceView(Context context, GameState gameState) {
        super(context);
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderMediaOverlay(true);
        setPreserveEGLContextOnPause(true);
        setRenderer(new RopeRenderer(
                context.getApplicationContext(),
                gameState,
                new ShaderPrograms(context.getApplicationContext())
        ));
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setInGameRendering(boolean enabled) {
        setRenderMode(enabled ? GLSurfaceView.RENDERMODE_CONTINUOUSLY : GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        if (enabled) {
            requestRender();
        }
    }
}
