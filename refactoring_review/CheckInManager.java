package com.meuprojeto.eudaimoniaforum;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CheckInManager {

    private static final String PREFS_NAME = "PrefsAbstinencia";
    private static final String KEY_TEMPO_INICIAL = "tempo_inicial";

    private final Context context;
    private final FirebaseAuth firebaseAuth;

    public interface CheckInStatusCallback {
        void onCheckInStateLoaded(boolean isCheckInCompletedToday, int correctStreak);
    }

    public interface CheckInActionCallback {
        void onCheckInSuccess(int newStreak);
        void onNewAchievementUnlocked(String tituloNovaConquista, String mensagemMotivacional);
        void onRegularCheckInCompleted();
    }

    public CheckInManager(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public void checkCurrentStatus(CheckInStatusCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

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
                    android.util.Log.d("CheckInManager", "checkCurrentStatus: Streak quebra pois faltou check-in (ultimo=" + ultimoCheckIn + ")");
                    streakAtual = 0;
                    userRef.child("streakAtual").setValue(0);
                }

                // VALIDAÇÃO CRÍTICA: o streak nunca pode ser maior que os dias reais do cronômetro
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                long tempoInicialSalvo = prefs.getLong(KEY_TEMPO_INICIAL, System.currentTimeMillis());
                int diasReaisAbstinencia = (int) TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - tempoInicialSalvo);

                // Se o streak está maior que o cronômetro permite, corrige silenciosamente
                if (streakAtual > diasReaisAbstinencia + 1) {
                    android.util.Log.d("CheckInManager", "checkCurrentStatus: Streak (" + streakAtual + ") maior que permitido (" + diasReaisAbstinencia + "). Corrigindo!");
                    streakAtual = Math.max(0, diasReaisAbstinencia);
                    userRef.child("streakAtual").setValue(streakAtual);
                }

                prefs.edit().putInt("streak_atual", streakAtual).apply();

                boolean isCheckInCompletedToday = snapshot.child("checkins").child(hoje).exists() || hoje.equals(ultimoCheckIn);
                
                if (callback != null) {
                    callback.onCheckInStateLoaded(isCheckInCompletedToday, streakAtual);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void performCheckIn(CheckInActionCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        String hoje = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .format(new java.util.Date());
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer currentStreak = snapshot.child("streakAtual").getValue(Integer.class);
                if (currentStreak == null) currentStreak = 0;

                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                long tempoInicialSalvo = prefs.getLong(KEY_TEMPO_INICIAL, System.currentTimeMillis());
                int diasReaisAbstinencia = (int) TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - tempoInicialSalvo);

                if (currentStreak > diasReaisAbstinencia) {
                    currentStreak = diasReaisAbstinencia;
                }

                int novoStreak = currentStreak + 1;

                if (novoStreak > diasReaisAbstinencia + 1) {
                    novoStreak = diasReaisAbstinencia + 1;
                }

                final int streakFinal = novoStreak;

                Map<String, Object> updates = new HashMap<>();
                updates.put("checkins/" + hoje, true);
                updates.put("ultimoCheckIn", hoje);
                updates.put("streakAtual", streakFinal);

                userRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                    prefs.edit().putInt("streak_atual", streakFinal).apply();

                    if (callback != null) {
                        callback.onCheckInSuccess(streakFinal);
                    }

                    verificarEConcederMedalhas(user.getUid(), streakFinal, callback);
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void verificarEConcederMedalhas(String userId, int dias, CheckInActionCallback callback) {
        DatabaseReference conquistasRef = FirebaseDatabase.getInstance().getReference("users")
                .child(userId).child("conquistas");

        conquistasRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                String tituloNovaConquista = null;
                String mensagemMotivacional = null;

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
                    conquistasRef.updateChildren(updates);
                }

                if (callback != null) {
                    if (tituloNovaConquista != null) {
                        callback.onNewAchievementUnlocked(tituloNovaConquista, mensagemMotivacional);
                    } else {
                        callback.onRegularCheckInCompleted();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private boolean isConquistaDesbloqueada(DataSnapshot snapshot, String badgeKey) {
        if (!snapshot.hasChild(badgeKey)) return false;
        Boolean valor = snapshot.child(badgeKey).getValue(Boolean.class);
        return valor != null && valor;
    }
}
