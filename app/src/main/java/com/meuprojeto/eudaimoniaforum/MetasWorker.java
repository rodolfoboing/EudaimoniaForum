package com.meuprojeto.eudaimoniaforum;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class MetasWorker extends Worker {

    private static final String PREFS_NAME = "PrefsAbstinencia";
    private static final String KEY_TEMPO_INICIAL = "tempo_inicial";

    public MetasWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        verificarMarcosDeAbstinencia();
        return Result.success();
    }

    private void verificarMarcosDeAbstinencia() {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long tempoInicial = preferences.getLong(KEY_TEMPO_INICIAL, 0);

        if (tempoInicial == 0) {
            return;
        }

        long diferenca = System.currentTimeMillis() - tempoInicial;
        long diasDeAbstinencia = TimeUnit.MILLISECONDS.toDays(diferenca);

        // Meta de 1 dia
        if (diasDeAbstinencia >= 1 && !preferences.getBoolean("milestone_1_day_shown", false)) {
            exibirNotificacaoLocal("1 Dia de Abstinência", "Parabéns! Você atingiu 1 dia de abstinência!");
            preferences.edit().putBoolean("milestone_1_day_shown", true).apply();
        }

        // Meta de 3 dias
        if (diasDeAbstinencia >= 3 && !preferences.getBoolean("milestone_3_days_shown", false)) {
            exibirNotificacaoLocal("3 Dias de Abstinência", "Força! Você já superou os 3 primeiros dias!");
            preferences.edit().putBoolean("milestone_3_days_shown", true).apply();
        }

        // Meta de 1 semana (7 dias)
        if (diasDeAbstinencia >= 7 && !preferences.getBoolean("milestone_7_days_shown", false)) {
            exibirNotificacaoLocal("1 Semana de Abstinência", "Você completou uma semana! Continue firme!");
            preferences.edit().putBoolean("milestone_7_days_shown", true).apply();
        }

        // Meta de 1 mês (30 dias)
        if (diasDeAbstinencia >= 30 && !preferences.getBoolean("milestone_30_days_shown", false)) {
            exibirNotificacaoLocal("1 Mês de Abstinência", "Incrível! Você atingiu 1 mês de abstinência!");
            preferences.edit().putBoolean("milestone_30_days_shown", true).apply();
        }
    }

    private void exibirNotificacaoLocal(String titulo, String mensagem) {
        Context context = getApplicationContext();
        String canalId = "metas_notificacoes";
        String canalNome = "Notificações de Metas";

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(canalId, canalNome, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(canal);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, canalId)
                .setSmallIcon(R.drawable.ic_eudaimoniaforum)
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        int notificationId = titulo.hashCode();
        notificationManager.notify(notificationId, builder.build());
    }
}
