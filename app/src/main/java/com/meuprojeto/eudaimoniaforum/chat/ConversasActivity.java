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

public class ConversasActivity extends AppCompatActivity {

    private RecyclerView recyclerViewConversas;
    private ConversasAdapter conversasAdapter;
    private List<Conversa> conversasListRaw = new ArrayList<>();
    private List<Conversa> filteredList = new ArrayList<>();
    
    private View emptyState;
    private Button buttonTodas, buttonNaoLidas;
    private boolean mostrandoNaoLidas = false;

    private ConversasManager conversasManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ConversasActivity", "onCreate() chamado. Inicializando ConversasActivity.");
        setContentView(R.layout.chat_conversas_activity);

        recyclerViewConversas = findViewById(R.id.recyclerViewConversas);
        emptyState = findViewById(R.id.emptyStateLayout);
        buttonTodas = findViewById(R.id.buttonTodasConversas);
        buttonNaoLidas = findViewById(R.id.buttonNaoLidas);
        
        recyclerViewConversas.setLayoutManager(new LinearLayoutManager(this));

        conversasManager = new ConversasManager();
        if (!conversasManager.isUserAuthenticated()) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupAdapter();
        setupFilterButtons();

        conversasManager.iniciarMonitoramento(new ConversasManager.ConversasListUpdateCallback() {
            @Override
            public void onListaAtualizada(List<Conversa> listaOrdenada) {
                if(isFinishing() || isDestroyed()) return;
                conversasListRaw = listaOrdenada;
                filterConversas();
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(ConversasActivity.this, erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupAdapter() {
        conversasAdapter = new ConversasAdapter(filteredList, conversa -> {
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra("USER_ID", conversa.getOtherUserId());
            startActivity(intent);
        });
        recyclerViewConversas.setAdapter(conversasAdapter);
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
            for (Conversa c : conversasListRaw) {
                if (c.getUnreadCount() > 0) {
                    filteredList.add(c);
                }
            }
        } else {
            filteredList.addAll(conversasListRaw);
        }

        if (conversasAdapter != null) {
            conversasAdapter.notifyDataSetChanged();
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
        if (conversasManager != null) {
            conversasManager.destruir();
        }
    }
}
