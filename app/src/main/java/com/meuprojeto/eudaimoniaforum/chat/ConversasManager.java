package com.meuprojeto.eudaimoniaforum.chat;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ConversasManager {
    private static final String TAG = "ConversasManager";

    private final String currentUserId;
    private DatabaseReference userConversasRef;
    private ChildEventListener userConversasListener;

    private final Map<String, ValueEventListener> chatListeners = new HashMap<>();
    private final Map<String, DatabaseReference> chatRefs = new HashMap<>();

    private final List<Conversa> conversasList = new ArrayList<>();

    public interface ConversasListUpdateCallback {
        void onListaAtualizada(List<Conversa> listaOrdenada);
        void onError(String erro);
    }

    private ConversasListUpdateCallback updateCallback;

    public ConversasManager() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (currentUser != null) ? currentUser.getUid() : "";
    }

    public boolean isUserAuthenticated() {
        return !currentUserId.isEmpty();
    }

    public void iniciarMonitoramento(ConversasListUpdateCallback callback) {
        if (!isUserAuthenticated()) return;
        this.updateCallback = callback;
        
        userConversasRef = FirebaseDatabase.getInstance().getReference("user_conversas").child(currentUserId);

        userConversasListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String chatId = snapshot.getKey();
                if (chatId != null) {
                    monitorarDetalhesDoChat(chatId);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
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
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if(updateCallback != null) {
                    updateCallback.onError("Erro ao carregar conversas: " + error.getMessage());
                }
            }
        };
        userConversasRef.addChildEventListener(userConversasListener);
    }

    private void monitorarDetalhesDoChat(String chatId) {
        if (chatListeners.containsKey(chatId)) return;

        DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot chatSnapshot) {
                if (chatSnapshot.exists()) {
                    processarConversa(chatSnapshot);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
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
        notificarView();
    }

    private void processarConversa(DataSnapshot chatSnapshot) {
        String chatId = chatSnapshot.getKey();
        if (chatId == null) return;

        String otherUserId = chatId.replace(currentUserId, "").replace("_", "");

        Conversa conversa = new Conversa();
        conversa.setChatId(chatId);
        conversa.setOtherUserId(otherUserId);

        String ultimaMsg = chatSnapshot.child("ultimaMensagem").getValue(String.class);
        Long timestamp = chatSnapshot.child("timestamp").getValue(Long.class);

        conversa.setLastMessage(ultimaMsg != null ? ultimaMsg : "");
        conversa.setLastMessageTimestamp(timestamp != null ? timestamp : 0);

        Long myReadTime = chatSnapshot.child("lidoPor").child(currentUserId).getValue(Long.class);
        boolean isUnread = (timestamp != null && (myReadTime == null || timestamp > myReadTime));
        conversa.setUnreadCount(isUnread ? 1 : 0);

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(otherUserId);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (!userSnapshot.exists()) return;

                conversa.setOtherUserNick(userSnapshot.child("nick").getValue(String.class));

                Long lastLogin = userSnapshot.child("lastLoginTimestamp").getValue(Long.class);
                long daysSinceLogin = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - (lastLogin != null ? lastLogin : 0));
                conversa.setOtherUserStatus(daysSinceLogin < 10);

                if (conversa.getOtherUserNick() == null) {
                    conversa.setOtherUserNick("Usuário Desconhecido");
                }
                
                addOrUpdateConversa(conversa);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                addOrUpdateConversa(conversa);
            }
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

        notificarView();
    }

    private void notificarView() {
        if (updateCallback != null) {
            // Sort list before returning
            List<Conversa> listaCopia = new ArrayList<>(conversasList);
            Collections.sort(listaCopia, (c1, c2) -> Long.compare(c2.getLastMessageTimestamp(), c1.getLastMessageTimestamp()));
            updateCallback.onListaAtualizada(listaCopia);
        }
    }

    public void destruir() {
        if (userConversasRef != null && userConversasListener != null) {
            userConversasRef.removeEventListener(userConversasListener);
        }
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
}
