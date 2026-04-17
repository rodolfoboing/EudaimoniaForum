package com.meuprojeto.eudaimoniaforum.forum;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meuprojeto.eudaimoniaforum.R;

import java.util.ArrayList;
import java.util.List;

public class NewPostActivity extends AppCompatActivity {

    private EditText editTextTitulo;
    private EditText editTextConteudo;
    private Spinner spinnerCategoriaPost;
    private ForumManager forumManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("NewPostActivity", "onCreate() chamado. Inicializando NewPostActivity.");
        setContentView(R.layout.forum_create_post_activity);

        editTextTitulo = findViewById(R.id.editTextTitulo);
        editTextConteudo = findViewById(R.id.editTextConteudo);
        spinnerCategoriaPost = findViewById(R.id.spinnerCategoriaPost);

        forumManager = new ForumManager();

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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.forum_spinner_item_custom, categorias);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoriaPost.setAdapter(adapter);
    }

    public void publicarPost(View view) {
        String titulo = editTextTitulo.getText().toString().trim();
        String resumo = editTextConteudo.getText().toString().trim();
        String categoria = spinnerCategoriaPost.getSelectedItem().toString();

        if (titulo.isEmpty() || resumo.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (titulo.length() > 80) {
            Toast.makeText(this, getString(R.string.error_title_too_long_80), Toast.LENGTH_SHORT).show();
            return;
        }

        if (resumo.length() > 280) {
            Toast.makeText(this, getString(R.string.error_post_content_too_long_280), Toast.LENGTH_SHORT).show();
            return;
        }

        forumManager.publicarPost(titulo, resumo, categoria, new ForumManager.PostCallback() {
            @Override
            public void onSuccess() {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(NewPostActivity.this, getString(R.string.msg_post_published), Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onWaitDelay(long secondsRemaining) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(NewPostActivity.this, String.format(getString(R.string.msg_wait_to_post), secondsRemaining), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(NewPostActivity.this, getString(R.string.error_post_failed) + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
