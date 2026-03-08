package com.meuprojeto.eudaimoniaforum;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity"; // Tag para logs

    private FirebaseAuth mAuth;
    private EditText editTextUsuario;
    private EditText editTextSenha;
    private Button buttonEntrar;
    private Button buttonCadastrar;
    private TextView textViewEsqueciSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("LoginActivity", "onCreate() chamado. Inicializando LoginActivity.");

        // Verificar se o usuário já está logado
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Log.d(TAG, "Usuário já está logado. Redirecionando para MainActivity.");
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.tela_login);
        mAuth = FirebaseAuth.getInstance();
        inicializarUI();
        configurarListeners();
    }

    private void inicializarUI() {
        editTextUsuario = findViewById(R.id.editTextUsuario);
        editTextSenha = findViewById(R.id.editTextSenha);
        buttonEntrar = findViewById(R.id.buttonEntrar);
        buttonCadastrar = findViewById(R.id.buttonCadastrar);
        textViewEsqueciSenha = findViewById(R.id.textViewEsqueciSenha);
    }

    private void configurarListeners() {
        buttonEntrar.setOnClickListener(v -> {
            String email = editTextUsuario.getText().toString().trim();
            String password = editTextSenha.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show();
                return;
            }
            loginUser(email, password);
        });

        textViewEsqueciSenha.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RecuperarSenhaActivity.class));
        });

        buttonCadastrar.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegistrarActivity.class));
        });
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        verificarBanimento(user);
                    } else {
                        Log.e(TAG, "Falha na autenticação: ", task.getException());
                        editTextSenha.setText(""); // Limpa o campo de senha em caso de erro
                        Toast.makeText(LoginActivity.this, "Falha na autenticação. Verifique suas credenciais.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void verificarBanimento(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(LoginActivity.this, "Erro ao verificar usuário.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference banidosRef = FirebaseDatabase.getInstance().getReference("banidos").child(user.getUid());
        banidosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(LoginActivity.this, "Você está banido.", Toast.LENGTH_SHORT).show();
                    editTextSenha.setText(""); // Limpa a senha se estiver banido por segurança
                    FirebaseAuth.getInstance().signOut();
                } else {
                    updateUI(user);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Erro ao verificar banimento: ", error.toException());
                Toast.makeText(LoginActivity.this, "Erro ao verificar banimento.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // ATUALIZA O TIMESTAMP DO ÚLTIMO LOGIN
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            userRef.child("lastLoginTimestamp").setValue(System.currentTimeMillis());

            Toast.makeText(this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}
