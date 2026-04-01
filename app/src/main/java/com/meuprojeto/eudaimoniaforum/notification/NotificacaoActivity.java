package com.meuprojeto.eudaimoniaforum.notification;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.chat.ChatActivity;
import com.meuprojeto.eudaimoniaforum.forum.ComentarioActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificacaoActivity extends AppCompatActivity implements NotificacaoAdapter.OnItemClickListener {

    private RecyclerView recyclerViewNotificacoes;
    private NotificacaoAdapter notificacaoAdapter;
    private List<Notificacao> notificacoes;
    private DatabaseReference notificacoesRef;
    private Button buttonLimparTudo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("NotificacaoActivity", "onCreate() chamado. Inicializando NotificacaoActivity.");
        setContentView(R.layout.notificacao_activity);

        recyclerViewNotificacoes = findViewById(R.id.recyclerViewNotificacoes);
        buttonLimparTudo = findViewById(R.id.buttonLimparTudo);
        recyclerViewNotificacoes.setLayoutManager(new LinearLayoutManager(this));

        notificacoes = new ArrayList<>();
        notificacaoAdapter = new NotificacaoAdapter(notificacoes, this);
        recyclerViewNotificacoes.setAdapter(notificacaoAdapter);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes").child(userId);

        carregarNotificacoes();

        // Configura o clique do botão para limpar as notificações
        buttonLimparTudo.setOnClickListener(v -> {
            if (notificacoesRef != null) {
                notificacoesRef.removeValue();
                Toast.makeText(this, "Notificações limpas.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void carregarNotificacoes() {
        notificacoesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                notificacoes.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Notificacao notificacao = snapshot.getValue(Notificacao.class);
                    if (notificacao != null) {
                        notificacao.setId(snapshot.getKey());
                        notificacoes.add(notificacao);
                    }
                }
                // Inverte a lista para mostrar as mais recentes primeiro
                Collections.reverse(notificacoes);
                notificacaoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(NotificacaoActivity.this, "Erro ao carregar notificações", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(Notificacao notificacao) {
        if (notificacao == null || notificacao.getTipo() == null || notificacao.getIdReferencia() == null) {
            return;
        }

        if (notificacoesRef != null && notificacao.getId() != null) {
            notificacoesRef.child(notificacao.getId()).child("lida").setValue(true);
        }

        if (notificacao.getTipo().equals("comentario")) {
            Intent intent = new Intent(this, ComentarioActivity.class);
            intent.putExtra("POST_ID", notificacao.getIdReferencia());
            startActivity(intent);
        } else if (notificacao.getTipo().equals("chat")) {
            Intent intent = new Intent(this, ChatActivity.class);
            // O idReferencia para chat guarda o ID do outro usuário
            intent.putExtra("USER_ID", notificacao.getIdReferencia());
            startActivity(intent);
        }
    }
}
