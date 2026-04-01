package com.meuprojeto.eudaimoniaforum.chat;

import android.util.Log;
import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.notification.Notificacao;
import com.meuprojeto.eudaimoniaforum.perfil.Usuario;

public class ChatNotificationHelper {

    private static final String TAG = "ChatNotificationHelper";

    public static void limparNotificacaoDeChat(String currentUserId, String receiverId) {
        if (currentUserId == null || receiverId == null) return;
        Log.d(TAG, "Silenciador de Chat: Limpando alertas e notificações pendentes do usuário " + receiverId);
        
        DatabaseReference notificacaoRef = FirebaseDatabase.getInstance().getReference("notificacoes")
                .child(currentUserId).child("chat_" + receiverId);
        notificacaoRef.removeValue().addOnSuccessListener(aVoid -> {
            Log.d(TAG, "Silenciador de Chat: Limpeza concluída do histórico na nuvem (se existia).");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Silenciador de Chat: Falha ao tentar limpar: " + e.getMessage());
        });
    }

    public static void enviarNotificacao(String currentUserId, String receiverId) {
        Log.d(TAG, "enviarNotificacao() chamado. receiverId=" + receiverId + ", currentUserId=" + currentUserId);

        DatabaseReference currentUserRef = FirebaseDatabase.getInstance().getReference("users").child(currentUserId);
        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Usuario user = snapshot.getValue(Usuario.class);
                String nomeRemetente = (user != null) ? user.getNick() : "Alguém";
                Log.d(TAG, "Nome do remetente: " + nomeRemetente);

                String notifId = "chat_" + currentUserId;
                String mensagemNotificacao = "Nova mensagem de " + nomeRemetente;

                DatabaseReference notificacaoRef = FirebaseDatabase.getInstance().getReference("notificacoes")
                        .child(receiverId).child(notifId);

                notificacaoRef.removeValue().addOnCompleteListener(task -> {
                    Notificacao notificacao = new Notificacao(notifId, "chat",
                            mensagemNotificacao, currentUserId, System.currentTimeMillis());

                    Log.d(TAG, "Criando notificação nova após remoção. ID=" + notifId);
                    notificacaoRef.setValue(notificacao)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Notificação gravada no Firebase com sucesso!"))
                            .addOnFailureListener(e -> Log.e(TAG, "❌ ERRO ao gravar notificação: " + e.getMessage()));
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Erro ao buscar dados do remetente: " + error.getMessage());
            }
        });
    }
}
