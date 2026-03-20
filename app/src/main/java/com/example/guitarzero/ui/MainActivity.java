package com.example.guitarzero.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.guitarzero.LightSensorManager;
import com.example.guitarzero.R;
import com.example.guitarzero.engine.AudioPlayer;
import com.example.guitarzero.engine.Note;
import com.example.guitarzero.game.GameState;
import com.example.guitarzero.render.canvas.GameView;
import com.example.guitarzero.render.opengl.RopeGLSurfaceView;

public class MainActivity extends Activity {
    private static final float NOTE_SLOW_ZONE_Y_NORMALIZED = 0.75f;

    private GameState gameState;
    private RopeGLSurfaceView ropeGLSurfaceView;

    private View mainMenuPanel;
    private View chooseSongPanel;
    private View inGameOverlay;
    private ImageView slowZoneBarView;
    private android.widget.TextView mainMenuSelectedSongText;
    private ImageButton openMainMenuButton;
    private Button[] songButtons;
    private AudioPlayer player;
    private LightSensorManager lightSensorManager;

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
        // init audio play, TODO: pick which mp3 to play here
        player = new AudioPlayer(this, R.raw.test);
        // init light sensor
        lightSensorManager = new LightSensorManager(this, lux -> {
            float multiplier; // ad-hoc
            if (lux < 10) multiplier = 0.5f; // very dark light (finger on sensor)
            else if (lux < 1000) multiplier = 1.0f; // normal light
            else multiplier = 2.0f; // sunlight
            player.setPitch(multiplier);
        });

        setupRenderViews();
        bindViews();
        bindListeners();
        refreshUiForState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        player.play();
        lightSensorManager.register();
        if (ropeGLSurfaceView != null) {
            ropeGLSurfaceView.onResume();
            ropeGLSurfaceView.setInGameRendering(
                    gameState.getCurrentScreen() == GameState.ScreenState.IN_GAME
            );
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        lightSensorManager.unregister();
        player.stop();
        if (ropeGLSurfaceView != null) {
            ropeGLSurfaceView.onPause();
        }
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
        slowZoneBarView = findViewById(R.id.image_slow_zone_bar);
        mainMenuSelectedSongText = findViewById(R.id.text_selected_song);
        openMainMenuButton = findViewById(R.id.button_open_main_menu);
        openMainMenuButton.setImageResource(R.drawable.ic_settings_overlay);
        inGameOverlay.addOnLayoutChangeListener((view,
                                                 left,
                                                 top,
                                                 right,
                                                 bottom,
                                                 oldLeft,
                                                 oldTop,
                                                 oldRight,
                                                 oldBottom) -> updateSlowZoneBarPosition());
        inGameOverlay.post(this::updateSlowZoneBarPosition);

        songButtons = new Button[]{
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
        ropeGLSurfaceView.setInGameRendering(isInGame);
    }

    private void updateSongTexts() {
        mainMenuSelectedSongText.setText(
                getString(R.string.selected_song_format, gameState.getCurrentSongLabel())
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

    private void updateSlowZoneBarPosition() {
        if (inGameOverlay == null || slowZoneBarView == null) {
            return;
        }

        int overlayHeight = inGameOverlay.getHeight();
        int barHeight = slowZoneBarView.getHeight();
        if (overlayHeight <= 0 || barHeight <= 0) {
            return;
        }

        float centeredY = (overlayHeight * NOTE_SLOW_ZONE_Y_NORMALIZED) - (barHeight / 2f);
        slowZoneBarView.setY(centeredY);
    }
}
