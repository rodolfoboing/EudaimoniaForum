package com.meuprojeto.eudaimoniaforum.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meuprojeto.eudaimoniaforum.R;

public class RecoverPasswordActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private Button buttonEnviar;
    private Button buttonVoltar;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_recuperar_senha_activity);

        authManager = new AuthManager();

        editTextEmail = findViewById(R.id.editTextEmailRecuperacao);
        buttonEnviar = findViewById(R.id.buttonEnviarRecuperacao);
        buttonVoltar = findViewById(R.id.buttonVoltarLogin);

        buttonEnviar.setOnClickListener(v -> enviarEmailRecuperacao());
        buttonVoltar.setOnClickListener(v -> finish());
    }

    private void enviarEmailRecuperacao() {
        String email = editTextEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Digite o e-mail");
            return;
        }

        authManager.recuperarSenha(email, new AuthManager.PasswordResetCallback() {
            @Override
            public void onSuccess() {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(RecoverPasswordActivity.this, "E-mail de recuperação enviado! Verifique sua caixa de entrada.", Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(RecoverPasswordActivity.this, "Falha ao enviar e-mail: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}