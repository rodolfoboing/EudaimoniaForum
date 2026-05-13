package com.meuprojeto.eudaimoniaforum.forum;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.meuprojeto.eudaimoniaforum.R;

public class ForumRulesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forum_rules_activity);

        Button buttonAgree = findViewById(R.id.buttonAgreeRules);
        
        // Verifica se a tela foi aberta a partir do menu, onde o usuário já aceitou
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean hasAccepted = prefs.getBoolean("has_accepted_forum_rules", false);
        
        if (hasAccepted) {
            buttonAgree.setText(getString(R.string.btn_close));
        }

        buttonAgree.setOnClickListener(v -> {
            if (!hasAccepted) {
                prefs.edit().putBoolean("has_accepted_forum_rules", true).apply();
            }
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean hasAccepted = prefs.getBoolean("has_accepted_forum_rules", false);
        
        // Se ainda não aceitou as regras, não permite voltar sem aceitar
        if (hasAccepted) {
            super.onBackPressed();
        }
    }
}
