package com.meuprojeto.eudaimoniaforum;

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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NovoPostActivity extends AppCompatActivity {

    private EditText editTextTitulo;
    private EditText editTextConteudo;
    private Spinner spinnerCategoriaPost;
    private DatabaseReference databaseReference;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_criar_post);

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
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_custom, categorias);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoriaPost.setAdapter(adapter);
    }

    public void publicarPost(View view) {
        String titulo = editTextTitulo.getText().toString().trim();
        String resumo = editTextConteudo.getText().toString().trim();
        String categoria = spinnerCategoriaPost.getSelectedItem().toString();

        if (titulo.isEmpty() || resumo.isEmpty()) {
            Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show();
        } else if (resumo.length() > 280) {
            Toast.makeText(this, "O resumo do post não pode ter mais de 280 caracteres.", Toast.LENGTH_SHORT).show();
        } else {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                String autor = user.getUid();
                String data = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

                String postId = databaseReference.push().getKey();
                if (postId != null) {
                    Post post = new Post(postId, titulo, resumo, 0, autor, data, categoria);

                    databaseReference.child(postId).setValue(post)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(NovoPostActivity.this, "Post publicado com sucesso!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(NovoPostActivity.this, "Erro ao publicar o post", Toast.LENGTH_SHORT).show();
                            });
                } else {
                    Toast.makeText(this, "Erro ao gerar ID do post", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Erro: usuário não autenticado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
