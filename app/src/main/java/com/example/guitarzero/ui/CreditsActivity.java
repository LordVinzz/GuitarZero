package com.example.guitarzero.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.example.guitarzero.R;

public class CreditsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        setContentView(R.layout.activity_credits);

        bindLink(R.id.text_loan_link);
        bindLink(R.id.text_vincent_link);
        bindLink(R.id.text_nabil_link);
        findViewById(R.id.button_back_from_credits).setOnClickListener(view -> finish());
    }

    private void bindLink(int textViewId) {
        TextView textView = findViewById(textViewId);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
