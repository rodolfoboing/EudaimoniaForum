package com.meuprojeto.eudaimoniaforum;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegistrarActivity extends AppCompatActivity {

    private static final String TAG = "RegistrarActivity";

    private FirebaseAuth mAuth;
    private EditText editTextEmail;
    private EditText editTextNome;
    private Spinner spinnerVicio;
    private EditText editTextSenhaCadastro;
    private Button buttonCadastrar;

    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_registrar);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextNome = findViewById(R.id.editTextNome);
        spinnerVicio = findViewById(R.id.spinnerVicio);
        editTextSenhaCadastro = findViewById(R.id.editTextSenhaCadastro);
        buttonCadastrar = findViewById(R.id.buttonCadastrar);

        setupSpinner();

        buttonCadastrar.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextSenhaCadastro.getText().toString().trim();
            String nickDesejado = editTextNome.getText().toString().trim();
            String vicioSelecionado = spinnerVicio.getSelectedItem().toString();

            if (email.isEmpty() || password.isEmpty() || nickDesejado.isEmpty()) {
                Toast.makeText(RegistrarActivity.this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (nickDesejado.length() < 3) {
                Toast.makeText(RegistrarActivity.this, "O nick deve ter pelo menos 3 caracteres.", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog progressDialog = new ProgressDialog(RegistrarActivity.this);
            progressDialog.setMessage("Verificando nick...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            verificarNickDuplicado(nickDesejado, new OnNickCheckListener() {
                @Override
                public void onNickExists(boolean exists) {
                    progressDialog.dismiss();
                    if (exists) {
                        Toast.makeText(RegistrarActivity.this, "Este nick já está em uso, escolha outro!", Toast.LENGTH_SHORT).show();
                    } else {
                        registerUser(email, password, nickDesejado, vicioSelecionado);
                    }
                }

                @Override
                public void onError(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(RegistrarActivity.this, "Erro ao verificar nick: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupSpinner() {
        List<String> vicios = new ArrayList<>();
        vicios.add("Pornografia");
        vicios.add("Jogos de Azar");
        vicios.add("Videogame");
        vicios.add("Álcool");
        vicios.add("Drogas");
        vicios.add("Cigarro");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vicios);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVicio.setAdapter(adapter);
    }

    private void verificarNickDuplicado(String nick, OnNickCheckListener listener) {
        // Agora verifica no nó 'usernames' ao invés de buscar em 'users'
        DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference("usernames");
        usernamesRef.child(nick).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                listener.onNickExists(task.getResult().exists());
            } else {
                listener.onError(task.getException() != null ? task.getException().getMessage() : "Erro desconhecido");
            }
        });
    }

    private void registerUser(String email, String password, String nick, String vicio) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "createUserWithEmail:success");
                FirebaseUser user = mAuth.getCurrentUser();
                salvarDadosUsuario(user, nick, vicio);
            } else {
                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                Toast.makeText(RegistrarActivity.this, "Falha ao criar conta: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void salvarDadosUsuario(FirebaseUser user, String nick, String vicio) {
        if (user == null) {
            Toast.makeText(this, "Erro ao obter usuário autenticado!", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        long dataEntradaTimestamp = System.currentTimeMillis();
        String sobreMim = "Bem-vindo ao meu perfil!";

        Usuario novoUsuario = new Usuario(userId, nick, String.valueOf(dataEntradaTimestamp), sobreMim, vicio);

        // Cria um mapa para atualização simultânea em 'users' e 'usernames'
        Map<String, Object> updates = new HashMap<>();
        updates.put("users/" + userId, novoUsuario);
        updates.put("usernames/" + nick, userId);

        // Executa a atualização atômica na raiz do banco
        FirebaseDatabase.getInstance().getReference().updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Usuário registrado com sucesso!", Toast.LENGTH_SHORT).show();
                updateUI(user);
            } else {
                Toast.makeText(this, "Falha ao salvar dados no banco de dados: " + 
                        (task.getException() != null ? task.getException().getMessage() : ""), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            finish();
        }
    }

    interface OnNickCheckListener {
        void onNickExists(boolean exists);
        void onError(String error);
    }
}
