package com.example.guitarzero.game;

import android.content.res.Resources;
import android.graphics.Canvas;

import com.example.guitarzero.R;
import com.example.guitarzero.engine.GameEngine;
import com.example.guitarzero.engine.GuitarString;
import com.example.guitarzero.engine.NoteWaveRenderState;
import com.example.guitarzero.engine.map.MapFile;

public class GameState {
    public static final int STRING_COUNT = 4;
    private static final long DEFAULT_MAP_SEED = 42L;

    public enum ScreenState {
        IN_GAME,
        MAIN_MENU,
        CHOOSE_SONG
    }

    private final MapFile[] mapFiles;
    private final GameEngine gameEngine;

    private ScreenState currentScreen = ScreenState.MAIN_MENU;
    private int selectedSongIndex = 0;

    public GameState(Resources resources) {
        mapFiles = new MapFile[] {
                MapFile.load(
                        resources,
                        R.raw.scom,
                        "scom",
                        "scom.mid",
                        DEFAULT_MAP_SEED,
                        3, //guitar channel
                        STRING_COUNT,
                        0L
                )
        };
        gameEngine = new GameEngine(STRING_COUNT, mapFiles[0]);
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
        return mapFiles.length;
    }

    public synchronized String getSongLabel(int songIndex) {
        if (!isValidSongIndex(songIndex)) {
            throw new IllegalArgumentException("Invalid song index: " + songIndex);
        }

        return mapFiles[songIndex].getDisplayName();
    }

    public synchronized String getCurrentSongLabel() {
        return mapFiles[selectedSongIndex].getDisplayName();
    }

    public synchronized String getCurrentLevelLabel() {
        return "Niveau 1";
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
        return songIndex >= 0 && songIndex < mapFiles.length;
    }
}
