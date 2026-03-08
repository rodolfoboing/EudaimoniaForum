package com.meuprojeto.eudaimoniaforum;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "\u2709 onMessageReceived chamado! From: " + remoteMessage.getFrom());
        Log.d(TAG, "Notification payload: " + remoteMessage.getNotification());
        Log.d(TAG, "Data payload: " + remoteMessage.getData());

        // Enviar notificação se o app estiver em primeiro plano também
        if (remoteMessage.getNotification() != null) {
            String tipo = remoteMessage.getData().get("tipo");
            String idRef = remoteMessage.getData().get("idReferencia");
            if (deveSilenciarNotificacao(tipo, idRef))
                return;
            Log.d(TAG, "Processando notification payload: title=" + remoteMessage.getNotification().getTitle()
                    + ", body=" + remoteMessage.getNotification().getBody());
            sendNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody(),
                    tipo, idRef);
        } else if (remoteMessage.getData().size() > 0) {
            // Se for payload de dados
            String titulo = remoteMessage.getData().get("title");
            String corpo = remoteMessage.getData().get("body");
            String tipo = remoteMessage.getData().get("tipo");
            String idRef = remoteMessage.getData().get("idReferencia");
            if (deveSilenciarNotificacao(tipo, idRef))
                return;
            Log.d(TAG, "Processando data payload: title=" + titulo + ", body=" + corpo);
            sendNotification(titulo, corpo, tipo, idRef);
        } else {
            Log.w(TAG, "Mensagem recebida sem notification nem data payload!");
        }
    }

    private boolean deveSilenciarNotificacao(String tipo, String idReferencia) {
        if ("chat".equals(tipo) && idReferencia != null && idReferencia.equals(ChatActivity.activeChatUserId)) {
            Log.d(TAG, "Notificação silenciada: Usuário já está interagindo ativamente na ChatActivity com "
                    + idReferencia);
            return true;
        }
        if ("comentario".equals(tipo) && idReferencia != null && idReferencia.equals(ComentarioActivity.activePostId)) {
            Log.d(TAG, "Notificação silenciada: Usuário já está interagindo ativamente na ComentarioActivity no Post "
                    + idReferencia);
            return true;
        }
        return false;
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // Salvar token no banco de dados do usuário
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(TAG, "Salvando token FCM no banco para o usuário: " + user.getUid());
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
            userRef.child("fcmToken").setValue(token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "\u2705 Token FCM salvo com sucesso!"))
                    .addOnFailureListener(e -> Log.e(TAG, "\u274c ERRO ao salvar token FCM: " + e.getMessage()));
        } else {
            Log.w(TAG, "\u26a0 Usuário não logado ao tentar salvar token FCM. Token perdido!");
        }
    }

    private void sendNotification(String title, String messageBody, String tipo, String idReferencia) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("FROM_NOTIFICATION", true);

        if (tipo != null)
            intent.putExtra("tipo", tipo);
        if (idReferencia != null)
            intent.putExtra("idReferencia", idReferencia);

        // Cada notificação precisa de um requestCode único para não sobrescrever o
        // PendingIntent
        int requestCode = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "fcm_default_channel";
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_eudaimoniaforum)
                .setContentTitle(title != null ? title : "Eudaimonia Forum")
                .setContentText(messageBody != null ? messageBody : "Você tem uma nova notificação")
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Notificações do Fórum",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notificações de chat, comentários e metas");
            notificationManager.createNotificationChannel(channel);
        }

        // ID único para cada notificação (evita sobrescrever notificações anteriores)
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
