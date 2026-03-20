package com.example.guitarzero.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class GameState {
    public enum ScreenState {
        IN_GAME,
        MAIN_MENU,
        CHOOSE_SONG
    }

    public static final class RopeRenderState {
        public final boolean visible;
        public final float translateX;
        public final float translateY;
        public final float scaleX;
        public final float scaleY;

        public RopeRenderState(
                boolean visible,
                float translateX,
                float translateY,
                float scaleX,
                float scaleY
        ) {
            this.visible = visible;
            this.translateX = translateX;
            this.translateY = translateY;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
        }
    }

    private static final float MAX_DELTA_TIME_SECONDS = 0.05f;
    private static final RopeRenderState HIDDEN_ROPE_RENDER_STATE =
            new RopeRenderState(false, 0f, 0f, 1f, 1f);
    private static final RopeRenderState IN_GAME_ROPE_RENDER_STATE =
            new RopeRenderState(true, 0f, 0f, 0.25f, 1f);

    private final String[] songs = {"Fuzz Intro", "Velvet Riff", "Arcade Solo"};
    private final String[] levels = {"Niveau 1", "Niveau 2", "Niveau 3"};

    private ScreenState currentScreen = ScreenState.MAIN_MENU;
    private int selectedSongIndex = 0;
    private int currentLevelIndex = 0;
    private float elapsedInGameSeconds = 0f;

    public GameState() {
    }

    public synchronized void update(float deltaTimeSeconds) {
        float clampedDeltaTime = Math.max(0f, Math.min(deltaTimeSeconds, MAX_DELTA_TIME_SECONDS));

        if (currentScreen == ScreenState.IN_GAME) {
            elapsedInGameSeconds += clampedDeltaTime;
        }
    }

    public synchronized void draw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);

        if (currentScreen != ScreenState.IN_GAME) {
            return;
        }

    }

    public synchronized void showMainMenu() {
        currentScreen = ScreenState.MAIN_MENU;
    }

    public synchronized void showChooseSong() {
        currentScreen = ScreenState.CHOOSE_SONG;
    }

    public synchronized void selectSong(int songIndex) {
        if (isValidSongIndex(songIndex)) {
            selectedSongIndex = songIndex;
        }
    }

    public synchronized void startGame(int songIndex) {
        selectSong(songIndex);

        currentScreen = ScreenState.IN_GAME;
        elapsedInGameSeconds = 0f;
    }

    public synchronized ScreenState getCurrentScreen() {
        return currentScreen;
    }

    public synchronized int getSelectedSongIndex() {
        return selectedSongIndex;
    }

    public synchronized int getSongCount() {
        return songs.length;
    }

    public synchronized String getSongLabel(int songIndex) {
        if (!isValidSongIndex(songIndex)) {
            throw new IllegalArgumentException("Invalid song index: " + songIndex);
        }

        return songs[songIndex];
    }

    public synchronized String getCurrentSongLabel() {
        return songs[selectedSongIndex];
    }

    public synchronized String getCurrentLevelLabel() {
        return levels[currentLevelIndex];
    }

    public synchronized RopeRenderState getRopeRenderState() {
        if (currentScreen == ScreenState.IN_GAME) {
            return IN_GAME_ROPE_RENDER_STATE;
        }

        return HIDDEN_ROPE_RENDER_STATE;
    }

    private boolean isValidSongIndex(int songIndex) {
        return songIndex >= 0 && songIndex < songs.length;
    }
}
