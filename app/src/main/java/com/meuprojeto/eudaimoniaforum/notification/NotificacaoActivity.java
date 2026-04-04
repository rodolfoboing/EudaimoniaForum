package com.meuprojeto.eudaimoniaforum.notification;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.chat.ChatActivity;
import com.meuprojeto.eudaimoniaforum.forum.ComentarioActivity;

import java.util.ArrayList;
import java.util.List;

public class NotificacaoActivity extends AppCompatActivity implements NotificacaoAdapter.OnItemClickListener {

    private RecyclerView recyclerViewNotificacoes;
    private NotificacaoAdapter notificacaoAdapter;
    private List<Notificacao> notificacoes;
    private Button buttonLimparTudo;

    private NotificacaoManager notificacaoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("NotificacaoActivity", "onCreate() chamado. Inicializando NotificacaoActivity.");
        setContentView(R.layout.notificacao_activity);

        notificacaoManager = new NotificacaoManager();

        if (notificacaoManager.getCurrentUserId() == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerViewNotificacoes = findViewById(R.id.recyclerViewNotificacoes);
        buttonLimparTudo = findViewById(R.id.buttonLimparTudo);
        recyclerViewNotificacoes.setLayoutManager(new LinearLayoutManager(this));

        notificacoes = new ArrayList<>();
        notificacaoAdapter = new NotificacaoAdapter(notificacoes, this);
        recyclerViewNotificacoes.setAdapter(notificacaoAdapter);

        carregarNotificacoes();

        buttonLimparTudo.setOnClickListener(v -> {
            notificacaoManager.limparTodas(new NotificacaoManager.AcaoCallback() {
                @Override
                public void onSuccess() {
                    if(isFinishing() || isDestroyed()) return;
                    Toast.makeText(NotificacaoActivity.this, "Notificações limpas.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String erro) {
                    if(isFinishing() || isDestroyed()) return;
                    Toast.makeText(NotificacaoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void carregarNotificacoes() {
        notificacaoManager.monitorarNotificacoes(new NotificacaoManager.FeedCallback() {
            @Override
            public void onLoaded(List<Notificacao> dados) {
                if(isFinishing() || isDestroyed()) return;
                notificacoes.clear();
                notificacoes.addAll(dados);
                notificacaoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(NotificacaoActivity.this, "Erro ao carregar notificações: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(Notificacao notificacao) {
        if (notificacao == null || notificacao.getTipo() == null || notificacao.getIdReferencia() == null) {
            return;
        }

        if (notificacao.getId() != null) {
            notificacaoManager.marcarComoLida(notificacao.getId());
        }

        if (notificacao.getTipo().equals("comentario")) {
            Intent intent = new Intent(this, ComentarioActivity.class);
            intent.putExtra("POST_ID", notificacao.getIdReferencia());
            startActivity(intent);
        } else if (notificacao.getTipo().equals("chat")) {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("USER_ID", notificacao.getIdReferencia());
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(notificacaoManager != null) {
            notificacaoManager.removerListeners();
        }
    }
}
