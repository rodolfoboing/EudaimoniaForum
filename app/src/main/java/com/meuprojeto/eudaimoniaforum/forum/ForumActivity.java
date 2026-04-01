package com.meuprojeto.eudaimoniaforum.forum;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForumActivity extends AppCompatActivity {

    private RecyclerView recyclerViewPosts;
    private PostAdapter postAdapter;
    private List<Post> postList; // Lista que sempre contém todos os posts do Firebase
    private List<Post> postsExibidos; // Lista para os posts que estão sendo exibidos (filtrados)
    private DatabaseReference databaseReference;
    private EditText editTextSearch;
    private Button buttonOrdenarRecentes, buttonOrdenarComentados, buttonMinhasPostagens;
    private Spinner spinnerCategorias;
    private String categoriaSelecionada = "Todos"; // Categoria selecionada, padrão é "Todos"
    private boolean isModerador = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ForumActivity", "onCreate() chamado. Inicializando ForumActivity.");
        setContentView(R.layout.forum_activity);

        // Inicialização dos componentes
        editTextSearch = findViewById(R.id.editTextSearch);
        buttonOrdenarRecentes = findViewById(R.id.buttonOrdenarRecentes);
        buttonOrdenarComentados = findViewById(R.id.buttonOrdenarComentados);
        buttonMinhasPostagens = findViewById(R.id.buttonMinhasPostagens);
        spinnerCategorias = findViewById(R.id.spinnerCategorias);
        recyclerViewPosts = findViewById(R.id.recyclerViewPosts);
        recyclerViewPosts.setLayoutManager(new LinearLayoutManager(this));

        postList = new ArrayList<>();
        postsExibidos = new ArrayList<>();
        // Inicializa com isModerador = false (será atualizado assincronamente)
        postAdapter = new PostAdapter(this, postsExibidos, isModerador);
        recyclerViewPosts.setAdapter(postAdapter);

        databaseReference = FirebaseDatabase.getInstance().getReference("forum/posts");

        // Verifica se é moderador
        verificarModerador();

        // Configura o seletor de categorias
        setupSpinner();

        // Carrega os posts do Firebase
        loadPosts();

        // Configura a busca
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarPostsExibidos(); // Filtra a lista já exibida
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Configura botões de ordenação
        buttonOrdenarRecentes.setOnClickListener(v -> ordenarPorMaisRecentes());
        buttonOrdenarComentados.setOnClickListener(v -> ordenarPorMaisComentados());

        // Configura botão de criar post
        findViewById(R.id.buttonCriarPost).setOnClickListener(v -> {
            startActivity(new Intent(ForumActivity.this, NovoPostActivity.class));
        });

        // Configura o novo botão "Minhas Postagens"
        buttonMinhasPostagens.setOnClickListener(v -> {
            startActivity(new Intent(ForumActivity.this, MinhasPostagensActivity.class));
        });
    }

    private void verificarModerador() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference modRef = FirebaseDatabase.getInstance().getReference("moderadores").child(user.getUid());
            modRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                        isModerador = true;
                        // Recria o adapter com a permissão de moderador
                        postAdapter = new PostAdapter(ForumActivity.this, postsExibidos, isModerador);
                        recyclerViewPosts.setAdapter(postAdapter);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("ForumActivity", "Erro ao verificar moderador: " + error.getMessage());
                }
            });
        }
    }

    private void setupSpinner() {
        // Define as categorias
        List<String> categorias = new ArrayList<>();
        categorias.add("Todos");
        categorias.add("Pornografia");
        categorias.add("Jogos de Azar");
        categorias.add("Videogame");
        categorias.add("Álcool");
        categorias.add("Drogas");
        categorias.add("Cigarro");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.forum_spinner_item, categorias);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategorias.setAdapter(adapter);

        // Listener para quando uma categoria é selecionada
        spinnerCategorias.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String novaCategoria = categorias.get(position);
                if (!novaCategoria.equals(categoriaSelecionada)) {
                    categoriaSelecionada = novaCategoria;
                    loadPosts(); // Recarrega do Firebase com a nova query
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private Query postsQuery;
    private ValueEventListener postsListener;

    private void loadPosts() {
        // Remove listener antigo se existir antes de trocar a query
        if (postsQuery != null && postsListener != null) {
            postsQuery.removeEventListener(postsListener);
        }

        // DEFINIÇÃO DO CAMINHO (Otimização de Custo):
        // Se selecionou categoria, filtra os posts pela categoria.
        // Se selecionou "Todos", busca todos os posts recentes.
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("forum/posts");
        if (categoriaSelecionada.equals("Todos")) {
            postsQuery = postsRef.limitToLast(100);
        } else {
            postsQuery = postsRef.orderByChild("categoria").equalTo(categoriaSelecionada).limitToLast(100);
        }

        postsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Post post = postSnapshot.getValue(Post.class);
                    if (post != null) {
                        post.setId(postSnapshot.getKey());
                        postList.add(post);
                    }
                }
                Collections.reverse(postList);
                filtrarPostsExibidos(); // Aplica busca local (texto) sobre os dados carregados
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (!isFinishing() && !isDestroyed()) {
                    Toast.makeText(ForumActivity.this, "Erro ao carregar postagens", Toast.LENGTH_SHORT).show();
                }
            }
        };
        postsQuery.addValueEventListener(postsListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (postsQuery != null && postsListener != null) {
            postsQuery.removeEventListener(postsListener);
        }
    }

    // Método unificado para aplicar todos os filtros
    private void filtrarPostsExibidos() {
        List<Post> postsFiltradosTemporarios = new ArrayList<>();

        // 1. Filtro por Categoria
        for (Post post : postList) {
            if (categoriaSelecionada.equals("Todos") || categoriaSelecionada.equals(post.getCategoria())) {
                postsFiltradosTemporarios.add(post);
            }
        }

        // 2. Filtro por Palavra-chave (busca)
        String palavraChave = editTextSearch.getText().toString();
        if (!palavraChave.isEmpty()) {
            List<Post> postsBuscados = new ArrayList<>();
            for (Post post : postsFiltradosTemporarios) {
                if (post.getTitulo().toLowerCase().contains(palavraChave.toLowerCase()) ||
                        (post.getResumo() != null
                                && post.getResumo().toLowerCase().contains(palavraChave.toLowerCase()))) {
                    postsBuscados.add(post);
                }
            }
            postsFiltradosTemporarios = postsBuscados;
        }

        // Atualiza a lista de exibição e notifica o adapter
        postsExibidos.clear();
        postsExibidos.addAll(postsFiltradosTemporarios);
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
}
