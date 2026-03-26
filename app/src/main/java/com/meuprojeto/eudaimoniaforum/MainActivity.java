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
    private static final String CONQUISTA_WORK_TAG = "ConquistasWork";

    private TextView textViewTempoAbstinenciaMeses, textViewTempoAbstinenciaDias, textViewTempoAbstinenciaHoras,
            textViewTempoAbstinenciaMinutos, textViewHabito, textViewDiasValidos;
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
        atualizarTempoAbstinencia(); // Força atualização imediata do cronômetro
        carregarDadosUsuario();
        verificarCheckInDiario(); // Recalcula streak e estado do botão de check-in
        android.util.Log.d("MainActivity", "onResume: tempoInicial=" + tempoInicial + " (diff=" + (System.currentTimeMillis() - tempoInicial) + "ms)");
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
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null)
            return;

        String hoje = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer currentStreak = snapshot.child("streakAtual").getValue(Integer.class);
                if (currentStreak == null) currentStreak = 0;

                android.util.Log.d("MainActivity", "realizarCheckIn: lido streakAtual=" + currentStreak);

                // VALIDAÇÃO CRÍTICA: o streak nunca pode ultrapassar os dias reais do cronômetro
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                long tempoInicialSalvo = prefs.getLong(KEY_TEMPO_INICIAL, System.currentTimeMillis());
                int diasReaisAbstinencia = (int) TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - tempoInicialSalvo);

                // Se o streak atual já está inconsistente (maior que dias reais), corrige primeiro
                if (currentStreak > diasReaisAbstinencia) {
                    currentStreak = diasReaisAbstinencia;
                }

                int novoStreak = currentStreak + 1;

                // Teto máximo: o streak com o check-in de hoje não pode exceder (diasReais + 1)
                // porque o dia de hoje pode ainda não ter completado 24h no cronômetro
                if (novoStreak > diasReaisAbstinencia + 1) {
                    novoStreak = diasReaisAbstinencia + 1;
                }

                final int streakFinal = novoStreak;

                Map<String, Object> updates = new HashMap<>();
                updates.put("checkins/" + hoje, true);
                updates.put("ultimoCheckIn", hoje);
                updates.put("streakAtual", streakFinal);

                android.util.Log.d("MainActivity", "realizarCheckIn: diasReaisAbstinencia=" + diasReaisAbstinencia + " -> Atualizando streak para " + streakFinal);

                userRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    prefs.edit().putInt("streak_atual", streakFinal).apply();

                    buttonCheckInDiario.setText("✅ Compromisso Feito!");
                    buttonCheckInDiario.setEnabled(false);
                    buttonCheckInDiario.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.GRAY));
                    if (textViewDiasValidos != null) {
                        textViewDiasValidos.setText("⭐ " + streakFinal);
                    }

                    verificarEConcederMedalhasEExibirMensagem(user.getUid(), streakFinal);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void verificarEConcederMedalhasEExibirMensagem(String userId, int dias) {
        DatabaseReference conquistasRef = FirebaseDatabase.getInstance().getReference("users")
                .child(userId).child("conquistas");

        conquistasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                String tituloNovaConquista = null;
                String mensagemMotivacional = null;

                android.util.Log.d("Conquistas", "Verificando conquistas para streak=" + dias);
                android.util.Log.d("Conquistas", "Conquistas atuais no Firebase: " + snapshot.getValue());

                // Checa as milestones — usa getValue(Boolean.class) em vez de hasChild()
                // porque hasChild() retorna true mesmo se o valor for false
                if (dias >= 1 && !isConquistaDesbloqueada(snapshot, "badge_1_dia")) {
                    updates.put("badge_1_dia", true);
                    tituloNovaConquista = "🥉 Medalha de 1 Dia";
                    mensagemMotivacional = "O primeiro passo é sempre o mais importante. Você começou sua jornada de compromisso!";
                }
                if (dias >= 3 && !isConquistaDesbloqueada(snapshot, "badge_3_dias")) {
                    updates.put("badge_3_dias", true);
                    tituloNovaConquista = "🥈 Medalha de 3 Dias";
                    mensagemMotivacional = "Três dias firme! Sua disciplina está se consolidando. Continue assim!";
                }
                if (dias >= 7 && !isConquistaDesbloqueada(snapshot, "badge_1_semana")) {
                    updates.put("badge_1_semana", true);
                    tituloNovaConquista = "🥇 Medalha de 1 Semana";
                    mensagemMotivacional = "Uma semana inteira de compromisso diário! Você está provando sua força interior.";
                }
                if (dias >= 30 && !isConquistaDesbloqueada(snapshot, "badge_1_mes")) {
                    updates.put("badge_1_mes", true);
                    tituloNovaConquista = "🏅 Medalha de 1 Mês";
                    mensagemMotivacional = "30 dias de compromisso! Isso é mais do que determinação — é transformação real.";
                }
                if (dias >= 90 && !isConquistaDesbloqueada(snapshot, "badge_3_meses")) {
                    updates.put("badge_3_meses", true);
                    tituloNovaConquista = "🔥 Medalha de 3 Meses";
                    mensagemMotivacional = "90 dias! Você já mudou hábitos profundos. Sua versão mais forte está aqui!";
                }
                if (dias >= 180 && !isConquistaDesbloqueada(snapshot, "badge_6_meses")) {
                    updates.put("badge_6_meses", true);
                    tituloNovaConquista = "💎 Medalha de 6 Meses";
                    mensagemMotivacional = "Meio ano de compromisso diário! Você é uma inspiração para a comunidade.";
                }
                if (dias >= 365 && !isConquistaDesbloqueada(snapshot, "badge_1_ano")) {
                    updates.put("badge_1_ano", true);
                    tituloNovaConquista = "👑 Medalha de 1 Ano";
                    mensagemMotivacional = "UM ANO! A maior conquista possível. Você é a prova viva de que a mudança é real!";
                }

                if (!updates.isEmpty()) {
                    android.util.Log.d("Conquistas", "Novas conquistas desbloqueadas: " + updates.keySet());
                    conquistasRef.updateChildren(updates);
                }

                if (tituloNovaConquista != null) {
                    android.util.Log.d("Conquistas", "Exibindo dialog de conquista: " + tituloNovaConquista);
                    exibirDialogConquista(tituloNovaConquista, mensagemMotivacional);
                } else {
                    android.util.Log.d("Conquistas", "Nenhuma nova conquista. Exibindo mensagem normal de check-in.");
                    // Mensagem Normal de Check-in Regular
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Compromisso Firmado!")
                            .setMessage("Parabéns por renovar seu compromisso de sobriedade de hoje. Você está no caminho certo!")
                            .setPositiveButton("Vamos lá!", null)
                            .show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("Conquistas", "Erro ao verificar conquistas: " + error.getMessage());
            }
        });
    }

    /** Verifica se uma conquista está realmente desbloqueada (valor true no Firebase) */
    private boolean isConquistaDesbloqueada(DataSnapshot snapshot, String badgeKey) {
        if (!snapshot.hasChild(badgeKey)) return false;
        Boolean valor = snapshot.child(badgeKey).getValue(Boolean.class);
        return valor != null && valor;
    }

    private void exibirDialogConquista(String titulo, String mensagem) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_conquista);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    android.view.WindowManager.LayoutParams.MATCH_PARENT,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT
            );
        }

        // Configura os textos
        TextView textTitulo = dialog.findViewById(R.id.textViewConquistaNome);
        TextView textMensagem = dialog.findViewById(R.id.textViewConquistaMensagem);
        textTitulo.setText(titulo);
        textMensagem.setText(mensagem);

        // Animação de bounce-in no dialog inteiro
        View dialogRoot = dialog.findViewById(android.R.id.content);
        if (dialogRoot != null) {
            android.view.animation.Animation bounceIn = android.view.animation.AnimationUtils
                    .loadAnimation(this, R.anim.conquista_bounce_in);
            dialogRoot.startAnimation(bounceIn);
        }

        // Animação de pulsar no troféu
        android.widget.ImageView trofeu = dialog.findViewById(R.id.imageViewTrofeu);
        if (trofeu != null) {
            android.view.animation.Animation pulse = android.view.animation.AnimationUtils
                    .loadAnimation(this, R.anim.pulse_trophy);
            trofeu.startAnimation(pulse);
        }

        // Animação de rotação no glow de fundo
        View glow = dialog.findViewById(R.id.viewGlowBackground);
        if (glow != null) {
            ObjectAnimator rotation = ObjectAnimator.ofFloat(glow, "rotation", 0f, 360f);
            rotation.setDuration(8000);
            rotation.setRepeatCount(ObjectAnimator.INFINITE);
            rotation.setInterpolator(new android.view.animation.LinearInterpolator());
            rotation.start();
        }

        // Botão Ver Perfil
        dialog.findViewById(R.id.buttonVerPerfil).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(MainActivity.this, PerfilActivity.class));
        });

        // Botão Fechar
        dialog.findViewById(R.id.textViewFechar).setOnClickListener(v -> dialog.dismiss());

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void verificarCheckInDiario() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null)
            return;

        String hoje = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
                
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DATE, -1);
        String ontem = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(cal.getTime());

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer streakAtual = snapshot.child("streakAtual").getValue(Integer.class);
                if (streakAtual == null) streakAtual = 0;
                
                String ultimoCheckIn = snapshot.child("ultimoCheckIn").getValue(String.class);
                
                // Quebrar a streak se não houver checkin hoje nem ontem
                if (ultimoCheckIn != null && !ultimoCheckIn.equals(hoje) && !ultimoCheckIn.equals(ontem)) {
                    android.util.Log.d("MainActivity", "verificarCheckInDiario: Streak quebra pois faltou check-in (ultimo=" + ultimoCheckIn + ")");
                    streakAtual = 0;
                    userRef.child("streakAtual").setValue(0);
                }

                // VALIDAÇÃO CRÍTICA: o streak nunca pode ser maior que os dias reais do cronômetro
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                long tempoInicialSalvo = prefs.getLong(KEY_TEMPO_INICIAL, System.currentTimeMillis());
                int diasReaisAbstinencia = (int) TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - tempoInicialSalvo);

                // Se o streak está maior que o cronômetro permite, corrige silenciosamente
                if (streakAtual > diasReaisAbstinencia + 1) {
                    android.util.Log.d("MainActivity", "verificarCheckInDiario: Streak (" + streakAtual + ") maior que permitido (" + diasReaisAbstinencia + "). Corrigindo!");
                    streakAtual = Math.max(0, diasReaisAbstinencia);
                    userRef.child("streakAtual").setValue(streakAtual);
                } else {
                    android.util.Log.d("MainActivity", "verificarCheckInDiario: Streak validado (" + streakAtual + ")");
                }

                prefs.edit().putInt("streak_atual", streakAtual).apply();

                if (textViewDiasValidos != null) {
                    textViewDiasValidos.setText("⭐ " + streakAtual);
                }

                if (snapshot.child("checkins").child(hoje).exists() || hoje.equals(ultimoCheckIn)) {
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
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 40);
        layout.setGravity(android.view.Gravity.CENTER);
        
        // Fundo escuro elegante (evita bugs visuais e destaca a cor branca solicitada)
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setCornerRadius(32f);
        shape.setColor(android.graphics.Color.parseColor("#1F2937")); // Cinza bem escuro
        layout.setBackground(shape);

        // Título
        android.widget.TextView title = new android.widget.TextView(this);
        title.setText("Contato & Feedback");
        title.setTextSize(22);
        title.setTextColor(android.graphics.Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        layout.addView(title);

        // Mensagem Explicativa
        android.widget.TextView subtitle = new android.widget.TextView(this);
        subtitle.setText("\nPara dúvidas, sugestões ou suporte técnico geral, envie um e-mail direto para nossa equipe:\n");
        subtitle.setTextSize(15);
        subtitle.setTextColor(android.graphics.Color.parseColor("#D1D5DB")); // Cinza clarinho
        subtitle.setGravity(android.view.Gravity.CENTER);
        layout.addView(subtitle);

        // Email (Branco, conforme solicitado)
        android.widget.TextView email = new android.widget.TextView(this);
        email.setText("rodolfo.bm.reserva@gmail.com");
        email.setTextSize(16);
        email.setTextColor(android.graphics.Color.WHITE);
        email.setTypeface(null, android.graphics.Typeface.BOLD);
        email.setGravity(android.view.Gravity.CENTER);
        email.setAutoLinkMask(android.text.util.Linkify.EMAIL_ADDRESSES);
        email.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        email.setLinkTextColor(android.graphics.Color.WHITE); // Transforma link vermelho em BRANCO
        layout.addView(email);

        // Espaçador
        android.view.View spacer = new android.view.View(this);
        spacer.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 40));
        layout.addView(spacer);

        // Botão Política de Privacidade
        com.google.android.material.button.MaterialButton btnPrivacy = new com.google.android.material.button.MaterialButton(this);
        btnPrivacy.setText("📜 Política de Privacidade");
        btnPrivacy.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#374151")));
        btnPrivacy.setTextColor(android.graphics.Color.WHITE);
        btnPrivacy.setCornerRadius(16);
        btnPrivacy.setOnClickListener(v -> {
            startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://gist.github.com/rodolfoboing/c68da4a7504b78036166b44b11e8c7ee")));
        });
        layout.addView(btnPrivacy);

        // Botão Fechar (Branco, conforme solicitado)
        com.google.android.material.button.MaterialButton btnFechar = new com.google.android.material.button.MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnFechar.setText("FECHAR");
        btnFechar.setTextColor(android.graphics.Color.WHITE); // Texto branco!
        android.widget.LinearLayout.LayoutParams fecharParams = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        fecharParams.setMargins(0, 20, 0, 0);
        btnFechar.setLayoutParams(fecharParams);
        btnFechar.setOnClickListener(v -> dialog.dismiss());
        layout.addView(btnFechar);

        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.show();
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
        android.util.Log.d("MainActivity", "reiniciarContador: Usuário zerou o contador de abstinência.");
        tempoInicial = System.currentTimeMillis();
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit()
                .putLong(KEY_TEMPO_INICIAL, tempoInicial)
                .putInt("streak_atual", 0)
                // Reseta flags de notificações de marcos de abstinência
                .remove("milestone_1_day_shown")
                .remove("milestone_3_days_shown")
                .remove("milestone_7_days_shown")
                .remove("milestone_30_days_shown")
                .remove("milestone_90_days_shown")
                .remove("milestone_180_days_shown")
                .remove("milestone_365_days_shown")
                .apply();
        
        atualizarTempoAbstinencia();
        
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
