package com.example.guitarzero.render.opengl;

import android.content.Context;
import android.opengl.GLES20;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ShaderPrograms {
    private final Context context;
    private final Map<String, Integer> programs = new HashMap<String, Integer>();

    public ShaderPrograms(Context context) {
        this.context = context.getApplicationContext();
    }

    public int getOrCreateProgram(String key, int vertexResId, int fragmentResId) {
        Integer cachedProgram = programs.get(key);
        if (cachedProgram != null) {
            return cachedProgram;
        }

        int program = createProgram(
                readRawResource(vertexResId),
                readRawResource(fragmentResId)
        );
        programs.put(key, program);
        return program;
    }

    public void reset() {
        programs.clear();
    }

    private String readRawResource(int resId) {
        StringBuilder builder = new StringBuilder();

        try {
            InputStream inputStream = context.getResources().openRawResource(resId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read shader resource " + resId, e);
        }

        return builder.toString();
    }

    private int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        int createdProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(createdProgram, vertexShader);
        GLES20.glAttachShader(createdProgram, fragmentShader);
        GLES20.glLinkProgram(createdProgram);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(createdProgram, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] == 0) {
            String error = GLES20.glGetProgramInfoLog(createdProgram);
            GLES20.glDeleteProgram(createdProgram);
            throw new IllegalStateException("Program link failed: " + error);
        }

        return createdProgram;
    }

    private int compileShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            String error = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed: " + error);
        }

        return shader;
    }
}
