package com.meuprojeto.eudaimoniaforum.forum;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForumActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private List<Post> postListRaw;
    private List<Post> postsExibidos;
    private EditText editTextSearch;
    private Button buttonOrdenarRecentes, buttonOrdenarComentados, buttonMinhasPostagens;
    private Spinner spinnerCategorias;
    private String categoriaSelecionada = "Todos";
    private boolean isModerador = false;
    
    private ForumManager forumManager;
    private ValueEventListener postsListener;
    private Query[] activeQueryContainer = new Query[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ForumActivity", "onCreate() chamado. Inicializando ForumActivity.");
        setContentView(R.layout.forum_activity);

        editTextSearch = findViewById(R.id.editTextSearch);
        buttonOrdenarRecentes = findViewById(R.id.buttonOrdenarRecentes);
        buttonOrdenarComentados = findViewById(R.id.buttonOrdenarComentados);
        buttonMinhasPostagens = findViewById(R.id.buttonMinhasPostagens);
        spinnerCategorias = findViewById(R.id.spinnerCategorias);
        recyclerViewPosts = findViewById(R.id.recyclerViewPosts);
        
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(this));

        postListRaw = new ArrayList<>();
        postsExibidos = new ArrayList<>();
        
        postAdapter = new PostAdapter(this, postsExibidos, isModerador);
        recyclerViewPosts.setAdapter(postAdapter);

        forumManager = new ForumManager();

        verificarModeracao();
        setupSpinner();
        carregarDadosBase();

        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarDadosExibidos();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        buttonOrdenarRecentes.setOnClickListener(v -> ordenarPorMaisRecentes());
        buttonOrdenarComentados.setOnClickListener(v -> ordenarPorMaisComentados());

        findViewById(R.id.buttonCriarPost).setOnClickListener(v -> {
            startActivity(new Intent(ForumActivity.this, NewPostActivity.class));
        });

        buttonMinhasPostagens.setOnClickListener(v -> {
            startActivity(new Intent(ForumActivity.this, MyPostActivity.class));
        });
    }

    private void verificarModeracao() {
        forumManager.verificarModerador(moderador -> {
            if(isFinishing() || isDestroyed()) return;
            isModerador = moderador;
            postAdapter = new PostAdapter(ForumActivity.this, postsExibidos, isModerador);
            recyclerViewPosts.setAdapter(postAdapter);
        });
    }

    private void setupSpinner() {
        String[] categoriasArray = getResources().getStringArray(R.array.lista_categorias);
        List<String> categorias = new ArrayList<>(java.util.Arrays.asList(categoriasArray));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.forum_spinner_item, categorias);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorias.setAdapter(adapter);

        spinnerCategorias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String novaCategoria = categorias.get(position);
                if (!novaCategoria.equals(categoriaSelecionada)) {
                    categoriaSelecionada = novaCategoria;
                    carregarDadosBase();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void carregarDadosBase() {
        if (activeQueryContainer[0] != null && postsListener != null) {
            activeQueryContainer[0].removeEventListener(postsListener);
        }

        postsListener = forumManager.carregarFeed(categoriaSelecionada, activeQueryContainer, new ForumManager.FeedCallback() {
            @Override
            public void onPostsCarregados(List<Post> posts) {
                if(isFinishing() || isDestroyed()) return;
                postListRaw.clear();
                postListRaw.addAll(posts);
                filtrarDadosExibidos();
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(ForumActivity.this, getString(R.string.error_loading_data) + ": " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrarDadosExibidos() {
        List<Post> postsTrabalho = new ArrayList<>();

        for (Post post : postListRaw) {
            if (categoriaSelecionada.equals("Todos") || categoriaSelecionada.equals(post.getCategoria())) {
                postsTrabalho.add(post);
            }
        }

        String palavraChave = editTextSearch.getText().toString().toLowerCase();
        if (!palavraChave.isEmpty()) {
            List<Post> pesquisados = new ArrayList<>();
            for (Post post : postsTrabalho) {
                boolean matchTittle = post.getTitulo() != null && post.getTitulo().toLowerCase().contains(palavraChave);
                boolean matchResume = post.getResumo() != null && post.getResumo().toLowerCase().contains(palavraChave);
                if (matchTittle || matchResume) {
                    pesquisados.add(post);
                }
            }
            postsTrabalho = pesquisados;
        }

        postsExibidos.clear();
        postsExibidos.addAll(postsTrabalho);
        postAdapter.notifyDataSetChanged();
    }

    private void ordenarPorMaisRecentes() {
        Collections.sort(postsExibidos, (post1, post2) -> Long.compare(post2.getData(), post1.getData()));
        postAdapter.notifyDataSetChanged();
    }

    private void ordenarPorMaisComentados() {
        Collections.sort(postsExibidos, (post1, post2) -> {
            Integer comentarios1 = post1.getNumeroComentarios() != null ? post1.getNumeroComentarios() : 0;
            Integer comentarios2 = post2.getNumeroComentarios() != null ? post2.getNumeroComentarios() : 0;
            return Integer.compare(comentarios2, comentarios1);
        });
        postAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activeQueryContainer[0] != null && postsListener != null) {
            activeQueryContainer[0].removeEventListener(postsListener);
        }
    }
}
