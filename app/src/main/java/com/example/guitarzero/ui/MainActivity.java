package com.example.guitarzero.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.guitarzero.R;
import com.example.guitarzero.game.GameState;
import com.example.guitarzero.render.canvas.GameView;
import com.example.guitarzero.render.opengl.RopeGLSurfaceView;

public class MainActivity extends Activity {
    private GameState gameState;
    private RopeGLSurfaceView ropeGLSurfaceView;

    private View mainMenuPanel;
    private View chooseSongPanel;
    private View inGameOverlay;
    private TextView mainMenuSelectedSongText;
    private TextView inGameSongText;
    private TextView inGameLevelText;
    private ImageButton openMainMenuButton;
    private Button[] songButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main);
        gameState = new GameState();

        setupRenderViews();
        bindViews();
        bindListeners();
        refreshUiForState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ropeGLSurfaceView != null) {
            ropeGLSurfaceView.onResume();
            if (gameState.getCurrentScreen() == GameState.ScreenState.IN_GAME) {
                ropeGLSurfaceView.requestRender();
            }
        }
    }

    @Override
    protected void onPause() {
        if (ropeGLSurfaceView != null) {
            ropeGLSurfaceView.onPause();
        }
        super.onPause();
    }

    private void setupRenderViews() {
        FrameLayout renderContainer = findViewById(R.id.render_container);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        renderContainer.addView(new GameView(this, gameState), layoutParams);

        ropeGLSurfaceView = new RopeGLSurfaceView(this, gameState);
        renderContainer.addView(ropeGLSurfaceView, layoutParams);
    }

    private void bindViews() {
        mainMenuPanel = findViewById(R.id.main_menu_panel);
        chooseSongPanel = findViewById(R.id.choose_song_panel);
        inGameOverlay = findViewById(R.id.in_game_overlay);
        mainMenuSelectedSongText = findViewById(R.id.text_selected_song);
        inGameSongText = findViewById(R.id.text_in_game_song);
        inGameLevelText = findViewById(R.id.text_in_game_level);
        openMainMenuButton = findViewById(R.id.button_open_main_menu);
        openMainMenuButton.setImageResource(R.drawable.ic_settings_overlay);

        songButtons = new Button[] {
                findViewById(R.id.button_song_0),
                findViewById(R.id.button_song_1),
                findViewById(R.id.button_song_2)
        };
    }

    private void bindListeners() {
        findViewById(R.id.button_play).setOnClickListener(v -> {
            gameState.startGame(gameState.getSelectedSongIndex());
            refreshUiForState();
        });

        findViewById(R.id.button_choose_song).setOnClickListener(v -> {
            gameState.showChooseSong();
            refreshUiForState();
        });

        for (int i = 0; i < songButtons.length; i++) {
            final int songIndex = i;
            songButtons[i].setOnClickListener(v -> {
                gameState.selectSong(songIndex);
                gameState.showMainMenu();
                refreshUiForState();
            });
        }

        findViewById(R.id.button_back_from_choose_song).setOnClickListener(v -> {
            gameState.showMainMenu();
            refreshUiForState();
        });

        openMainMenuButton.setOnClickListener(v -> {
            gameState.showMainMenu();
            refreshUiForState();
        });
    }

    private void refreshUiForState() {
        updateSongTexts();

        GameState.ScreenState screenState = gameState.getCurrentScreen();
        boolean isMainMenu = screenState == GameState.ScreenState.MAIN_MENU;
        boolean isChooseSong = screenState == GameState.ScreenState.CHOOSE_SONG;
        boolean isInGame = screenState == GameState.ScreenState.IN_GAME;

        mainMenuPanel.setVisibility(isMainMenu ? View.VISIBLE : View.GONE);
        chooseSongPanel.setVisibility(isChooseSong ? View.VISIBLE : View.GONE);
        inGameOverlay.setVisibility(isInGame ? View.VISIBLE : View.GONE);
        ropeGLSurfaceView.setVisibility(isInGame ? View.VISIBLE : View.GONE);

        if (isInGame) {
            ropeGLSurfaceView.requestRender();
        }
    }

    private void updateSongTexts() {
        mainMenuSelectedSongText.setText(
                getString(R.string.selected_song_format, gameState.getCurrentSongLabel())
        );
        inGameSongText.setText(
                getString(R.string.in_game_song_format, gameState.getCurrentSongLabel())
        );
        inGameLevelText.setText(
                getString(R.string.in_game_level_format, gameState.getCurrentLevelLabel())
        );

        int selectedSongIndex = gameState.getSelectedSongIndex();
        for (int i = 0; i < songButtons.length; i++) {
            String label = gameState.getSongLabel(i);
            if (i == selectedSongIndex) {
                label = label + " (selected)";
            }
            songButtons[i].setText(label);
        }
    }
}
