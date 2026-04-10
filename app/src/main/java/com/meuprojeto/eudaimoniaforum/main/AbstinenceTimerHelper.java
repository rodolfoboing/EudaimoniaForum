package com.meuprojeto.eudaimoniaforum.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.TimeUnit;

public class AbstinenceTimerHelper {

    private static final String PREFS_NAME = "PrefsAbstinencia";
    private static final String KEY_TEMPO_INICIAL = "tempo_inicial";

    private final Context context;
    private final TimerCallback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable runnableAtualizarTempo;
    private long tempoInicial;

    public interface TimerCallback {
        void onTimeUpdated(long meses, long dias, long horas, long minutos);
    }

    public AbstinenceTimerHelper(Context context, TimerCallback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
    }

    public void init() {
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!preferences.contains(KEY_TEMPO_INICIAL)) {
            tempoInicial = System.currentTimeMillis();
            preferences.edit().putLong(KEY_TEMPO_INICIAL, tempoInicial).apply();
        } else {
            tempoInicial = preferences.getLong(KEY_TEMPO_INICIAL, System.currentTimeMillis());
        }
    }

    public void start() {
        if (runnableAtualizarTempo == null) {
            runnableAtualizarTempo = () -> {
                refresh();
                handler.postDelayed(runnableAtualizarTempo, 1000);
            };
        }
        handler.removeCallbacks(runnableAtualizarTempo);
        handler.post(runnableAtualizarTempo);
    }

    public void stop() {
        if (runnableAtualizarTempo != null) {
            handler.removeCallbacks(runnableAtualizarTempo);
        }
    }

    public void refresh() {
        long diferenca = System.currentTimeMillis() - tempoInicial;
        long meses = TimeUnit.MILLISECONDS.toDays(diferenca) / 30;
        long dias = TimeUnit.MILLISECONDS.toDays(diferenca) % 30;
        long horas = TimeUnit.MILLISECONDS.toHours(diferenca) % 24;
        long minutos = TimeUnit.MILLISECONDS.toMinutes(diferenca) % 60;

        if (callback != null) {
            callback.onTimeUpdated(meses, dias, horas, minutos);
        }
    }

    public long reset() {
        tempoInicial = System.currentTimeMillis();
        SharedPreferences preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences.edit()
                .putLong(KEY_TEMPO_INICIAL, tempoInicial)
                .putInt("streak_atual", 0)
                .remove("milestone_1_day_shown")
                .remove("milestone_3_days_shown")
                .remove("milestone_7_days_shown")
                .remove("milestone_30_days_shown")
                .remove("milestone_90_days_shown")
                .remove("milestone_180_days_shown")
                .remove("milestone_365_days_shown")
                .apply();
        
        refresh();
        return tempoInicial;
    }

    public long getTempoInicial() {
        return tempoInicial;
    }
}
