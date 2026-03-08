package com.meuprojeto.eudaimoniaforum;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

    private static final String TAG = "ChatActivity";

    private RecyclerView recyclerViewChat;
    private EditText editTextChatMessage;
    private ImageButton buttonSendMessage, buttonOpcoes;
    private TextView textViewNomeChat, textViewStatusOnline;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    public static String activeChatUserId = null;

    private String receiverId, currentUserId, chatId;
    private DatabaseReference messagesRef;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ChatActivity", "onCreate() called. Inicializando ChatActivity.");
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

        // Mensagens ficam em "messages/{chatId}" para não pesar o
        // carregamento dos metadados
        messagesRef = FirebaseDatabase.getInstance().getReference("messages").child(chatId);
        userRef = FirebaseDatabase.getInstance().getReference("users");

        setupRecyclerView();
        loadMessages();
        carregarDadosDoCabecalho();

        // Marca a conversa como lida ao entrar
        atualizarStatusLidoNoChat();

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

    private void atualizarStatusLidoNoChat() {
        // Atualiza o timestamp de leitura do usuário nos metadados do chat
        DatabaseReference readRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("lidoPor")
                .child(currentUserId);
        readRef.setValue(System.currentTimeMillis());
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeChatUserId = receiverId;
        limparNotificacaoDeChat();
    }

    @Override
    protected void onPause() {
        super.onPause();
        activeChatUserId = null;
    }

    private void limparNotificacaoDeChat() {
        if (currentUserId == null || receiverId == null)
            return;
        Log.d(TAG, "Silenciador de Chat: Limpando alertas e notificações pendentes do usuário " + receiverId);
        // Quando entra no chat, automaticamente apaga se houver notificação pendente no
        // servidor
        DatabaseReference notificacaoRef = FirebaseDatabase.getInstance().getReference("notificacoes")
                .child(currentUserId).child("chat_" + receiverId);
        notificacaoRef.removeValue().addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Silenciador de Chat: Limpeza concluída do histórico na nuvem (se existia).");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Silenciador de Chat: Falha ao tentar limpar: " + e.getMessage());
        });
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
                    // Remoção Atômica Multicaminho (Apaga Metadados, Mensagens e Índices)
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("/messages/" + chatId, null);
                    updates.put("/chats/" + chatId, null); // Apaga metadata
                    updates.put("/user_conversas/" + currentUserId + "/" + chatId, null);
                    updates.put("/user_conversas/" + receiverId + "/" + chatId, null);

                    FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                            .addOnCompleteListener(task -> {
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
                    userRef.child(currentUserId).child("hasBlocked").child(receiverId).setValue(true)
                            .addOnCompleteListener(task -> {
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
        if (TextUtils.isEmpty(messageText))
            return;

        // Verifica se o destinatário te bloqueou
        userRef.child(receiverId).child("hasBlocked").child(currentUserId).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                Toast.makeText(this, "Você não pode enviar mensagens para este usuário.", Toast.LENGTH_SHORT).show();
                return;
            }

            long timestamp = System.currentTimeMillis();
            ChatMessage chatMessage = new ChatMessage(messageText, currentUserId, receiverId, timestamp);

            // Gerar chaves
            String messageKey = messagesRef.push().getKey();

            // Mapa de Atualização Atômica (Melhoria de Performance e Consistência)
            Map<String, Object> updates = new HashMap<>();

            // 1. Mensagem em si (Coleção separada)
            updates.put("/messages/" + chatId + "/" + messageKey, chatMessage);

            // 2. Metadados do Chat (Para a lista de conversas carregar rápido)
            updates.put("/chats/" + chatId + "/ultimaMensagem", messageText);
            updates.put("/chats/" + chatId + "/timestamp", timestamp);
            updates.put("/chats/" + chatId + "/membros/" + currentUserId, true);
            updates.put("/chats/" + chatId + "/membros/" + receiverId, true);
            // Marca como lido por quem enviou a mensagem agora
            updates.put("/chats/" + chatId + "/lidoPor/" + currentUserId, timestamp);

            // 3. Índices de Conversa (Garante que apareça na lista)
            updates.put("/user_conversas/" + currentUserId + "/" + chatId, true);
            updates.put("/user_conversas/" + receiverId + "/" + chatId, true);

            FirebaseDatabase.getInstance().getReference().updateChildren(updates).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    editTextChatMessage.setText("");
                    enviarNotificacao();
                } else {
                    Toast.makeText(this, "Falha ao enviar mensagem.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void enviarNotificacao() {
        Log.d(TAG, "enviarNotificacao() chamado. receiverId=" + receiverId + ", currentUserId=" + currentUserId);

        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Usuario user = snapshot.getValue(Usuario.class);
                String nomeRemetente = (user != null) ? user.getNick() : "Alguém";
                Log.d(TAG, "Nome do remetente: " + nomeRemetente);

                String notifId = "chat_" + currentUserId;
                String mensagemNotificacao = "Nova mensagem de " + nomeRemetente;

                DatabaseReference notificacaoRef = FirebaseDatabase.getInstance().getReference("notificacoes")
                        .child(receiverId).child(notifId);

                // Primeiro APAGA o nó antigo, depois CRIA um novo
                // Isso garante que a Cloud Function detecte a criação como um evento novo
                notificacaoRef.removeValue().addOnCompleteListener(task -> {
                    Notificacao notificacao = new Notificacao(notifId, "chat",
                            mensagemNotificacao, currentUserId, System.currentTimeMillis());

                    Log.d(TAG, "Criando notificação nova após remoção. ID=" + notifId);
                    notificacaoRef.setValue(notificacao)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Notificação gravada no Firebase com sucesso!"))
                            .addOnFailureListener(e -> Log.e(TAG, "❌ ERRO ao gravar notificação: " + e.getMessage()));
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Erro ao buscar dados do remetente: " + error.getMessage());
            }
        });
    }

    private DatabaseReference receiverRef;
    private ValueEventListener headerListener;

    private void carregarDadosDoCabecalho() {
        receiverRef = FirebaseDatabase.getInstance().getReference("users").child(receiverId);
        headerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed())
                    return;
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
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isFinishing() && !isDestroyed()) {
                    textViewNomeChat.setText("Usuário desconhecido");
                }
            }
        };
        receiverRef.addValueEventListener(headerListener);
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(chatAdapter);
    }

    private ValueEventListener messagesListener;

    private void loadMessages() {
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinishing() || isDestroyed())
                    return;
                chatMessages.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatMessage message = dataSnapshot.getValue(ChatMessage.class);
                    if (message != null) {
                        chatMessages.add(message);

                        // Atualiza status de leitura localmente se necessário
                        if (!message.getSenderId().equals(currentUserId) && !"lido".equals(message.getStatus())) {
                            messagesRef.child(dataSnapshot.getKey()).child("status").setValue("lido");
                        }
                    }
                }
                chatAdapter.notifyDataSetChanged();
                recyclerViewChat.scrollToPosition(chatMessages.size() - 1);

                // Sempre que carregarem mensagens novas, atualizamos o status de leitura global
                atualizarStatusLidoNoChat();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        messagesRef.addValueEventListener(messagesListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
