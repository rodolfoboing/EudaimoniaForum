package com.meuprojeto.eudaimoniaforum;

import android.animation.ObjectAnimator;
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

    private TextView textViewTempoAbstinenciaMeses, textViewTempoAbstinenciaDias, textViewTempoAbstinenciaHoras,
            textViewTempoAbstinenciaMinutos, textViewHabito;
    private ImageButton buttonNotificacao;
    private MaterialButton buttonModeracao;
    private MaterialButton buttonCheckInDiario;
    private long tempoInicial;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnableAtualizarTempo;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;
    private Map<DatabaseReference, ChildEventListener> activeChildListeners = new HashMap<>();
    private Map<DatabaseReference, ValueEventListener> activeValueListeners = new HashMap<>();
    private ObjectAnimator animacaoSino;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("MainActivity", "onCreate() chamado. Inicializando MainActivity.");
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        }

        // Verifica se é a primeira vez (Onboarding)
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean("onboarding_complete", false)) {
            startActivity(new Intent(this, OnboardingActivity.class));
        }

        inicializarUI();

        configurarBotoes();

        recuperarTempoInicial();

        iniciarAtualizacaoTempo();

        verificarModerador();

        carregarDadosUsuario();

        verificarCheckInDiario(); // Verifica se já fez check-in hoje

        agendarTrabalhoDeMetas();

        iniciarListenersDeNotificacao();

        verificarIntentDeNotificacao(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        verificarIntentDeNotificacao(intent);
    }

    private void verificarIntentDeNotificacao(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            String tipo = intent.getStringExtra("tipo");
            String idReferencia = intent.getStringExtra("idReferencia");

            if (tipo != null && idReferencia != null) {
                if (tipo.equals("chat")) {
                    Intent chatIntent = new Intent(this, ChatActivity.class);
                    chatIntent.putExtra("USER_ID", idReferencia);
                    startActivity(chatIntent);
                } else if (tipo.equals("comentario")) {
                    Intent comIntent = new Intent(this, ComentarioActivity.class);
                    comIntent.putExtra("POST_ID", idReferencia);
                    startActivity(comIntent);
                }
            }
        }
    }

    private void iniciarListenersDeNotificacao() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null)
            return;
        String userId = currentUser.getUid();

        monitorarStatusDoLeitor(userId);

        // Criar Canal de Notificação para versões Oreo ou superior
        criarCanalDeNotificacao();

        // Solicitar permissão de notificação (Android 13+)
        verificarPermissaoNotificacao();

        // Atualizar Token FCM
        atualizarFcmToken();
    }

    private void criarCanalDeNotificacao() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    "fcm_default_channel",
                    "Notificações do Fórum",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Canal principal para mensagens e alertas do Eudaimonia Fórum");
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void verificarPermissaoNotificacao() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this,
                        new String[] { android.Manifest.permission.POST_NOTIFICATIONS }, 101);
            }
        }
    }

    private void atualizarFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        android.util.Log.e("MainActivity", "❌ Falha ao obter token FCM", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    android.util.Log.d("MainActivity",
                            "Token FCM obtido: " + (token != null ? token.substring(0, 20) + "..." : "null"));
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null && token != null) {
                        android.util.Log.d("MainActivity", "Salvando token FCM para userId: " + user.getUid());
                        FirebaseDatabase.getInstance().getReference("users")
                                .child(user.getUid())
                                .child("fcmToken")
                                .setValue(token)
                                .addOnSuccessListener(aVoid -> android.util.Log.d("MainActivity",
                                        "✅ Token FCM salvo com sucesso no banco!"))
                                .addOnFailureListener(e -> android.util.Log.e("MainActivity",
                                        "❌ ERRO ao salvar token: " + e.getMessage()));
                    } else {
                        android.util.Log.w("MainActivity",
                                "⚠ user=" + user + ", token=" + token + " — não foi possível salvar");
                    }
                });
    }

    private void monitorarStatusDoLeitor(String userId) {
        if (animacaoSino == null) {
            animacaoSino = ObjectAnimator.ofFloat(buttonNotificacao, "rotation", 0f, 15f, -15f, 10f, -10f, 5f, -5f, 0f);
            animacaoSino.setDuration(1200); // 1.2 segundos por ciclo
            animacaoSino.setRepeatCount(ObjectAnimator.INFINITE);
        }

        DatabaseReference notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes").child(userId);
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean temNaoLida = false;
                for (DataSnapshot doc : snapshot.getChildren()) {
                    Notificacao notif = doc.getValue(Notificacao.class);
                    if (notif != null && !notif.isLida()) {
                        temNaoLida = true;
                        break;
                    }
                }

                if (temNaoLida) {
                    buttonNotificacao.setColorFilter(Color.parseColor("#FFD700")); // Dourado mais bonito
                    if (!animacaoSino.isRunning()) {
                        animacaoSino.start();
                    }
                } else {
                    buttonNotificacao.setColorFilter(Color.WHITE);
                    if (animacaoSino.isRunning()) {
                        animacaoSino.cancel();
                        buttonNotificacao.setRotation(0f);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        notificacoesRef.addValueEventListener(listener);
        activeValueListeners.put(notificacoesRef, listener);

        buttonNotificacao.setOnClickListener(v -> startActivity(new Intent(this, NotificacaoActivity.class)));
    }

    private void removerTodosOsListeners() {
        for (Map.Entry<DatabaseReference, ChildEventListener> entry : activeChildListeners.entrySet()) {
            entry.getKey().removeEventListener(entry.getValue());
        }
        activeChildListeners.clear();

        for (Map.Entry<DatabaseReference, ValueEventListener> entry : activeValueListeners.entrySet()) {
            entry.getKey().removeEventListener(entry.getValue());
        }
        activeValueListeners.clear();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        textViewTempoAbstinenciaMinutos = findViewById(R.id.textViewTempoAbstinenciaMinutos);
        textViewHabito = findViewById(R.id.textViewHabito);
        buttonNotificacao = findViewById(R.id.buttonNotificacao);
        buttonModeracao = findViewById(R.id.buttonModeracao);
        buttonCheckInDiario = findViewById(R.id.buttonCheckInDiario);
    }

    private void realizarCheckIn() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null)
            return;

        String hoje = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        DatabaseReference checkInRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid())
                .child("checkins");

        checkInRef.child(hoje).setValue(true).addOnSuccessListener(aVoid -> {
            buttonCheckInDiario.setText("✅ Compromisso Feito!");
            buttonCheckInDiario.setEnabled(false);
            buttonCheckInDiario.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));

            // Incrementa Streak
            incrementarStreak(user.getUid());

            // Verifica e concede medalhas baseadas no tempo de abstinência atual
            verificarEConcederMedalhas(user.getUid());

            new AlertDialog.Builder(this)
                    .setTitle("Compromisso Firmado!")
                    .setMessage(
                            "Parabéns por renovar seu compromisso de sobriedade por mais um dia. Um dia de cada vez!")
                    .setPositiveButton("Vamos lá!", null)
                    .show();
        });
    }

    private void verificarEConcederMedalhas(String userId) {
        long diferenca = System.currentTimeMillis() - tempoInicial;
        long dias = TimeUnit.MILLISECONDS.toDays(diferenca);

        DatabaseReference conquistasRef = FirebaseDatabase.getInstance().getReference("users")
                .child(userId).child("conquistas");

        Map<String, Object> updates = new HashMap<>();

        // Milestones: 1, 3, 7, 30, 90, 180, 365
        if (dias >= 1)
            updates.put("badge_1_dia", true);
        if (dias >= 3)
            updates.put("badge_3_dias", true);
        if (dias >= 7)
            updates.put("badge_1_semana", true);
        if (dias >= 30)
            updates.put("badge_1_mes", true);
        if (dias >= 90)
            updates.put("badge_3_meses", true);
        if (dias >= 180)
            updates.put("badge_6_meses", true);
        if (dias >= 365)
            updates.put("badge_1_ano", true);

        if (!updates.isEmpty()) {
            conquistasRef.updateChildren(updates);
        }
    }

    private void incrementarStreak(String userId) {
        DatabaseReference streakRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                .child("streakAtual");
        streakRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(
                    @NonNull com.google.firebase.database.MutableData currentData) {
                Integer currentStreak = currentData.getValue(Integer.class);
                if (currentStreak == null) {
                    currentData.setValue(1);
                } else {
                    currentData.setValue(currentStreak + 1);
                }
                return com.google.firebase.database.Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                    @Nullable DataSnapshot currentData) {
            }
        });
    }

    private void verificarCheckInDiario() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null)
            return;

        String hoje = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        DatabaseReference checkInRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid())
                .child("checkins").child(hoje);

        checkInRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    buttonCheckInDiario.setText("✅ Compromisso Feito!");
                    buttonCheckInDiario.setEnabled(false);
                    buttonCheckInDiario.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
                } else {
                    buttonCheckInDiario.setText("✅ Compromisso Diário");
                    buttonCheckInDiario.setEnabled(true);
                    buttonCheckInDiario.setBackgroundTintList(
                            android.content.res.ColorStateList.valueOf(Color.parseColor("#673AB7"))); // Roxo
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void configurarBotoes() {
        findViewById(R.id.buttonNovoRegistro).setOnClickListener(v -> reiniciarContador());
        findViewById(R.id.buttonEditar)
                .setOnClickListener(v -> startActivity(new Intent(this, EditarAbstinenciaActivity.class)));
        findViewById(R.id.linearLayoutForum)
                .setOnClickListener(v -> startActivity(new Intent(this, ForumActivity.class)));
        findViewById(R.id.navConversas)
                .setOnClickListener(v -> startActivity(new Intent(this, ConversasActivity.class)));
        findViewById(R.id.navMenu).setOnClickListener(this::showPopupMenu);
        buttonModeracao.setOnClickListener(v -> startActivity(new Intent(this, ModeracaoActivity.class)));
        buttonCheckInDiario.setOnClickListener(v -> realizarCheckIn());
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_perfil) {
                startActivity(new Intent(this, PerfilActivity.class));
                return true;
            } else if (itemId == R.id.menu_orientacoes) {
                startActivity(new Intent(this, OrientacoesActivity.class));
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
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Contato & Feedback");

        // Cria um layout personalizado para o diálogo
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Texto de ajuda
        android.widget.TextView message = new android.widget.TextView(this);
        message.setText("Para dúvidas, sugestões ou suporte, envie um e-mail para:\n\nrodolfo.bm.reserva@gmail.com");
        message.setTextSize(16);
        message.setTextColor(android.graphics.Color.BLACK);
        message.setAutoLinkMask(android.text.util.Linkify.EMAIL_ADDRESSES); // Torna o e-mail clicável
        message.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        layout.addView(message);

        // Botão para Política de Privacidade
        android.widget.Button btnPrivacy = new android.widget.Button(this);
        btnPrivacy.setText("Política de Privacidade");
        btnPrivacy.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0E0E0")));
        btnPrivacy.setTextColor(android.graphics.Color.BLACK);
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 40, 0, 0);
        btnPrivacy.setLayoutParams(params);

        btnPrivacy.setOnClickListener(v -> {
            android.content.Intent browserIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://gist.github.com/rodolfoboing/c68da4a7504b78036166b44b11e8c7ee"));
            startActivity(browserIntent);
        });

        layout.addView(btnPrivacy);
        builder.setView(layout);
        builder.setPositiveButton("Fechar", null);
        builder.show();
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
        PeriodicWorkRequest metasWorkRequest = new PeriodicWorkRequest.Builder(MetasWorker.class, 1, TimeUnit.DAYS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(META_WORK_TAG, ExistingPeriodicWorkPolicy.KEEP,
                metasWorkRequest);
    }

    private void recuperarTempoInicial() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!preferences.contains(KEY_TEMPO_INICIAL)) {
            tempoInicial = System.currentTimeMillis();
            preferences.edit().putLong(KEY_TEMPO_INICIAL, tempoInicial).apply();
        } else {
            tempoInicial = preferences.getLong(KEY_TEMPO_INICIAL, System.currentTimeMillis());
        }
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
        long segundos = TimeUnit.MILLISECONDS.toSeconds(diferenca) % 60;

        textViewTempoAbstinenciaMeses.setText(String.valueOf(meses));
        textViewTempoAbstinenciaDias.setText(String.valueOf(dias));
        textViewTempoAbstinenciaHoras.setText(String.valueOf(horas));
        textViewTempoAbstinenciaMinutos.setText(String.valueOf(minutos));
    }

    private void carregarDadosUsuario() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null)
            return;

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());

        userRef.child("vicio").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String vicio = snapshot.getValue(String.class);
                if (vicio != null && !vicio.isEmpty()) {
                    textViewHabito.setText(vicio);
                } else {
                    textViewHabito.setText("Hábito não definido");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                textViewHabito.setText("Erro ao carregar");
            }
        });
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
                public void onCancelled(@NonNull DatabaseError error) {
                }
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
