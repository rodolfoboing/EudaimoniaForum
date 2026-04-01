package com.meuprojeto.eudaimoniaforum.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.meuprojeto.eudaimoniaforum.R;

public class RecuperarSenhaActivity extends AppCompatActivity {

    private EditText editTextEmail;
    private Button buttonEnviar;
    private Button buttonVoltar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_recuperar_senha_activity);

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.editTextEmailRecuperacao);
        buttonEnviar = findViewById(R.id.buttonEnviarRecuperacao);
        buttonVoltar = findViewById(R.id.buttonVoltarLogin);

        buttonEnviar.setOnClickListener(v -> enviarEmailRecuperacao());

        buttonVoltar.setOnClickListener(v -> finish()); // Apenas fecha a tela e volta pro Login
    }

    private void enviarEmailRecuperacao() {
        String email = editTextEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Digite o e-mail");
            return;
        }

        // Método do Firebase para enviar o e-mail
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(RecuperarSenhaActivity.this,
                                    "E-mail de recuperação enviado! Verifique sua caixa de entrada.",
                                    Toast.LENGTH_LONG).show();
                            // Opcional: fechar a tela após sucesso
                            finish();
                        } else {
                            String erro = task.getException() != null ? task.getException().getMessage() : "Erro desconhecido";
                            Toast.makeText(RecuperarSenhaActivity.this,
                                    "Falha ao enviar e-mail: " + erro,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}