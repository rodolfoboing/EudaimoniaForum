package com.meuprojeto.eudaimoniaforum.chat;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.meuprojeto.eudaimoniaforum.R;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements ChatManager.ChatUpdateListener {

    private static final String TAG = "ChatActivity";

    private RecyclerView recyclerViewChat;
    private EditText editTextChatMessage;
    private ImageButton buttonSendMessage, buttonOpcoes;
    private TextView textViewNomeChat, textViewStatusOnline;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;

    public static String activeChatUserId = null;

    private String receiverId, currentUserId;
    private ChatManager chatManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d(TAG, "onCreate() called. Inicializando ChatActivity.");
        setContentView(R.layout.chat_activity);

        receiverId = getIntent().getStringExtra("USER_ID");

        inicializarUI();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || receiverId == null) {
            Toast.makeText(this, "Erro: Usuário não identificado.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        chatManager = new ChatManager(currentUserId, receiverId);

        setupRecyclerView();
        
        chatManager.carregarDadosDoCabecalho(this);
        chatManager.loadMessages(this);
        chatManager.atualizarStatusLidoNoChat();

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

    @Override
    protected void onResume() {
        super.onResume();
        activeChatUserId = receiverId;
        ChatNotificationHelper.limparNotificacaoDeChat(currentUserId, receiverId);
        if (chatManager != null) {
            chatManager.atualizarStatusOnline();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        activeChatUserId = null;
    }

    private void showChatMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.chat_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_apagar_conversa) {
                confirmOp("Apagar Conversation", "Tem certeza que deseja apagar esta conversa? A ação não pode ser desfeita.",
                        () -> chatManager.apagarConversa(this));
                return true;
            } else if (itemId == R.id.menu_bloquear_usuario) {
                confirmOp("Bloquear Usuário", "Tem certeza que deseja bloquear este usuário? Você não receberá mais mensagens dele.",
                        () -> chatManager.bloquearUsuario(this));
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmOp(String title, String message, Runnable onConfirm) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Sim", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Não", null)
                .show();
    }

    private void sendMessage() {
        String messageText = editTextChatMessage.getText().toString().trim();
        chatManager.sendMessage(messageText, this, () -> {
            editTextChatMessage.setText("");
            ChatNotificationHelper.enviarNotificacao(currentUserId, receiverId);
        });
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);
        recyclerViewChat.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewChat.setAdapter(chatAdapter);
    }

    @Override
    public void onMessageAdded(ChatMessage message) {
        if(isFinishing() || isDestroyed()) return;
        chatMessages.add(message);
        int newPosition = chatMessages.size() - 1;
        chatAdapter.notifyItemInserted(newPosition);
        recyclerViewChat.scrollToPosition(newPosition);
    }

    @Override
    public void onMessageChanged(ChatMessage message) {
        if(isFinishing() || isDestroyed()) return;
        for (int i = 0; i < chatMessages.size(); i++) {
            if (chatMessages.get(i).getId() != null && chatMessages.get(i).getId().equals(message.getId())) {
                chatMessages.set(i, message);
                chatAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    @Override
    public void onMessageRemoved(String messageId) {
        if(isFinishing() || isDestroyed()) return;
        for (int i = 0; i < chatMessages.size(); i++) {
            if (messageId.equals(chatMessages.get(i).getId())) {
                chatMessages.remove(i);
                chatAdapter.notifyItemRemoved(i);
                break;
            }
        }
    }

    @Override
    public void onHeaderUpdated(String nome, String statusText, int statusColor) {
        if(isFinishing() || isDestroyed()) return;
        textViewNomeChat.setText(nome);
        textViewStatusOnline.setText(statusText);
        textViewStatusOnline.setTextColor(statusColor);
    }

    @Override
    public void onActionSuccess(String message) {
        if(isFinishing() || isDestroyed()) return;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        if ("Conversation apagada.".equals(message)) {
            finish();
        }
    }

    @Override
    public void onActionFailure(String error) {
        if(isFinishing() || isDestroyed()) return;
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatManager != null) {
            chatManager.removeListeners();
        }
    }
}
