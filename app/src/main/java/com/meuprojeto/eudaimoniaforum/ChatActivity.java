package com.meuprojeto.eudaimoniaforum;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerViewChat;
    private EditText editTextChatMessage;
    private ImageButton buttonSendMessage, buttonOpcoes;
    private TextView textViewNomeChat, textViewStatusOnline;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    private String receiverId, currentUserId, chatId;
    private DatabaseReference chatRef, userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        receiverId = getIntent().getStringExtra("USER_ID");

        inicializarUI();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || receiverId == null) {
            Toast.makeText(this, "Erro: Usuário não identificado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        chatId = getChatId(currentUserId, receiverId);
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");
        userRef = FirebaseDatabase.getInstance().getReference("users");

        setupRecyclerView();
        loadMessages();
        carregarDadosDoCabecalho();

        buttonSendMessage.setOnClickListener(v -> sendMessage());
        buttonOpcoes.setOnClickListener(this::showChatMenu);
    }

    private void inicializarUI() {
        recyclerViewChat = findViewById(R.id.recyclerViewChat);
        editTextChatMessage = findViewById(R.id.editTextChatMessage);
        buttonSendMessage = findViewById(R.id.buttonSendMessage);
        textViewNomeChat = findViewById(R.id.textViewNomeChat);
        textViewStatusOnline = findViewById(R.id.textViewStatusOnline);
        buttonOpcoes = findViewById(R.id.buttonOpcoes);
    }

    private void showChatMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.chat_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_apagar_conversa) {
                apagarConversa();
                return true;
            } else if (itemId == R.id.menu_bloquear_usuario) {
                bloquearUsuario();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void apagarConversa() {
        new AlertDialog.Builder(this)
                .setTitle("Apagar Conversa")
                .setMessage("Tem certeza que deseja apagar esta conversa? A ação não pode ser desfeita.")
                .setPositiveButton("Sim", (dialog, which) -> {
                    chatRef.getParent().removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Conversa apagada.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void bloquearUsuario() {
        new AlertDialog.Builder(this)
                .setTitle("Bloquear Usuário")
                .setMessage("Tem certeza que deseja bloquear este usuário? Você não receberá mais mensagens dele.")
                .setPositiveButton("Sim", (dialog, which) -> {
                    userRef.child(currentUserId).child("hasBlocked").child(receiverId).setValue(true).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Usuário bloqueado.", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void sendMessage() {
        String messageText = editTextChatMessage.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) return;

        // Verifica se o destinatário te bloqueou
        userRef.child(receiverId).child("hasBlocked").child(currentUserId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                Toast.makeText(this, "Você não pode enviar mensagens para este usuário.", Toast.LENGTH_SHORT).show();
                return;
            }
            // Se não, envia a mensagem
            long timestamp = System.currentTimeMillis();
            ChatMessage chatMessage = new ChatMessage(messageText, currentUserId, receiverId, timestamp);
            
            // Usar updateChildren para garantir que "membros" seja escrito JUNTO com a mensagem.
            // Isso satisfaz a regra de segurança que exige que membros contenha auth.uid.
            DatabaseReference chatRoot = FirebaseDatabase.getInstance().getReference("chats").child(chatId);
            String messageKey = chatRoot.child("messages").push().getKey();
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("messages/" + messageKey, chatMessage);
            updates.put("membros/" + currentUserId, true);
            updates.put("membros/" + receiverId, true);
            
            chatRoot.updateChildren(updates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    editTextChatMessage.setText("");
                    enviarNotificacao();
                    
                    // Atualiza índice de conversas (para listar na ConversasActivity)
                    atualizarIndicesDeConversa();
                } else {
                    Toast.makeText(this, "Falha ao enviar mensagem: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void atualizarIndicesDeConversa() {
        DatabaseReference userConversas = FirebaseDatabase.getInstance().getReference("user_conversas");
        userConversas.child(currentUserId).child(chatId).setValue(true);
        userConversas.child(receiverId).child(chatId).setValue(true);
    }

    private void enviarNotificacao() {
        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Usuario user = snapshot.getValue(Usuario.class);
                String nomeRemetente = (user != null) ? user.getNick() : "Alguém";
                String mensagemNotificacao = "Nova mensagem de " + nomeRemetente;

                DatabaseReference notificacaoRef = FirebaseDatabase.getInstance().getReference("notificacoes").child(receiverId).push();
                String notifId = notificacaoRef.getKey();

                Notificacao notificacao = new Notificacao(notifId, "chat", mensagemNotificacao, currentUserId, System.currentTimeMillis());
                notificacaoRef.setValue(notificacao);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void carregarDadosDoCabecalho() {
        DatabaseReference receiverRef = FirebaseDatabase.getInstance().getReference("users").child(receiverId);
        receiverRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Usuario user = snapshot.getValue(Usuario.class);
                    if (user != null) {
                        textViewNomeChat.setText(user.getNick());
                        long lastLogin = user.getLastLoginTimestamp();
                        long daysSinceLogin = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - lastLogin);
                        if (daysSinceLogin < 10) {
                            textViewStatusOnline.setText("🟢 Ativo");
                            textViewStatusOnline.setTextColor(Color.parseColor("#C5E1A5"));
                        } else {
                            textViewStatusOnline.setText("⚪ Inativo");
                            textViewStatusOnline.setTextColor(Color.GRAY);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { textViewNomeChat.setText("Usuário desconhecido"); }
        });
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(chatAdapter);
    }

    private void loadMessages() {
        chatRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatMessages.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatMessage message = dataSnapshot.getValue(ChatMessage.class);
                    if (message != null) {
                        chatMessages.add(message);
                        if (!message.getSenderId().equals(currentUserId) && !"lido".equals(message.getStatus())) {
                            marcarMensagemComoLida(dataSnapshot.getKey());
                        }
                    }
                }
                chatAdapter.notifyDataSetChanged();
                recyclerViewChat.scrollToPosition(chatMessages.size() - 1);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void marcarMensagemComoLida(String messageKey) {
        if (messageKey != null) {
            chatRef.child(messageKey).child("status").setValue("lido");
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
