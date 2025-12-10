package com.meuprojeto.eudaimoniaforum;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import java.util.concurrent.TimeUnit;

public class ConversasActivity extends AppCompatActivity {

    private RecyclerView recyclerViewConversas;
    private ConversasAdapter conversasAdapter;
    private List<Conversa> conversasList = new ArrayList<>();
    private List<Conversa> filteredList = new ArrayList<>();
    private View emptyState;
    private Button buttonTodas, buttonNaoLidas;
    private boolean mostrandoNaoLidas = false;

    private DatabaseReference chatsRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversas);

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

        // Referência mantida para uso no Adapter
        chatsRef = FirebaseDatabase.getInstance().getReference("chats");

        setupAdapter();
        setupFilterButtons();

        loadConversas();
    }

    private void setupAdapter() {
        conversasAdapter = new ConversasAdapter(filteredList, conversa -> {
            DatabaseReference readStatusRef = chatsRef.child(conversa.getChatId()).child("readStatus").child(currentUserId);
            readStatusRef.setValue(System.currentTimeMillis());

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

    private void loadConversas() {
        // Correção: Buscar apenas os chats do usuário através do índice "user_conversas"
        // Isso evita o erro de PERMISSION_DENIED ao tentar ler "chats" inteiro
        DatabaseReference userConversasRef = FirebaseDatabase.getInstance().getReference("user_conversas").child(currentUserId);

        userConversasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                conversasList.clear();
                filterConversas(); // Limpa a lista atual na UI

                if (!snapshot.exists()) {
                    updateEmptyState();
                    return;
                }

                // Para cada ID de chat encontrado, carrega os detalhes do chat
                for (DataSnapshot chatRefSnap : snapshot.getChildren()) {
                    String chatId = chatRefSnap.getKey();
                    if (chatId != null) {
                        carregarDetalhesDoChat(chatId);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ConversasActivity.this, "Erro ao carregar conversas.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void carregarDetalhesDoChat(String chatId) {
        FirebaseDatabase.getInstance().getReference("chats").child(chatId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot chatSnapshot) {
                if (chatSnapshot.exists()) {
                    processarConversa(chatSnapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Falha silenciosa ao carregar um chat específico
            }
        });
    }

    private void processarConversa(DataSnapshot chatSnapshot) {
        String chatId = chatSnapshot.getKey();
        // Assume formato uid1_uid2
        String otherUserId = chatId.replace(currentUserId, "").replace("_", "");

        Conversa conversa = new Conversa();
        conversa.setChatId(chatId);
        conversa.setOtherUserId(otherUserId);

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(otherUserId);
        DatabaseReference messagesRef = chatSnapshot.child("messages").getRef();
        DatabaseReference readStatusRef = chatSnapshot.child("readStatus").child(currentUserId).getRef();

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (!userSnapshot.exists()) return;
                conversa.setOtherUserNick(userSnapshot.child("nick").getValue(String.class));
                long lastLogin = userSnapshot.child("lastLoginTimestamp").getValue(Long.class) != null ? userSnapshot.child("lastLoginTimestamp").getValue(Long.class) : 0;
                long daysSinceLogin = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastLogin);
                conversa.setOtherUserStatus(daysSinceLogin < 10);

                messagesRef.orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot msgSnapshot) {
                        if (msgSnapshot.exists()) {
                            for (DataSnapshot lastMsg : msgSnapshot.getChildren()) {
                                ChatMessage lastMessage = lastMsg.getValue(ChatMessage.class);
                                conversa.setLastMessage(lastMessage.getMessageText());
                                conversa.setLastMessageTimestamp(lastMessage.getTimestamp());
                            }
                        }

                        readStatusRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot readSnapshot) {
                                long lastReadTimestamp = readSnapshot.exists() ? readSnapshot.getValue(Long.class) : 0;
                                messagesRef.orderByChild("timestamp").startAt(lastReadTimestamp + 1).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot unreadSnapshot) {
                                        int unreadCount = 0;
                                        for(DataSnapshot msg : unreadSnapshot.getChildren()){
                                            ChatMessage chatMessage = msg.getValue(ChatMessage.class);
                                            if(chatMessage != null && !chatMessage.getSenderId().equals(currentUserId)){
                                                unreadCount++;
                                            }
                                        }
                                        conversa.setUnreadCount(unreadCount);
                                        addOrUpdateConversa(conversa);
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) { addOrUpdateConversa(conversa); }
                                });
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) { addOrUpdateConversa(conversa); }
                        });
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) { addOrUpdateConversa(conversa); }
                });
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void addOrUpdateConversa(Conversa novaConversa) {
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

        Collections.sort(conversasList, (c1, c2) -> Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));
        filterConversas();
    }

    private void filterConversas() {
        filteredList.clear();
        if (mostrandoNaoLidas) { // Filtro "Não Lidas"
            for (Conversa c : conversasList) {
                if (c.getUnreadCount() > 0) {
                    filteredList.add(c);
                }
            }
        } else { // Filtro "Todas"
            filteredList.addAll(conversasList);
        }
        conversasAdapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState(){
        if (filteredList.isEmpty()) {
            recyclerViewConversas.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerViewConversas.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }
}
