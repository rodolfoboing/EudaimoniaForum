package com.meuprojeto.eudaimoniaforum.forum;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.meuprojeto.eudaimoniaforum.R;

import java.util.ArrayList;
import java.util.List;

public class NovoPostActivity extends AppCompatActivity {

    private EditText editTextTitulo;
    private EditText editTextConteudo;
    private Spinner spinnerCategoriaPost;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("NovoPostActivity", "onCreate() chamado. Inicializando NovoPostActivity.");
        setContentView(R.layout.forum_criar_post_activity);

        editTextTitulo = findViewById(R.id.editTextTitulo);
        editTextConteudo = findViewById(R.id.editTextConteudo);
        spinnerCategoriaPost = findViewById(R.id.spinnerCategoriaPost);

        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("forum/posts");

        setupSpinner();
    }

    private void setupSpinner() {
        List<String> categorias = new ArrayList<>();
        categorias.add("Pornografia");
        categorias.add("Jogos de Azar");
        categorias.add("Videogame");
        categorias.add("Álcool");
        categorias.add("Drogas");
        categorias.add("Cigarro");

        // Usando o novo layout customizado aqui
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.forum_spinner_item_custom, categorias);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoriaPost.setAdapter(adapter);
    }

    public void publicarPost(View view) {
        String titulo = editTextTitulo.getText().toString().trim();
        String resumo = editTextConteudo.getText().toString().trim();
        String categoria = spinnerCategoriaPost.getSelectedItem().toString();

        if (titulo.isEmpty() || resumo.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (resumo.length() > 280) {
            Toast.makeText(this, "O resumo do post não pode ter mais de 280 caracteres.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Erro: usuário não autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String autor = user.getUid();
        DatabaseReference userLastPostRef = FirebaseDatabase.getInstance().getReference("users").child(autor)
                .child("lastPostTimestamp");

        userLastPostRef.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                Long lastPostTimestamp = snapshot.getValue(Long.class);
                long currentTime = System.currentTimeMillis();
                long cooldownMillis = 60000; // 60 segundos

                if (lastPostTimestamp != null && (currentTime - lastPostTimestamp) < cooldownMillis) {
                    long segundosRestantes = (cooldownMillis - (currentTime - lastPostTimestamp)) / 1000;
                    Toast.makeText(NovoPostActivity.this, "Aguarde " + segundosRestantes + "s para postar novamente.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    realizarPublicacao(autor, titulo, resumo, categoria, currentTime);
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(NovoPostActivity.this, "Erro ao verificar status: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void realizarPublicacao(String autor, String titulo, String resumo, String categoria, long data) {
        String postId = databaseReference.push().getKey();
        if (postId == null) {
            Toast.makeText(this, "Erro ao gerar ID do post", Toast.LENGTH_SHORT).show();
            return;
        }

        Post post = new Post(postId, titulo, resumo, 0, autor, data, categoria);

        // Inicia mapa de atualizações atômicas (Atomic Writes)
        java.util.Map<String, Object> childUpdates = new java.util.HashMap<>();

        // Caminho 1: Lista Geral de Posts
        childUpdates.put("/forum/posts/" + postId, post);

        // Caminho 2: Meus Posts (histórico do usuário)
        childUpdates.put("/users/" + autor + "/posts/" + postId, true);

        // Caminho 4: Atualizar Timestamp do último post (Anti-spam)
        childUpdates.put("/users/" + autor + "/lastPostTimestamp", data);

        // Executa todas as gravações de uma vez
        FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(NovoPostActivity.this, "Post publicado com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NovoPostActivity.this, "Erro ao publicar: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }
}
