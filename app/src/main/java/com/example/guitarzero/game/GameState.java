package com.example.guitarzero.game;

import android.graphics.Canvas;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public static final int STRING_COUNT = 4;

    private static final float MAX_DELTA_TIME_SECONDS = 0.05f;

    public enum ScreenState {
        IN_GAME,
        MAIN_MENU,
        CHOOSE_SONG
    }

    private final String[] songs = {"Fuzz Intro", "Velvet Riff", "Arcade Solo"};
    private final String[] levels = {"Niveau 1", "Niveau 2", "Niveau 3"};
    private final List<GuitarString> guitarStrings = new ArrayList<GuitarString>(STRING_COUNT);

    private ScreenState currentScreen = ScreenState.MAIN_MENU;
    private int selectedSongIndex = 0;
    private int currentLevelIndex = 0;
    private float elapsedInGameSeconds = 0f;

    public GameState() {
        for (int stringIndex = 0; stringIndex < STRING_COUNT; stringIndex++) {
            guitarStrings.add(new GuitarString(stringIndex));
        }
    }

    public synchronized void update(float deltaTimeSeconds) {
        float clampedDeltaTime = Math.max(0f, Math.min(deltaTimeSeconds, MAX_DELTA_TIME_SECONDS));

        if (currentScreen == ScreenState.IN_GAME) {
            elapsedInGameSeconds += clampedDeltaTime;

            for (GuitarString guitarString : guitarStrings) {
                guitarString.update(clampedDeltaTime);
            }
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
        resetStringOscillations();
    }

    public synchronized void showChooseSong() {
        currentScreen = ScreenState.CHOOSE_SONG;
        resetStringOscillations();
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
        resetStringOscillations();
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

    public synchronized void setStringHitboxLayout(int surfaceWidth, int surfaceHeight) {
        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            return;
        }

        float hitboxWidth = surfaceWidth / (float) STRING_COUNT;
        for (int stringIndex = 0; stringIndex < guitarStrings.size(); stringIndex++) {
            float hitboxLeft = stringIndex * hitboxWidth;
            float hitboxRight = hitboxLeft + hitboxWidth;
            guitarStrings.get(stringIndex).setHitboxBounds(
                    hitboxLeft,
                    0f,
                    hitboxRight,
                    surfaceHeight,
                    surfaceWidth
            );
        }
    }

    public synchronized int handleStringTouch(float touchX, float touchY) {
        if (currentScreen != ScreenState.IN_GAME) {
            return -1;
        }

        for (GuitarString guitarString : guitarStrings) {
            if (guitarString.handleTouch(touchX, touchY)) {
                return guitarString.getStringIndex();
            }
        }

        return -1;
    }

    public synchronized GuitarString.RenderState[] getGuitarStringRenderStates() {
        boolean visible = currentScreen == ScreenState.IN_GAME;
        GuitarString.RenderState[] renderStates = new GuitarString.RenderState[guitarStrings.size()];
        for (int stringIndex = 0; stringIndex < guitarStrings.size(); stringIndex++) {
            renderStates[stringIndex] = guitarStrings.get(stringIndex).getRenderState(visible);
        }
        return renderStates;
    }

    private boolean isValidSongIndex(int songIndex) {
        return songIndex >= 0 && songIndex < songs.length;
    }

    private void resetStringOscillations() {
        for (GuitarString guitarString : guitarStrings) {
            guitarString.resetOscillation();
        }
    }
}
