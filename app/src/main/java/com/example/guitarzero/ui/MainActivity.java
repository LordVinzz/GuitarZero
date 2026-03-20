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
import android.widget.TextView;

import com.example.guitarzero.LightSensorManager;
import com.example.guitarzero.R;
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
    private TextView mainMenuSelectedSongText;
    private TextView scoreTextView;
    private ImageButton openMainMenuButton;
    private Button[] songButtons;
    private LightSensorManager lightSensorManager;
    private final Runnable scoreOverlayUpdater = new Runnable() {
        @Override
        public void run() {
            if (scoreTextView == null || gameState == null) {
                return;
            }

            if (gameState.isCountdownActive()) {
                scoreTextView.setText(gameState.getCountdownLabel());
            } else {
                scoreTextView.setText(
                        getString(R.string.score_overlay_format, gameState.getCurrentScore())
                );
            }

            if (gameState.getCurrentScreen() == GameState.ScreenState.IN_GAME) {
                scoreTextView.postDelayed(this, 33L);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_main);
        gameState = new GameState(this);
        lightSensorManager = new LightSensorManager(this, lux -> {
            float multiplier;
            if (lux < 10f) {
                multiplier = 0.5f;
            } else if (lux < 1000f) {
                multiplier = 1.0f;
            } else {
                multiplier = 2.0f;
            }
            gameState.setHitSoundPitch(multiplier);
        });

        setupRenderViews();
        bindViews();
        bindListeners();
        refreshUiForState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensorManager != null) {
            lightSensorManager.register();
        }
        gameState.onAppResume();
        if (ropeGLSurfaceView != null) {
            ropeGLSurfaceView.onResume();
            ropeGLSurfaceView.setInGameRendering(
                    gameState.getCurrentScreen() == GameState.ScreenState.IN_GAME
            );
        }
        if (gameState.getCurrentScreen() == GameState.ScreenState.IN_GAME) {
            startScoreOverlayUpdates();
        }
    }

    @Override
    protected void onPause() {
        stopScoreOverlayUpdates();
        if (ropeGLSurfaceView != null) {
            ropeGLSurfaceView.onPause();
        }
        if (lightSensorManager != null) {
            lightSensorManager.unregister();
        }
        gameState.onAppPause();
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
        slowZoneBarView = findViewById(R.id.image_slow_zone_bar);
        mainMenuSelectedSongText = findViewById(R.id.text_selected_song);
        scoreTextView = findViewById(R.id.text_score_overlay);
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
        ropeGLSurfaceView.setInGameRendering(isInGame);

        if (isInGame) {
            startScoreOverlayUpdates();
            updateSlowZoneBarPosition();
        } else {
            stopScoreOverlayUpdates();
        }
    }

    private void updateSongTexts() {
        mainMenuSelectedSongText.setText(
                getString(R.string.selected_song_format, gameState.getCurrentSongLabel())
        );

        int songCount = gameState.getSongCount();
        int selectedSongIndex = gameState.getSelectedSongIndex();
        for (int i = 0; i < songButtons.length; i++) {
            if (i >= songCount) {
                songButtons[i].setVisibility(View.GONE);
                continue;
            }

            songButtons[i].setVisibility(View.VISIBLE);
            String label = gameState.getSongLabel(i);
            if (i == selectedSongIndex) {
                label = label + " (selected)";
            }
            songButtons[i].setText(label);
        }
    }

    private void startScoreOverlayUpdates() {
        if (scoreTextView == null) {
            return;
        }

        scoreTextView.removeCallbacks(scoreOverlayUpdater);
        scoreOverlayUpdater.run();
    }

    private void stopScoreOverlayUpdates() {
        if (scoreTextView == null) {
            return;
        }

        scoreTextView.removeCallbacks(scoreOverlayUpdater);
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
