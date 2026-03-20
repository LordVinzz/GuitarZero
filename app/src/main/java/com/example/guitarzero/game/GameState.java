package com.example.guitarzero.game;

import android.content.res.Resources;
import android.graphics.Canvas;

import com.example.guitarzero.engine.GameEngine;
import com.example.guitarzero.engine.GuitarString;
import com.example.guitarzero.engine.NoteWaveRenderState;

public class GameState {
    public static final int STRING_COUNT = 4;

    public enum ScreenState {
        IN_GAME,
        MAIN_MENU,
        CHOOSE_SONG
    }

    private final String[] songs = {"Fuzz Intro", "Velvet Riff", "Arcade Solo"};
    private final String[] levels = {"Niveau 1", "Niveau 2", "Niveau 3"};
    private final GameEngine gameEngine = new GameEngine(STRING_COUNT);

    private ScreenState currentScreen = ScreenState.MAIN_MENU;
    private int selectedSongIndex = 0;
    private int currentLevelIndex = 0;

    public GameState() {
    }

    public synchronized void update(float deltaTimeSeconds) {
        gameEngine.update(deltaTimeSeconds, currentScreen == ScreenState.IN_GAME);
    }

    public synchronized void draw(Canvas canvas) {
        gameEngine.draw(canvas, currentScreen == ScreenState.IN_GAME);
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

    public synchronized void onSurfaceChanged(Resources resources, int surfaceWidth, int surfaceHeight) {
        gameEngine.onSurfaceChanged(resources, surfaceWidth, surfaceHeight);
    }

    public synchronized void onSurfaceDestroyed() {
        gameEngine.onSurfaceDestroyed();
    }

    public synchronized boolean handleTouch(float touchX, float touchY) {
        return gameEngine.handleTouch(touchX, touchY, currentScreen == ScreenState.IN_GAME);
    }

    public synchronized GuitarString.RenderState[] getGuitarStringRenderStates() {
        return gameEngine.getGuitarStringRenderStates(currentScreen == ScreenState.IN_GAME);
    }

    public synchronized NoteWaveRenderState[] getNoteWaveRenderStates() {
        return gameEngine.getNoteWaveRenderStates(currentScreen == ScreenState.IN_GAME);
    }

    private boolean isValidSongIndex(int songIndex) {
        return songIndex >= 0 && songIndex < songs.length;
    }
}
