package com.meuprojeto.eudaimoniaforum;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class NotificationSetupHelper {

    public static void setupNotifications(AppCompatActivity activity) {
        criarCanalDeNotificacao(activity);
        verificarPermissaoNotificacao(activity);
        atualizarFcmToken();
    }

    private static void criarCanalDeNotificacao(AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "fcm_default_channel",
                    "Notificações do Fórum",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Canal principal para mensagens e alertas do Eudaimonia Fórum");
            NotificationManager manager = activity.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private static void verificarPermissaoNotificacao(AppCompatActivity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity,
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity,
                        new String[] { android.Manifest.permission.POST_NOTIFICATIONS }, 101);
            }
        }
    }

    private static void atualizarFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        android.util.Log.e("NotificationHelper", "❌ Falha ao obter token FCM", task.getException());
                        return;
                    }
                    String token = task.getResult();
                    android.util.Log.d("NotificationHelper",
                            "Token FCM obtido: " + (token != null ? token.substring(0, 20) + "..." : "null"));
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null && token != null) {
                        android.util.Log.d("NotificationHelper", "Salvando token FCM para userId: " + user.getUid());
                        FirebaseDatabase.getInstance().getReference("users")
                                .child(user.getUid())
                                .child("fcmToken")
                                .setValue(token)
                                .addOnSuccessListener(aVoid -> android.util.Log.d("NotificationHelper",
                                        "✅ Token FCM salvo com sucesso no banco!"))
                                .addOnFailureListener(e -> android.util.Log.e("NotificationHelper",
                                        "❌ ERRO ao salvar token: " + e.getMessage()));
                    } else {
                        android.util.Log.w("NotificationHelper",
                                "⚠ user=" + user + ", token=" + token + " — não foi possível salvar");
                    }
                });
    }
}
