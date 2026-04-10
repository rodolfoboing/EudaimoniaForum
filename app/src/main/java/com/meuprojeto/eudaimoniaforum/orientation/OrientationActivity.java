package com.meuprojeto.eudaimoniaforum.orientation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.onboarding.OnboardingActivity;

public class OrientationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.orientacoes_activity);

        Button buttonVerTutorial = findViewById(R.id.buttonVerTutorial);
        buttonVerTutorial.setOnClickListener(v -> {
            Intent intent = new Intent(OrientationActivity.this, OnboardingActivity.class);
            intent.putExtra("IS_HELP_MODE", true);
            startActivity(intent);
        });
    }
}
