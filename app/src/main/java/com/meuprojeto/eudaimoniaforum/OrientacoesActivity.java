package com.meuprojeto.eudaimoniaforum;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class OrientacoesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orientacoes);

        Button buttonVerTutorial = findViewById(R.id.buttonVerTutorial);
        buttonVerTutorial.setOnClickListener(v -> {
            Intent intent = new Intent(OrientacoesActivity.this, OnboardingActivity.class);
            intent.putExtra("IS_HELP_MODE", true);
            startActivity(intent);
        });
    }
}
