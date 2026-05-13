package com.meuprojeto.eudaimoniaforum.forum;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.notification.Notification;

public class CommentNotificationHelper {

    private static final String TAG = "ComentarioNotifHelper";

    public interface NotificacaoStatusListener {
        void onStatusLoaded(Boolean isAcompanhando);
        void onStatusToggled(boolean willReceive, String message);
    }

    public static void limparNotificacoesDePost(String userId, String postId) {
        if (userId == null || postId == null) return;

        Log.d(TAG, "Silenciador de Comentarios: Buscando alertas referentes ao Post " + postId);
        DatabaseReference notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes")
                .child(userId);

        notificacoesRef.orderByChild("idReferencia").equalTo(postId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int excluidos = 0;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ds.getRef().removeValue();
                            excluidos++;
                        }
                        Log.d(TAG, "Silenciador de Comentarios: Finalizado. " + excluidos
                                + " notificação(ões) relacionada(s) foram apagadas local/servidor.");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Erro na varredura " + error.getMessage());
                    }
                });
    }

    public static void verificarStatusNotificacao(String userId, String postId, NotificacaoStatusListener listener) {
        if (userId == null || postId == null) return;

        DatabaseReference seguidorRef = FirebaseDatabase.getInstance().getReference("forum/posts")
                .child(postId).child("seguidores").child(userId);

        seguidorRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (listener != null) {
                    listener.onStatusLoaded(snapshot.exists() ? snapshot.getValue(Boolean.class) : null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public static void alternarNotificacao(String userId, String autorDoPostId, String postId, Boolean isAcompanhando, NotificacaoStatusListener listener) {
        if (userId == null || postId == null) return;

        boolean currentlyReceiving = (isAcompanhando == null) ? (autorDoPostId != null && autorDoPostId.equals(userId)) : isAcompanhando;
        boolean willReceive = !currentlyReceiving;

        DatabaseReference seguidorRef = FirebaseDatabase.getInstance().getReference("forum/posts")
                .child(postId).child("seguidores").child(userId);

        seguidorRef.setValue(willReceive).addOnSuccessListener(aVoid -> {
            if (listener != null) {
                String msg = willReceive ? "Você será notificado sobre novos comentários." : "Notificações silenciadas para esta postagem.";
                listener.onStatusToggled(willReceive, msg);
            }
        });
    }

    public static void verificarEEnviarNotificacao(String currentUserId, String autorDoPostId, String postId, String conteudoComentario) {
        DatabaseReference seguidoresRef = FirebaseDatabase.getInstance().getReference("forum/posts")
                .child(postId).child("seguidores");

        seguidoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean autorGerenciado = false;

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String userId = ds.getKey();
                    Boolean status = ds.getValue(Boolean.class);

                    if (autorDoPostId != null && autorDoPostId.equals(userId)) {
                        autorGerenciado = true;
                    }

                    if (Boolean.TRUE.equals(status) && !userId.equals(currentUserId)) {
                        enviarNotificacaoParaUsuario(userId, postId, conteudoComentario);
                    }
                }

                if (!autorGerenciado && autorDoPostId != null && !autorDoPostId.equals(currentUserId)) {
                    enviarNotificacaoParaUsuario(autorDoPostId, postId, conteudoComentario);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static void enviarNotificacaoParaUsuario(String userId, String postId, String conteudoComentario) {
        String resumoComentario = conteudoComentario.length() > 30 ? conteudoComentario.substring(0, 30) + "..."
                : conteudoComentario;
        String mensagemNotificacao = "Novo comentário: " + resumoComentario;

        DatabaseReference notificacaoRef = FirebaseDatabase.getInstance().getReference("notificacoes")
                .child(userId).push();
        String notifId = notificacaoRef.getKey();

        if (notifId != null) {
            Notification notification = new Notification(notifId, "comentario", mensagemNotificacao, postId,
                    System.currentTimeMillis());
            notificacaoRef.setValue(notification);
        }
    }
}
