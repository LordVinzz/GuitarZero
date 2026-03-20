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

    public AudioPlayer(Context context, int resId) {
        this.context = context.getApplicationContext();
        this.resId = resId;
    }

    public void play() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(context, resId);
        applyPitch();
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

    private void applyPitch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PlaybackParams params = new PlaybackParams();
            params.setPitch(pitch);
            mediaPlayer.setPlaybackParams(params);
        }
    }
}
