package com.example.guitarzero.engine;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.os.Build;

/**
 * Play mp3 and set pitch dynamically
 */
public class AudioPlayer {

    private MediaPlayer mediaPlayer;
    private final Context context;
    private final int resId;
    private float pitch = 1.0f;
    private float volume = 1.0f;

    public AudioPlayer(Context context, int resId) {
        this(context, resId, 1.0f);
    }

    public AudioPlayer(Context context, int resId, float volume) {
        this.context = context.getApplicationContext();
        this.resId = resId;
        this.volume = clampVolume(volume);
    }

    public void play() {
        playFrom(0L);
    }

    public void playFrom(long startPositionMs) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(context, resId);
        applyVolume();
        applyPitch();
        applyStartPosition(startPositionMs);
        mediaPlayer.start();
    }

    public void play(float pitch) {
        this.pitch = pitch;
        play();
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void setPitch(float newPitch) {
        this.pitch = newPitch;
        if (mediaPlayer != null) {
            applyPitch();
        }
    }

    public void setVolume(float newVolume) {
        this.volume = clampVolume(newVolume);
        if (mediaPlayer != null) {
            applyVolume();
        }
    }

    private void applyPitch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PlaybackParams params = new PlaybackParams();
            params.setPitch(pitch);
            mediaPlayer.setPlaybackParams(params);
        }
    }

    private void applyVolume() {
        mediaPlayer.setVolume(volume, volume);
    }

    private void applyStartPosition(long startPositionMs) {
        if (startPositionMs <= 0L) {
            return;
        }

        int clampedStartPositionMs = (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(0L, startPositionMs)
        );
        int durationMs = mediaPlayer.getDuration();
        if (durationMs > 0) {
            clampedStartPositionMs = Math.min(clampedStartPositionMs, Math.max(0, durationMs - 1));
        }
        mediaPlayer.seekTo(clampedStartPositionMs);
    }

    private static float clampVolume(float volume) {
        if (volume < 0f) {
            return 0f;
        }
        if (volume > 1f) {
            return 1f;
        }
        return volume;
    }
}
