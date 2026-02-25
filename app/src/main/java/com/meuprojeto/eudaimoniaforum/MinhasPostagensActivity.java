package com.meuprojeto.eudaimoniaforum;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MinhasPostagensActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> postList = new ArrayList<>();
    private TextView emptyState;
    private Button btnMinhasPostagens, btnComentadas;

    private DatabaseReference postsRef;
    private String currentUserId;

    private enum Filtro {
        MINHAS_POSTAGENS, COMENTADAS
    }

    private Filtro filtroAtual = Filtro.MINHAS_POSTAGENS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_minhas_postagens);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = user.getUid();

        inicializarUI();
        configurarRecyclerView();
        configurarBotoesFiltro();

        postsRef = FirebaseDatabase.getInstance().getReference("forum/posts");

        carregarPostagens();
    }

    private void inicializarUI() {
        recyclerView = findViewById(R.id.recyclerViewMinhasPostagens);
        emptyState = findViewById(R.id.textViewEmptyState);
        btnMinhasPostagens = findViewById(R.id.buttonFiltroMinhasPostagens);
        btnComentadas = findViewById(R.id.buttonFiltroComentadas);
    }

    private void configurarRecyclerView() {
        adapter = new PostAdapter(this, postList, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void configurarBotoesFiltro() {
        btnMinhasPostagens.setOnClickListener(v -> {
            filtroAtual = Filtro.MINHAS_POSTAGENS;
            carregarPostagens();
        });

        btnComentadas.setOnClickListener(v -> {
            filtroAtual = Filtro.COMENTADAS;
            carregarPostagens();
        });
    }

    private void carregarPostagens() {
        postList.clear();
        adapter.notifyDataSetChanged();
        updateEmptyState(true);

        if (filtroAtual == Filtro.MINHAS_POSTAGENS) {
            carregarMinhasPostagens();
        } else {
            carregarPostagensComentadas();
        }
    }

    private void carregarMinhasPostagens() {
        postsRef.orderByChild("autor").equalTo(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                        Post post = postSnapshot.getValue(Post.class);
                        if (post != null) {
                            post.setId(postSnapshot.getKey());
                            postList.add(post);
                        }
                    }
                    Collections.sort(postList, (p1, p2) -> Long.compare(p2.getData(), p1.getData()));
                }
                adapter.notifyDataSetChanged();
                updateEmptyState(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MinhasPostagensActivity.this, "Erro ao carregar postagens", Toast.LENGTH_SHORT).show();
                updateEmptyState(false);
            }
        });
    }

    private void carregarPostagensComentadas() {
        DatabaseReference userPostsComentadosRef = FirebaseDatabase.getInstance().getReference("users")
                .child(currentUserId).child("postsComentados");

        userPostsComentadosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    carregarPostagensPorIds(snapshot);
                } else {
                    carregarPostagensComentadasLegado();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MinhasPostagensActivity.this, "Erro ao carregar referências", Toast.LENGTH_SHORT).show();
                updateEmptyState(false);
            }
        });
    }

    private void carregarPostagensPorIds(DataSnapshot postsIdsSnapshot) {
        postList.clear();
        List<String> postsIds = new ArrayList<>();
        for (DataSnapshot idSnapshot : postsIdsSnapshot.getChildren()) {
            postsIds.add(idSnapshot.getKey());
        }

        if (postsIds.isEmpty()) {
            updateEmptyState(false);
            return;
        }

        final int[] loadedCount = { 0 };
        final int totalPosts = postsIds.size();

        for (String postId : postsIds) {
            postsRef.child(postId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Post post = snapshot.getValue(Post.class);
                    if (post != null) {
                        post.setId(snapshot.getKey());
                        postList.add(post);
                    }
                    loadedCount[0]++;
                    if (loadedCount[0] >= totalPosts) {
                        finalizarCarregamento();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    loadedCount[0]++;
                    if (loadedCount[0] >= totalPosts) {
                        finalizarCarregamento();
                    }
                }
            });
        }
    }

    private void finalizarCarregamento() {
        Collections.sort(postList, (p1, p2) -> Long.compare(p2.getData(), p1.getData()));
        adapter.notifyDataSetChanged();
        updateEmptyState(false);
    }

    private void carregarPostagensComentadasLegado() {
        postsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                if (!snapshot.exists()) {
                    updateEmptyState(false);
                    return;
                }

                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Post post = postSnapshot.getValue(Post.class);
                    if (post == null) {
                        continue;
                    }

                    if (postSnapshot.child("comentarios").exists()) {
                        for (DataSnapshot comentarioSnapshot : postSnapshot.child("comentarios").getChildren()) {
                            Comentario comentario = comentarioSnapshot.getValue(Comentario.class);
                            if (comentario != null && currentUserId.equals(comentario.getAutor())) {
                                post.setId(postSnapshot.getKey());
                                postList.add(post);
                                salvarReferenciaPostComentado(post.getId());
                                break;
                            }
                        }
                    }
                }

                finalizarCarregamento();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MinhasPostagensActivity.this, "Erro ao verificar comentários", Toast.LENGTH_SHORT)
                        .show();
                updateEmptyState(false);
            }
        });
    }

    private void salvarReferenciaPostComentado(String postId) {
        FirebaseDatabase.getInstance().getReference("users")
                .child(currentUserId).child("postsComentados").child(postId).setValue(true);
    }

    private void updateEmptyState(boolean isLoading) {
        if (isLoading) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("Carregando...");
        } else if (postList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("Nenhuma postagem encontrada.");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }
}
