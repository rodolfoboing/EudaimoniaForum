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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.button.MaterialButton;
import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.auth.LoginActivity;
import com.meuprojeto.eudaimoniaforum.chat.ChatActivity;
import com.meuprojeto.eudaimoniaforum.chat.ConversationActivity;
import com.meuprojeto.eudaimoniaforum.forum.CommentActivity;
import com.meuprojeto.eudaimoniaforum.forum.ForumActivity;
import com.meuprojeto.eudaimoniaforum.moderation.ModerationActivity;
import com.meuprojeto.eudaimoniaforum.notification.NotificationActivity;
import com.meuprojeto.eudaimoniaforum.notification.NotificationSetupHelper;
import com.meuprojeto.eudaimoniaforum.onboarding.OnboardingActivity;
import com.meuprojeto.eudaimoniaforum.orientation.OrientationActivity;
import com.meuprojeto.eudaimoniaforum.profile.ProfileActivity;
import com.meuprojeto.eudaimoniaforum.utils.DialogManager;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "PrefsAbstinencia";
    private static final String CONQUISTA_WORK_TAG = "ConquistasWork";

    private TextView textViewTempoAbstinenciaMeses, textViewTempoAbstinenciaDias, textViewTempoAbstinenciaHoras,
            textViewTempoAbstinenciaMinutos, textViewHabito, textViewDiasValidos;
    private ImageButton buttonNotificacao;
    private MaterialButton buttonModeracao;
    private MaterialButton buttonCheckInDiario;
    
    private AbstinenceTimerHelper timerHelper;
    private CheckInManager checkInManager;
    private MainManager mainManager;
    private ObjectAnimator animacaoSino;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("MainActivity", "onCreate() chamado. Inicializando MainActivity.");
        setContentView(R.layout.main_activity);

        mainManager = new MainManager(this);

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

        verificarModoOperacao();
        carregarDadosUsuario();
        verificarCheckInDiario();
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
                if (mainManager != null) {
                    mainManager.marcarNotificacaoComoLidaPorReferencia(tipo, idReferencia);
                }

                if (tipo.equals("chat")) {
                    Intent chatIntent = new Intent(this, ChatActivity.class);
                    chatIntent.putExtra("USER_ID", idReferencia);
                    startActivity(chatIntent);
                } else if (tipo.equals("comentario")) {
                    Intent comIntent = new Intent(this, CommentActivity.class);
                    comIntent.putExtra("POST_ID", idReferencia);
                    startActivity(comIntent);
                }
            }
        }
    }

    private void iniciarListenersDeNotificacao() {
        if (mainManager.getCurrentUserId() == null) return;
        
        animacaoSino = ObjectAnimator.ofFloat(buttonNotificacao, "rotation", 0f, 15f, -15f, 10f, -10f, 5f, -5f, 0f);
        animacaoSino.setDuration(1200);
        animacaoSino.setRepeatCount(ObjectAnimator.INFINITE);

        mainManager.monitorarNotificacoesNaoLidas(temNaoLida -> {
            if(isFinishing() || isDestroyed()) return;
            if (temNaoLida) {
                buttonNotificacao.setColorFilter(Color.parseColor("#FFD700"));
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
        });

        NotificationSetupHelper.setupNotifications(this);
        buttonNotificacao.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (timerHelper != null) {
            timerHelper.init();
            timerHelper.refresh();
        }
        carregarDadosUsuario();
        verificarCheckInDiario();
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
                atualizarBtnCheckinCompleto();
                if (textViewDiasValidos != null) {
                    textViewDiasValidos.setText("⭐ " + newStreak);
                }
            }

            @Override
            public void onNewAchievementUnlocked(String titulo, String mensagem) {
                DialogManager.exibirDialogConquista(MainActivity.this, titulo, mensagem, () -> {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
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

            @Override
            public void onError(String erro) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(MainActivity.this, "Erro no Check-in: " + erro, Toast.LENGTH_SHORT).show();
                buttonCheckInDiario.setEnabled(true);
                buttonCheckInDiario.setText("✅ Compromisso Diário");
            }
        });
    }

    private void verificarCheckInDiario() {
        checkInManager.checkCurrentStatus(new CheckInManager.CheckInStatusCallback() {
            @Override
            public void onCheckInStateLoaded(boolean isCompletedToday, int streak) {
                if (textViewDiasValidos != null) {
                    textViewDiasValidos.setText("⭐ " + streak);
                }
                if (isCompletedToday) {
                    atualizarBtnCheckinCompleto();
                } else {
                    buttonCheckInDiario.setText("✅ Compromisso Diário");
                    buttonCheckInDiario.setEnabled(true);
                    buttonCheckInDiario.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#673AB7")));
                }
            }

            @Override
            public void onError(String erro) {
                if (isFinishing() || isDestroyed()) return;
                Toast.makeText(MainActivity.this, "Erro ao carregar status: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void atualizarBtnCheckinCompleto() {
        buttonCheckInDiario.setText("✅ Compromisso Feito!");
        buttonCheckInDiario.setEnabled(false);
        buttonCheckInDiario.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
    }

    private void configurarBotoes() {
        findViewById(R.id.buttonNovoRegistro).setOnClickListener(v -> reiniciarContador());
        findViewById(R.id.buttonEditar).setOnClickListener(v -> startActivity(new Intent(this, EditAbstinenceActivity.class)));
        findViewById(R.id.linearLayoutForum).setOnClickListener(v -> startActivity(new Intent(this, ForumActivity.class)));
        findViewById(R.id.navConversas).setOnClickListener(v -> startActivity(new Intent(this, ConversationActivity.class)));
        findViewById(R.id.navMenu).setOnClickListener(this::showPopupMenu);
        buttonModeracao.setOnClickListener(v -> startActivity(new Intent(this, ModerationActivity.class)));
        buttonCheckInDiario.setOnClickListener(v -> realizarCheckIn());
    }

    private void showPopupMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.main_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_perfil) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            } else if (itemId == R.id.menu_orientacoes) {
                startActivity(new Intent(this, OrientationActivity.class));
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
        mainManager.deslogar();
        WorkManager.getInstance(this).cancelUniqueWork(CONQUISTA_WORK_TAG);
        Toast.makeText(this, "Usuário deslogado com sucesso!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void agendarTrabalhoDeConquistas() {
        PeriodicWorkRequest conquistasWorkRequest = new PeriodicWorkRequest.Builder(AchievementWorker.class, 1, TimeUnit.DAYS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(CONQUISTA_WORK_TAG, ExistingPeriodicWorkPolicy.KEEP, conquistasWorkRequest);
    }

    private void reiniciarContador() {
        if (timerHelper != null) {
            timerHelper.reset();
        }
        if (textViewDiasValidos != null) {
            textViewDiasValidos.setText("⭐ 0");
        }
        
        mainManager.zerarContadorRastreamento(new MainManager.AcaoCallback() {
            @Override
            public void onSuccess() {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(MainActivity.this, getString(R.string.contador_reiniciado_aviso), Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, getString(R.string.contador_reiniciado_motivacao), Toast.LENGTH_LONG).show();
                verificarCheckInDiario();
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(MainActivity.this, "Falha ao zerar: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void carregarDadosUsuario() {
        mainManager.carregarVicioDoUsuario(new MainManager.DadosUsuarioCallback() {
            @Override
            public void onVicioCarregado(String vicio) {
                if(isFinishing() || isDestroyed()) return;
                textViewHabito.setText(vicio.isEmpty() ? "Hábito não definido" : vicio);
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                textViewHabito.setText("Erro ao carregar");
            }
        });
    }

    private void verificarModoOperacao() {
        mainManager.verificarModerador(isModerador -> {
            if(isFinishing() || isDestroyed()) return;
            buttonModeracao.setVisibility(isModerador ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mainManager != null) {
            mainManager.removerListeners();
        }
        if (timerHelper != null) {
            timerHelper.stop();
        }
    }
}
