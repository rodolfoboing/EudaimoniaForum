package com.meuprojeto.eudaimoniaforum.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.main.MainActivity;
import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.perfil.SetupPerfilActivity;

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
        FirebaseUser autoLoginUser = FirebaseAuth.getInstance().getCurrentUser();
        if (autoLoginUser != null) {
            Log.d(TAG, "Usuário já está logado. Verificando perfil...");
            DatabaseReference autoRef = FirebaseDatabase.getInstance().getReference("users").child(autoLoginUser.getUid());
            autoRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Boolean configurado = snapshot.child("perfilConfigurado").getValue(Boolean.class);
                    // Usuários antigos que já têm dados no banco (sobreMim, checkins, etc.)
                    // são considerados já configurados automaticamente
                    boolean isUsuarioAntigo = snapshot.child("sobreMim").exists()
                            || snapshot.child("checkins").exists()
                            || snapshot.child("conquistas").exists();

                    if ((configurado != null && configurado) || isUsuarioAntigo) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    } else {
                        startActivity(new Intent(LoginActivity.this, SetupPerfilActivity.class));
                    }
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            });
            return;
        }

        setContentView(R.layout.auth_login_activity);
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

            // Verifica se o perfil foi configurado
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Boolean configurado = snapshot.child("perfilConfigurado").getValue(Boolean.class);
                    boolean isUsuarioAntigo = snapshot.child("sobreMim").exists()
                            || snapshot.child("checkins").exists()
                            || snapshot.child("conquistas").exists();

                    if ((configurado != null && configurado) || isUsuarioAntigo) {
                        Toast.makeText(LoginActivity.this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    } else {
                        Toast.makeText(LoginActivity.this, "Configure seu perfil!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, SetupPerfilActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(LoginActivity.this, "Login bem-sucedido!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            });
        }
    }
}
