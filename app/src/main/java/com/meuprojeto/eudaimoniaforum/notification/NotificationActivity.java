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
import com.meuprojeto.eudaimoniaforum.forum.CommentActivity;

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.OnItemClickListener {

    private RecyclerView recyclerViewNotificacoes;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificacoes;
    private Button buttonLimparTudo;

    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("NotificationActivity", "onCreate() chamado. Inicializando NotificationActivity.");
        setContentView(R.layout.notificacao_activity);

        notificationManager = new NotificationManager();

        if (notificationManager.getCurrentUserId() == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recyclerViewNotificacoes = findViewById(R.id.recyclerViewNotificacoes);
        buttonLimparTudo = findViewById(R.id.buttonLimparTudo);
        recyclerViewNotificacoes.setLayoutManager(new LinearLayoutManager(this));

        notificacoes = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(notificacoes, this);
        recyclerViewNotificacoes.setAdapter(notificationAdapter);

        carregarNotificacoes();

        buttonLimparTudo.setOnClickListener(v -> {
            notificationManager.limparTodas(new NotificationManager.AcaoCallback() {
                @Override
                public void onSuccess() {
                    if(isFinishing() || isDestroyed()) return;
                    Toast.makeText(NotificationActivity.this, "Notificações limpas.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(String erro) {
                    if(isFinishing() || isDestroyed()) return;
                    Toast.makeText(NotificationActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void carregarNotificacoes() {
        notificationManager.monitorarNotificacoes(new NotificationManager.FeedCallback() {
            @Override
            public void onLoaded(List<Notification> dados) {
                if(isFinishing() || isDestroyed()) return;
                notificacoes.clear();
                notificacoes.addAll(dados);
                notificationAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(NotificationActivity.this, "Erro ao carregar notificações: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(Notification notification) {
        if (notification == null || notification.getTipo() == null || notification.getIdReferencia() == null) {
            return;
        }

        if (notification.getId() != null) {
            notificationManager.marcarComoLida(notification.getId());
        }

        if (notification.getTipo().equals("comentario")) {
            Intent intent = new Intent(this, CommentActivity.class);
            intent.putExtra("POST_ID", notification.getIdReferencia());
            startActivity(intent);
        } else if (notification.getTipo().equals("chat")) {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("USER_ID", notification.getIdReferencia());
            startActivity(intent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(notificationManager != null) {
            notificationManager.removerListeners();
        }
    }
}
