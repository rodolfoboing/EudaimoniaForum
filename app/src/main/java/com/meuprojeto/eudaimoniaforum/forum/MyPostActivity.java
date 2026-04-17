package com.meuprojeto.eudaimoniaforum.forum;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.meuprojeto.eudaimoniaforum.R;

import java.util.ArrayList;
import java.util.List;

public class MyPostActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> postList = new ArrayList<>();
    private TextView emptyState;
    private Button btnMinhasPostagens, btnComentadas;

    private ForumManager forumManager;
    private String targetUserId;

    private enum Filtro {
        MINHAS_POSTAGENS, COMENTADAS
    }

    private Filtro filtroAtual = Filtro.MINHAS_POSTAGENS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("MinhasPostagensAct", "onCreate() chamado. Inicializando MyPostActivity.");
        setContentView(R.layout.forum_my_post_activity);

        forumManager = new ForumManager();
        String currentMySessionUid = forumManager.getCurrentUserId();
        String paramUserId = getIntent().getStringExtra("USER_ID");

        if (paramUserId != null && !paramUserId.isEmpty()) {
            targetUserId = paramUserId;
            if (currentMySessionUid != null && !paramUserId.equals(currentMySessionUid)) {
                setTitle("Postagens do Usuário");
            }
        } else if (currentMySessionUid != null) {
            targetUserId = currentMySessionUid;
        } else {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        inicializarUI();
        configurarRecyclerView();
        configurarBotoesFiltro();
        carregarPostagensContextualizadas();
    }

    private void inicializarUI() {
        recyclerView = findViewById(R.id.recyclerViewMinhasPostagens);
        emptyState = findViewById(R.id.textViewEmptyState);
        btnMinhasPostagens = findViewById(R.id.buttonFiltroMinhasPostagens);
        btnComentadas = findViewById(R.id.buttonFiltroComentadas);
    }

    private void configurarRecyclerView() {
        adapter = new PostAdapter(this, postList, false, true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void configurarBotoesFiltro() {
        btnMinhasPostagens.setAlpha(1.0f);
        btnComentadas.setAlpha(0.6f);

        btnMinhasPostagens.setOnClickListener(v -> {
            btnMinhasPostagens.setAlpha(1.0f);
            btnComentadas.setAlpha(0.6f);
            filtroAtual = Filtro.MINHAS_POSTAGENS;
            carregarPostagensContextualizadas();
        });

        btnComentadas.setOnClickListener(v -> {
            btnComentadas.setAlpha(1.0f);
            btnMinhasPostagens.setAlpha(0.6f);
            filtroAtual = Filtro.COMENTADAS;
            carregarPostagensContextualizadas();
        });
    }

    private void carregarPostagensContextualizadas() {
        postList.clear();
        adapter.notifyDataSetChanged();
        updateEmptyState(true);

        ForumManager.FeedCallback displayCallback = new ForumManager.FeedCallback() {
            @Override
            public void onPostsCarregados(List<Post> posts) {
                if(isFinishing() || isDestroyed()) return;
                postList.clear();
                postList.addAll(posts);
                adapter.notifyDataSetChanged();
                updateEmptyState(false);
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(MyPostActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                updateEmptyState(false);
            }
        };

        if (filtroAtual == Filtro.MINHAS_POSTAGENS) {
            forumManager.carregarMinhasPostagens(targetUserId, displayCallback);
        } else {
            forumManager.carregarPostagensComentadas(targetUserId, displayCallback);
        }
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
