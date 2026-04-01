package com.meuprojeto.eudaimoniaforum.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onboarding_splash_activity);

        ImageView logo = findViewById(R.id.logo);
        // Aplica a animação suave de fade e escala
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_in_scale);
        logo.startAnimation(anim);

        // Aguarda a animação e depois vai pra próxima tela
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, 2000);
    }
}