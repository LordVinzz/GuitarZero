package com.example.guitarzero;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        int valeurY = sharedPref.getInt("valeur_y", 0);
        valeurY = (valeurY + 100) % 400;

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("valeur_y", valeurY);
        editor.apply();

        setContentView(new GameView(this, valeurY));
    }
}
