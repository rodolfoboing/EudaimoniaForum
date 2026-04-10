package com.meuprojeto.eudaimoniaforum.chat;

import android.graphics.Color;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.profile.User;
import com.meuprojeto.eudaimoniaforum.utils.AppLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChatManager {

    private final String currentUserId;
    private final String receiverId;
    private final String chatId;

    private final DatabaseReference messagesRef;
    private final DatabaseReference userRef;
    private final DatabaseReference receiverRef;

    private ChildEventListener messagesListener;
    private ValueEventListener headerListener;

    private static long lastMessageTimestamp = 0;

    public interface ChatUpdateListener {
        void onMessageAdded(ChatMessage message);
        void onMessageChanged(ChatMessage message);
        void onMessageRemoved(String messageId);
        void onHeaderUpdated(String nome, String statusText, int statusColor);
        void onActionSuccess(String message);
        void onActionFailure(String error);
    }

    public ChatManager(String currentUserId, String receiverId) {
        this.currentUserId = currentUserId;
        this.receiverId = receiverId;
        this.chatId = getChatId(currentUserId, receiverId);

        this.messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId);
        this.userRef = FirebaseDatabase.getInstance().getReference("users");
        this.receiverRef = userRef.child(receiverId);
    }

    public void atualizarStatusLidoNoChat() {
        DatabaseReference readRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("lidoPor")
                .child(currentUserId);
        readRef.setValue(System.currentTimeMillis());
    }

    public void apagarConversa(ChatUpdateListener listener) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("/messages/" + chatId, null);
        updates.put("/chats/" + chatId, null); // Apaga metadata
        updates.put("/user_conversas/" + currentUserId + "/" + chatId, null);
        updates.put("/user_conversas/" + receiverId + "/" + chatId, null);

        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && listener != null) {
                        listener.onActionSuccess("Conversation apagada.");
                    }
                });
    }

    public void bloquearUsuario(ChatUpdateListener listener) {
        userRef.child(currentUserId).child("hasBlocked").child(receiverId).setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && listener != null) {
                        listener.onActionSuccess("Usuário bloqueado.");
                    }
                });
    }

    public void sendMessage(String messageText, ChatUpdateListener listener, Runnable onMessageSent) {
        if (TextUtils.isEmpty(messageText)) return;

        long currentTime = System.currentTimeMillis();
        long cooldownMillis = 2000; // 2 segundos
        if (currentTime - lastMessageTimestamp < cooldownMillis) {
            if (listener != null) listener.onActionFailure("Você está enviando mensagens rápido demais. Aguarde um instante.");
            AppLogger.logSpam(currentUserId, "ChatPrivadoId_" + chatId);
            return;
        }

        userRef.child(receiverId).child("hasBlocked").child(currentUserId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                if (listener != null) listener.onActionFailure("Você não pode enviar mensagens para este usuário.");
                return;
            }

            long timestamp = currentTime;
            ChatMessage chatMessage = new ChatMessage(messageText, currentUserId, receiverId, timestamp);
            String messageKey = messagesRef.push().getKey();

            Map<String, Object> updates = new HashMap<>();
            updates.put("/messages/" + chatId + "/" + messageKey, chatMessage);
            updates.put("/chats/" + chatId + "/ultimaMensagem", messageText);
            updates.put("/chats/" + chatId + "/timestamp", timestamp);
            updates.put("/chats/" + chatId + "/membros/" + currentUserId, true);
            updates.put("/chats/" + chatId + "/membros/" + receiverId, true);
            updates.put("/chats/" + chatId + "/lidoPor/" + currentUserId, timestamp);
            updates.put("/user_conversas/" + currentUserId + "/" + chatId, true);
            updates.put("/user_conversas/" + receiverId + "/" + chatId, true);

            FirebaseDatabase.getInstance().getReference().updateChildren(updates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    lastMessageTimestamp = currentTime;
                    if (onMessageSent != null) onMessageSent.run();
                } else {
                    if (listener != null) listener.onActionFailure("Falha ao enviar mensagem.");
                    if (task.getException() != null) AppLogger.logDbError("Chat_SendMessage", task.getException().getMessage());
                }
            });
        });
    }

    public void carregarDadosDoCabecalho(ChatUpdateListener listener) {
        headerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        long lastLogin = user.getLastLoginTimestamp();
                        long timeDiff = System.currentTimeMillis() - lastLogin;
                        String statusText;
                        int statusColor;

                        if (timeDiff < TimeUnit.MINUTES.toMillis(5)) {
                            statusText = "🟢 Online";
                            statusColor = Color.parseColor("#4CAF50");
                        } else {
                            statusText = "⚪ Offline";
                            statusColor = Color.GRAY;
                        }

                        if (listener != null) {
                            listener.onHeaderUpdated(user.getNick(), statusText, statusColor);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) {
                    listener.onHeaderUpdated("Usuário desconhecido", "", Color.GRAY);
                }
            }
        };
        receiverRef.addValueEventListener(headerListener);
    }

    public void loadMessages(ChatUpdateListener listener) {
        messagesRef.keepSynced(true);

        messagesListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                ChatMessage message = dataSnapshot.getValue(ChatMessage.class);
                if (message != null) {
                    message.setId(dataSnapshot.getKey());

                    if (!message.getSenderId().equals(currentUserId) && !"lido".equals(message.getStatus())) {
                        messagesRef.child(message.getId()).child("status").setValue("lido");
                    }

                    atualizarStatusLidoNoChat();

                    if (listener != null) {
                        listener.onMessageAdded(message);
                    }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                ChatMessage updatedMessage = dataSnapshot.getValue(ChatMessage.class);
                if (updatedMessage != null) {
                    updatedMessage.setId(dataSnapshot.getKey());
                    if (listener != null) listener.onMessageChanged(updatedMessage);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                String removedId = dataSnapshot.getKey();
                if (removedId != null && listener != null) {
                    listener.onMessageRemoved(removedId);
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {}

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        messagesRef.addChildEventListener(messagesListener);
    }

    public void removeListeners() {
        if (receiverRef != null && headerListener != null) {
            receiverRef.removeEventListener(headerListener);
        }
        if (messagesRef != null && messagesListener != null) {
            messagesRef.removeEventListener(messagesListener);
        }
    }

    private String getChatId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) > 0) {
            return userId1 + "_" + userId2;
        } else {
            return userId2 + "_" + userId1;
        }
    }
}
