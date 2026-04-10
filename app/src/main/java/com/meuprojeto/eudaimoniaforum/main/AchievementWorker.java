package com.meuprojeto.eudaimoniaforum.main;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.app.PendingIntent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.meuprojeto.eudaimoniaforum.R;

import java.util.concurrent.TimeUnit;

public class AchievementWorker extends Worker {

    private static final String PREFS_NAME = "PrefsAbstinencia";
    private static final String KEY_TEMPO_INICIAL = "tempo_inicial";

    public AchievementWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        android.util.Log.d("AchievementWorker", "Worker executando verificação de marcos de abstinência...");
        verificarMarcosDeAbstinencia();
        return Result.success();
    }

    private void verificarMarcosDeAbstinencia() {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Calcula os dias REAIS de abstinência pelo cronômetro (tempo_inicial)
        long tempoInicial = preferences.getLong(KEY_TEMPO_INICIAL, 0);
        if (tempoInicial == 0) {
            android.util.Log.d("AchievementWorker", "tempo_inicial não encontrado. Abortando.");
            return;
        }

        int diasDeAbstinencia = (int) TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - tempoInicial);
        android.util.Log.d("AchievementWorker", "Dias reais de abstinência: " + diasDeAbstinencia);

        if (diasDeAbstinencia <= 0) {
            return;
        }

        // Notificações de parabenização por tempo de abstinência (NÃO geram conquistas!)
        // Apenas mandam notificação push local para motivar o usuário.

        // 1 dia
        if (diasDeAbstinencia >= 1 && !preferences.getBoolean("milestone_1_day_shown", false)) {
            exibirNotificacaoLocal("🎉 1 Dia de Abstinência!",
                    "Parabéns! Você completou seu primeiro dia livre. O começo é o passo mais importante!");
            preferences.edit().putBoolean("milestone_1_day_shown", true).apply();
        }

        // 3 dias
        if (diasDeAbstinencia >= 3 && !preferences.getBoolean("milestone_3_days_shown", false)) {
            exibirNotificacaoLocal("💪 3 Dias de Abstinência!",
                    "Força! Você já superou os 3 primeiros dias. Continue firme!");
            preferences.edit().putBoolean("milestone_3_days_shown", true).apply();
        }

        // 1 semana (7 dias)
        if (diasDeAbstinencia >= 7 && !preferences.getBoolean("milestone_7_days_shown", false)) {
            exibirNotificacaoLocal("⭐ 1 Semana de Abstinência!",
                    "Incrível! Uma semana inteira livre. Sua disciplina está se fortalecendo!");
            preferences.edit().putBoolean("milestone_7_days_shown", true).apply();
        }

        // 1 mês (30 dias)
        if (diasDeAbstinencia >= 30 && !preferences.getBoolean("milestone_30_days_shown", false)) {
            exibirNotificacaoLocal("🏅 1 Mês de Abstinência!",
                    "Um mês completo! Você está provando que é mais forte que qualquer vício.");
            preferences.edit().putBoolean("milestone_30_days_shown", true).apply();
        }

        // 3 meses (90 dias)
        if (diasDeAbstinencia >= 90 && !preferences.getBoolean("milestone_90_days_shown", false)) {
            exibirNotificacaoLocal("🔥 3 Meses de Abstinência!",
                    "90 dias! Seu comprometimento é inspirador. A mudança já é real!");
            preferences.edit().putBoolean("milestone_90_days_shown", true).apply();
        }

        // 6 meses (180 dias)
        if (diasDeAbstinencia >= 180 && !preferences.getBoolean("milestone_180_days_shown", false)) {
            exibirNotificacaoLocal("💎 6 Meses de Abstinência!",
                    "Meio ano livre! Você é um exemplo de perseverança e força interior.");
            preferences.edit().putBoolean("milestone_180_days_shown", true).apply();
        }

        // 1 ano (365 dias)
        if (diasDeAbstinencia >= 365 && !preferences.getBoolean("milestone_365_days_shown", false)) {
            exibirNotificacaoLocal("🏆 1 Ano de Abstinência!",
                    "UM ANO INTEIRO! Você venceu uma das batalhas mais difíceis da vida. Orgulhe-se!");
            preferences.edit().putBoolean("milestone_365_days_shown", true).apply();
        }
    }

    private void exibirNotificacaoLocal(String titulo, String mensagem) {
        Context context = getApplicationContext();
        String canalId = "conquistas_notificacoes";
        String canalNome = "Notificações de Abstinência";

        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(canalId, canalNome,
                    NotificationManager.IMPORTANCE_HIGH);
            canal.setDescription("Notificações de marcos de tempo de abstinência");
            notificationManager.createNotificationChannel(canal);
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int requestCode = titulo.hashCode();
        PendingIntent pendingIntent = PendingIntent.getActivity(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, canalId)
                .setSmallIcon(R.drawable.ic_eudaimoniaforum)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensagem))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        int notificationId = titulo.hashCode();
        notificationManager.notify(notificationId, builder.build());
    }
}
