package com.meuprojeto.eudaimoniaforum.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.meuprojeto.eudaimoniaforum.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConversasActivity extends AppCompatActivity {

    private RecyclerView recyclerViewConversas;
    private ConversasAdapter conversasAdapter;
    private List<Conversa> conversasList = new ArrayList<>();
    private List<Conversa> filteredList = new ArrayList<>();
    private View emptyState;
    private Button buttonTodas, buttonNaoLidas;
    private boolean mostrandoNaoLidas = false;

    private String currentUserId;

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

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        setupAdapter();
        setupFilterButtons();

        loadConversas();
    }

    private void setupAdapter() {
        conversasAdapter = new ConversasAdapter(filteredList, conversa -> {
            // Ao clicar, atualiza o status de lido nos metadados
            // Mas a atualização real acontece ao abrir o ChatActivity agora
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

    private DatabaseReference userConversasRef;
    private com.google.firebase.database.ChildEventListener userConversasListener;
    private final java.util.Map<String, ValueEventListener> chatListeners = new java.util.HashMap<>();
    private final java.util.Map<String, DatabaseReference> chatRefs = new java.util.HashMap<>();

    private void loadConversas() {
        // Carrega o índice de conversas do usuário
        userConversasRef = FirebaseDatabase.getInstance().getReference("user_conversas").child(currentUserId);

        userConversasListener = new com.google.firebase.database.ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String chatId = snapshot.getKey();
                if (chatId != null) {
                    monitorarDetalhesDoChat(chatId);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Se algo mudar no índice (raro), garantimos o monitoramento
                String chatId = snapshot.getKey();
                if (chatId != null && !chatListeners.containsKey(chatId)) {
                    monitorarDetalhesDoChat(chatId);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                String chatId = snapshot.getKey();
                if (chatId != null) {
                    removerMonitoramentoChat(chatId);
                    removerConversaDaLista(chatId);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ConversasActivity.this, "Erro ao carregar conversas.", Toast.LENGTH_SHORT).show();
            }
        };
        userConversasRef.addChildEventListener(userConversasListener);
    }

    private void monitorarDetalhesDoChat(String chatId) {
        if (chatListeners.containsKey(chatId))
            return; // Já estamos monitorando

        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot chatSnapshot) {
                if (chatSnapshot.exists()) {
                    processarConversa(chatSnapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };

        chatRef.addValueEventListener(listener);
        chatListeners.put(chatId, listener);
        chatRefs.put(chatId, chatRef);
    }

    private void removerMonitoramentoChat(String chatId) {
        if (chatListeners.containsKey(chatId)) {
            DatabaseReference ref = chatRefs.get(chatId);
            ValueEventListener listener = chatListeners.get(chatId);
            if (ref != null && listener != null) {
                ref.removeEventListener(listener);
            }
            chatListeners.remove(chatId);
            chatRefs.remove(chatId);
        }
    }

    private void removerConversaDaLista(String chatId) {
        for (int i = 0; i < conversasList.size(); i++) {
            if (conversasList.get(i).getChatId().equals(chatId)) {
                conversasList.remove(i);
                break;
            }
        }
        filterConversas();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userConversasRef != null && userConversasListener != null) {
            userConversasRef.removeEventListener(userConversasListener);
        }

        // Limpar todos os listeners de chat
        for (String chatId : chatListeners.keySet()) {
            DatabaseReference ref = chatRefs.get(chatId);
            ValueEventListener listener = chatListeners.get(chatId);
            if (ref != null && listener != null) {
                ref.removeEventListener(listener);
            }
        }
        chatListeners.clear();
        chatRefs.clear();
    }

    private void processarConversa(DataSnapshot chatSnapshot) {
        String chatId = chatSnapshot.getKey();
        if (chatId == null)
            return;

        // Assume formato uid1_uid2 para extrair ID do outro
        String otherUserId = chatId.replace(currentUserId, "").replace("_", "");

        Conversa conversa = new Conversa();
        conversa.setChatId(chatId);
        conversa.setOtherUserId(otherUserId);

        // Extrai dados diretos dos Metadados (Otimização)
        String ultimaMsg = chatSnapshot.child("ultimaMensagem").getValue(String.class);
        Long timestamp = chatSnapshot.child("timestamp").getValue(Long.class);

        conversa.setLastMessage(ultimaMsg != null ? ultimaMsg : "");
        conversa.setLastMessageTimestamp(timestamp != null ? timestamp : 0);

        // Lógica de "Não Lido": Se última msg é mais recente que minha leitura
        Long myReadTime = chatSnapshot.child("lidoPor").child(currentUserId).getValue(Long.class);
        boolean isUnread = (timestamp != null && (myReadTime == null || timestamp > myReadTime));
        conversa.setUnreadCount(isUnread ? 1 : 0);

        // Busca Info do Usuário (Nick, Avatar, Status)
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(otherUserId);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (!userSnapshot.exists())
                    return; // Usuário pode ter sido deletado

                conversa.setOtherUserNick(userSnapshot.child("nick").getValue(String.class));

                Long lastLogin = userSnapshot.child("lastLoginTimestamp").getValue(Long.class);
                long daysSinceLogin = TimeUnit.MILLISECONDS
                        .toDays(System.currentTimeMillis() - (lastLogin != null ? lastLogin : 0));
                conversa.setOtherUserStatus(daysSinceLogin < 10);

                if (conversa.getOtherUserNick() == null) {
                    conversa.setOtherUserNick("Usuário Desconhecido");
                }

                addOrUpdateConversa(conversa);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Mesmo com erro no user, tentamos mostrar a conversa
                addOrUpdateConversa(conversa);
            }
        });
    }

    private void addOrUpdateConversa(Conversa novaConversa) {
        if (novaConversa == null || novaConversa.getChatId() == null || novaConversa.getOtherUserId() == null) {
            return;
        }

        // Verifica se já existe na lista para atualizar
        int index = -1;
        for (int i = 0; i < conversasList.size(); i++) {
            if (conversasList.get(i).getChatId().equals(novaConversa.getChatId())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            conversasList.set(index, novaConversa);
        } else {
            conversasList.add(novaConversa);
        }

        // Ordena por data (mais recente primeiro)
        Collections.sort(conversasList,
                (c1, c2) -> Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));
        filterConversas();
    }

    private void filterConversas() {
        filteredList.clear();
        if (mostrandoNaoLidas) {
            for (Conversa c : conversasList) {
                if (c.getUnreadCount() > 0) {
                    filteredList.add(c);
                }
            }
        } else {
            filteredList.addAll(conversasList);
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
}
