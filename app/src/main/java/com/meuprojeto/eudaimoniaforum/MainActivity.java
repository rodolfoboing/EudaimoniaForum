package com.meuprojeto.eudaimoniaforum;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "PrefsAbstinencia";
    private static final String KEY_TEMPO_INICIAL = "tempo_inicial";
    private static final String META_WORK_TAG = "MetasWork";

    private TextView textViewTempoAbstinenciaMeses, textViewTempoAbstinenciaDias, textViewTempoAbstinenciaHoras, textViewTempoAbstinenciaSegundos, textViewHabito;
    private ImageButton buttonNotificacao;
    private MaterialButton buttonModeracao;
    private long tempoInicial;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnableAtualizarTempo;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;
    private Map<DatabaseReference, ChildEventListener> activeListeners = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        }

        inicializarUI();
        configurarBotoes();
        recuperarTempoInicial();
        iniciarAtualizacaoTempo();
        verificarModerador();
        carregarDadosUsuario();
        agendarTrabalhoDeMetas();
        iniciarListenersDeNotificacao();
    }

    private void iniciarListenersDeNotificacao() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        monitorarStatusDoLeitor(userId);
        ouvirNovosComentarios(userId);
        ouvirNovasMensagensDeChat(userId);
    }

    private void monitorarStatusDoLeitor(String userId) {
        DatabaseReference notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes").child(userId);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                buttonNotificacao.setColorFilter(snapshot.hasChildren() ? Color.YELLOW : Color.WHITE);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        };
        notificacoesRef.addValueEventListener(listener);
        buttonNotificacao.setOnClickListener(v -> startActivity(new Intent(this, NotificacaoActivity.class)));
    }

    private void ouvirNovosComentarios(String userId) {
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("forum/posts");
        Query userPostsQuery = postsRef.orderByChild("autor").equalTo(userId);

        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot postSnapshot, @Nullable String previousChildName) {
                String postId = postSnapshot.getKey();
                if (postId == null) return;

                DatabaseReference commentsRef = postSnapshot.child("comentarios").getRef();
                commentsRef.orderByChild("timestamp").startAt(System.currentTimeMillis()).addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot commentSnapshot, @Nullable String previousChildName) {
                        Comentario comentario = commentSnapshot.getValue(Comentario.class);
                        if (comentario != null && !comentario.getAutor().equals(userId)) {
                            criarNotificacaoDeComentario(comentario, postId);
                        }
                    }
                    @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
                    @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
                    @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        userPostsQuery.addChildEventListener(listener);
        activeListeners.put(userPostsQuery.getRef(), listener);
    }

    private void ouvirNovasMensagensDeChat(String userId) {
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot chatSnapshot, @Nullable String previousChildName) {
                String chatId = chatSnapshot.getKey();
                if (chatId != null && chatId.contains(userId)) {
                    DatabaseReference messagesRef = chatSnapshot.child("messages").getRef();
                    messagesRef.orderByChild("timestamp").startAt(System.currentTimeMillis()).addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(@NonNull DataSnapshot messageSnapshot, @Nullable String previousChildName) {
                            ChatMessage message = messageSnapshot.getValue(ChatMessage.class);
                            if (message != null && !message.getSenderId().equals(userId)) {
                                criarNotificacaoDeChat(message);
                            }
                        }
                        @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
                        @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
                        @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                    });
                }
            }
            @Override public void onChildChanged(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot s) {}
            @Override public void onChildMoved(@NonNull DataSnapshot s, @Nullable String p) {}
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        };
        chatsRef.addChildEventListener(listener);
        activeListeners.put(chatsRef, listener);
    }

    private void criarNotificacaoDeComentario(Comentario comentario, String postId) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(comentario.getAutor());
        usersRef.child("nick").get().addOnSuccessListener(nickSnapshot -> {
            String nick = nickSnapshot.exists() ? nickSnapshot.getValue(String.class) : "Alguém";
            String mensagem = nick + " comentou no seu post.";
            DatabaseReference notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes").child(firebaseAuth.getCurrentUser().getUid());
            String notificacaoId = notificacoesRef.push().getKey();
            if (notificacaoId != null) {
                Notificacao notificacao = new Notificacao(notificacaoId, "comentario", mensagem, postId, comentario.getTimestamp());
                notificacoesRef.child(notificacaoId).setValue(notificacao);
            }
        });
    }

    private void criarNotificacaoDeChat(ChatMessage message) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users").child(message.getSenderId());
        usersRef.child("nick").get().addOnSuccessListener(nickSnapshot -> {
            String nick = nickSnapshot.exists() ? nickSnapshot.getValue(String.class) : "Alguém";
            String mensagem = "Nova mensagem de " + nick;
            DatabaseReference notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes").child(firebaseAuth.getCurrentUser().getUid());
            String notificacaoId = notificacoesRef.push().getKey();
            if (notificacaoId != null) {
                Notificacao notificacao = new Notificacao(notificacaoId, "chat", mensagem, message.getSenderId(), message.getTimestamp());
                notificacoesRef.child(notificacaoId).setValue(notificacao);
            }
        });
    }

    private void removerTodosOsListeners() {
        for (Map.Entry<DatabaseReference, ChildEventListener> entry : activeListeners.entrySet()) {
            entry.getKey().removeEventListener(entry.getValue());
        }
        activeListeners.clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Não precisamos mais salvar o timestamp aqui
    }

    @Override
    protected void onResume() {
        super.onResume();
        recuperarTempoInicial();
        carregarDadosUsuario();
    }

    private void inicializarUI() {
        textViewTempoAbstinenciaMeses = findViewById(R.id.textViewTempoAbstinenciaMeses);
        textViewTempoAbstinenciaDias = findViewById(R.id.textViewTempoAbstinenciaDias);
        textViewTempoAbstinenciaHoras = findViewById(R.id.textViewTempoAbstinenciaHoras);
        textViewTempoAbstinenciaSegundos = findViewById(R.id.textViewTempoAbstinenciaSegundos);
        textViewHabito = findViewById(R.id.textViewHabito);
        buttonNotificacao = findViewById(R.id.buttonNotificacao);
        buttonModeracao = findViewById(R.id.buttonModeracao);
    }

    private void carregarDadosUsuario() {
        if (userRef != null) {
            userRef.child("vicio").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    textViewHabito.setText(snapshot.exists() ? snapshot.getValue(String.class) : "Vício não definido");
                }
                @Override public void onCancelled(@NonNull DatabaseError error) { }
            });
        }
    }

    private void configurarBotoes() {
        findViewById(R.id.buttonNovoRegistro).setOnClickListener(v -> reiniciarContador());
        findViewById(R.id.buttonEditar).setOnClickListener(v -> startActivity(new Intent(this, EditarAbstinenciaActivity.class)));
        findViewById(R.id.linearLayoutForum).setOnClickListener(v -> startActivity(new Intent(this, ForumActivity.class)));
        findViewById(R.id.navConversas).setOnClickListener(v -> startActivity(new Intent(this, ConversasActivity.class)));
        findViewById(R.id.navMenu).setOnClickListener(this::showPopupMenu);
        buttonModeracao.setOnClickListener(v -> startActivity(new Intent(this, ModeracaoActivity.class)));
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                return true;
            } else if (itemId == R.id.menu_contato) {
                mostrarDialogoContato();
                return true;
            } else if (itemId == R.id.menu_deslogar) {
                deslogar();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void mostrarDialogoContato() {
        new AlertDialog.Builder(this).setTitle("Contato e Feedback").setMessage("Para feedback, sugestões ou denúncias, entre em contato: Rodolfo.bm.reserva@outlook.com").setPositiveButton("OK", null).show();
    }

    private void deslogar() {
        firebaseAuth.signOut();
        removerTodosOsListeners();
        WorkManager.getInstance(this).cancelUniqueWork(META_WORK_TAG);
        Toast.makeText(this, "Usuário deslogado com sucesso!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void agendarTrabalhoDeMetas() {
        PeriodicWorkRequest metasWorkRequest = new PeriodicWorkRequest.Builder(MetasWorker.class, 1, TimeUnit.DAYS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(META_WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, metasWorkRequest);
    }

    private void recuperarTempoInicial() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        tempoInicial = preferences.getLong(KEY_TEMPO_INICIAL, System.currentTimeMillis());
    }

    private void iniciarAtualizacaoTempo() {
        runnableAtualizarTempo = () -> {
            atualizarTempoAbstinencia();
            handler.postDelayed(runnableAtualizarTempo, 1000);
        };
        handler.post(runnableAtualizarTempo);
    }

    private void reiniciarContador() {
        tempoInicial = System.currentTimeMillis();
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit().putLong(KEY_TEMPO_INICIAL, tempoInicial).apply();
        atualizarTempoAbstinencia();
        Toast.makeText(this, "Contador reiniciado!", Toast.LENGTH_SHORT).show();
    }

    private void atualizarTempoAbstinencia() {
        long diferenca = System.currentTimeMillis() - tempoInicial;
        long meses = TimeUnit.MILLISECONDS.toDays(diferenca) / 30;
        long dias = TimeUnit.MILLISECONDS.toDays(diferenca) % 30;
        long horas = TimeUnit.MILLISECONDS.toHours(diferenca) % 24;
        long minutos = TimeUnit.MILLISECONDS.toMinutes(diferenca) % 60;
        textViewTempoAbstinenciaMeses.setText(String.valueOf(meses));
        textViewTempoAbstinenciaDias.setText(String.valueOf(dias));
        textViewTempoAbstinenciaHoras.setText(String.valueOf(horas));
        textViewTempoAbstinenciaSegundos.setText(String.valueOf(minutos));
    }

    private void verificarModerador() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference modRef = FirebaseDatabase.getInstance().getReference("moderadores").child(user.getUid());
            modRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                        buttonModeracao.setVisibility(View.VISIBLE);
                    } else {
                        buttonModeracao.setVisibility(View.GONE);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        } else {
            buttonModeracao.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removerTodosOsListeners();
        if (handler != null && runnableAtualizarTempo != null) {
            handler.removeCallbacks(runnableAtualizarTempo);
        }
    }
}
