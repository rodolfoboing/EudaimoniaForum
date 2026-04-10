package com.meuprojeto.eudaimoniaforum.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.meuprojeto.eudaimoniaforum.R;

import java.util.ArrayList;
import java.util.List;

public class ConversationActivity extends AppCompatActivity {

    private RecyclerView recyclerViewConversas;
    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversasListRaw = new ArrayList<>();
    private List<Conversation> filteredList = new ArrayList<>();
    
    private View emptyState;
    private Button buttonTodas, buttonNaoLidas;
    private boolean mostrandoNaoLidas = false;

    private ConversationManager conversationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ConversationActivity", "onCreate() chamado. Inicializando ConversationActivity.");
        setContentView(R.layout.chat_conversas_activity);

        recyclerViewConversas = findViewById(R.id.recyclerViewConversas);
        emptyState = findViewById(R.id.emptyStateLayout);
        buttonTodas = findViewById(R.id.buttonTodasConversas);
        buttonNaoLidas = findViewById(R.id.buttonNaoLidas);
        
        recyclerViewConversas.setLayoutManager(new LinearLayoutManager(this));

        conversationManager = new ConversationManager();
        if (!conversationManager.isUserAuthenticated()) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupAdapter();
        setupFilterButtons();

        conversationManager.iniciarMonitoramento(new ConversationManager.ConversasListUpdateCallback() {
            @Override
            public void onListaAtualizada(List<Conversation> listaOrdenada) {
                if(isFinishing() || isDestroyed()) return;
                conversasListRaw = listaOrdenada;
                filterConversas();
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(ConversationActivity.this, erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAdapter() {
        conversationAdapter = new ConversationAdapter(filteredList, conversation -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("USER_ID", conversation.getOtherUserId());
            startActivity(intent);
        });
        recyclerViewConversas.setAdapter(conversationAdapter);
    }

    private void setupFilterButtons() {
        buttonTodas.setOnClickListener(v -> {
            mostrandoNaoLidas = false;
            filterConversas();
        });
        
        buttonNaoLidas.setOnClickListener(v -> {
            mostrandoNaoLidas = true;
            filterConversas();
        });
    }

    private void filterConversas() {
        filteredList.clear();
        if (mostrandoNaoLidas) {
            for (Conversation c : conversasListRaw) {
                if (c.getUnreadCount() > 0) {
                    filteredList.add(c);
                }
            }
        } else {
            filteredList.addAll(conversasListRaw);
        }

        if (conversationAdapter != null) {
            conversationAdapter.notifyDataSetChanged();
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (filteredList.isEmpty()) {
            recyclerViewConversas.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerViewConversas.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conversationManager != null) {
            conversationManager.destruir();
        }
    }
}
