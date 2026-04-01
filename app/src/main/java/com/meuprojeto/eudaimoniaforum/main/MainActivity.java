package com.meuprojeto.eudaimoniaforum.main;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.chat.ConversasActivity;
import com.meuprojeto.eudaimoniaforum.utils.DialogManager;
import com.meuprojeto.eudaimoniaforum.orientacoes.OrientacoesActivity;
import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.auth.LoginActivity;
import com.meuprojeto.eudaimoniaforum.chat.ChatActivity;
import com.meuprojeto.eudaimoniaforum.forum.ComentarioActivity;
import com.meuprojeto.eudaimoniaforum.forum.ForumActivity;
import com.meuprojeto.eudaimoniaforum.moderacao.ModeracaoActivity;
import com.meuprojeto.eudaimoniaforum.notification.Notificacao;
import com.meuprojeto.eudaimoniaforum.notification.NotificacaoActivity;
import com.meuprojeto.eudaimoniaforum.notification.NotificationSetupHelper;
import com.meuprojeto.eudaimoniaforum.onboarding.OnboardingActivity;
import com.meuprojeto.eudaimoniaforum.perfil.PerfilActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "PrefsAbstinencia";
    private static final String KEY_TEMPO_INICIAL = "tempo_inicial";
    private static final String CONQUISTA_WORK_TAG = "ConquistasWork";

    private TextView textViewTempoAbstinenciaMeses, textViewTempoAbstinenciaDias, textViewTempoAbstinenciaHoras,
            textViewTempoAbstinenciaMinutos, textViewHabito, textViewDiasValidos;
    private ImageButton buttonNotificacao;
    private MaterialButton buttonModeracao;
    private MaterialButton buttonCheckInDiario;
    private AbstinenceTimerHelper timerHelper;
    private CheckInManager checkInManager;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;
    private Map<DatabaseReference, ChildEventListener> activeChildListeners = new HashMap<>();
    private Map<DatabaseReference, ValueEventListener> activeValueListeners = new HashMap<>();
    private ObjectAnimator animacaoSino;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("MainActivity", "onCreate() chamado. Inicializando MainActivity.");
        setContentView(R.layout.main_activity);

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

        timerHelper = new AbstinenceTimerHelper(this, (meses, dias, horas, minutos) -> {
            textViewTempoAbstinenciaMeses.setText(String.valueOf(meses));
            textViewTempoAbstinenciaDias.setText(String.valueOf(dias));
            textViewTempoAbstinenciaHoras.setText(String.valueOf(horas));
            textViewTempoAbstinenciaMinutos.setText(String.valueOf(minutos));
        });
        timerHelper.init();
        timerHelper.start();

        checkInManager = new CheckInManager(this);

        verificarModerador();

        carregarDadosUsuario();

        verificarCheckInDiario(); // Verifica se já fez check-in hoje

        agendarTrabalhoDeConquistas();

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

        NotificationSetupHelper.setupNotifications(this);
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
        if (timerHelper != null) {
            timerHelper.init();
            timerHelper.refresh();
            android.util.Log.d("MainActivity", "onResume: tempoInicial=" + timerHelper.getTempoInicial() + " (diff=" + (System.currentTimeMillis() - timerHelper.getTempoInicial()) + "ms)");
        }
        carregarDadosUsuario();
        verificarCheckInDiario(); // Recalcula streak e estado do botão de check-in
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
        textViewDiasValidos = findViewById(R.id.textViewDiasValidos);
    }

    private void realizarCheckIn() {
        checkInManager.performCheckIn(new CheckInManager.CheckInActionCallback() {
            @Override
            public void onCheckInSuccess(int newStreak) {
                buttonCheckInDiario.setText("✅ Compromisso Feito!");
                buttonCheckInDiario.setEnabled(false);
                buttonCheckInDiario.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
                if (textViewDiasValidos != null) {
                    textViewDiasValidos.setText("⭐ " + newStreak);
                }
            }

            @Override
            public void onNewAchievementUnlocked(String titulo, String mensagem) {
                DialogManager.exibirDialogConquista(MainActivity.this, titulo, mensagem, () -> {
                    startActivity(new Intent(MainActivity.this, PerfilActivity.class));
                });
            }

            @Override
            public void onRegularCheckInCompleted() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Compromisso Firmado!")
                        .setMessage("Parabéns por renovar seu compromisso de sobriedade de hoje. Você está no caminho certo!")
                        .setPositiveButton("Vamos lá!", null)
                        .show();
            }
        });
    }



    private void verificarCheckInDiario() {
        checkInManager.checkCurrentStatus((isCompletedToday, streak) -> {
            if (textViewDiasValidos != null) {
                textViewDiasValidos.setText("⭐ " + streak);
            }
            if (isCompletedToday) {
                buttonCheckInDiario.setText("✅ Compromisso Feito!");
                buttonCheckInDiario.setEnabled(false);
                buttonCheckInDiario.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
            } else {
                buttonCheckInDiario.setText("✅ Compromisso Diário");
                buttonCheckInDiario.setEnabled(true);
                buttonCheckInDiario.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(Color.parseColor("#673AB7"))); // Roxo
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
                DialogManager.mostrarDialogoContato(this);
                return true;
            } else if (itemId == R.id.menu_deslogar) {
                deslogar();
                return true;
            }
            return false;
        });
        popup.show();
    }



    private void deslogar() {
        firebaseAuth.signOut();
        removerTodosOsListeners();
        WorkManager.getInstance(this).cancelUniqueWork(CONQUISTA_WORK_TAG);
        Toast.makeText(this, "Usuário deslogado com sucesso!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void agendarTrabalhoDeConquistas() {
        PeriodicWorkRequest conquistasWorkRequest = new PeriodicWorkRequest.Builder(ConquistasWorker.class, 1,
                TimeUnit.DAYS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(CONQUISTA_WORK_TAG, ExistingPeriodicWorkPolicy.KEEP,
                conquistasWorkRequest);
    }



    private void reiniciarContador() {
        android.util.Log.d("MainActivity", "reiniciarContador: Usuário zerou o contador de abstinência.");
        if (timerHelper != null) {
            timerHelper.reset();
        }
        
        if (textViewDiasValidos != null) {
            textViewDiasValidos.setText("⭐ 0");
        }
        
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid());
            
            // Reset atômico: zera streak, limpa check-ins do ciclo anterior e ultimoCheckIn
            Map<String, Object> resetUpdates = new HashMap<>();
            resetUpdates.put("streakAtual", 0);
            resetUpdates.put("ultimoCheckIn", null);
            resetUpdates.put("checkins", null);
            userRef.updateChildren(resetUpdates);
        }

        Toast.makeText(this, "Contador e compromissos reiniciados!", Toast.LENGTH_SHORT).show();
        verificarCheckInDiario();
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
        if (timerHelper != null) {
            timerHelper.stop();
        }
    }
}
