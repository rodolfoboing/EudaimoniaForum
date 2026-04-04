package com.meuprojeto.eudaimoniaforum.forum;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.MutableData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.meuprojeto.eudaimoniaforum.utils.AppLogger;

public class ComentarioManager {

    private final String postId;
    private final String currentUserId;
    private String autorDoPostId;
    private boolean isModerador = false;

    private final DatabaseReference postRef;
    private final DatabaseReference comentariosRef;
    private final DatabaseReference moderadoresRef;

    private static long lastCommentTimestamp = 0;

    private ValueEventListener comentariosListener;

    public interface ComentarioUpdateListener {
        void onPostLoaded(Post post);
        void onPostLoadError(String error);
        void onComentariosLoaded(List<Comentario> comentarios, boolean isModerador, String autorDoPostId);
        void onComentariosLoadError(String error);
        void onActionSuccess(String message);
        void onActionFailure(String error);
        void onCommentAdded(String conteudoComentario);
    }

    public ComentarioManager(String currentUserId, String postId) {
        this.currentUserId = currentUserId;
        this.postId = postId;

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        this.postRef = db.getReference("forum/posts").child(postId);
        this.comentariosRef = db.getReference("forum/comentarios").child(postId);
        
        if (currentUserId != null) {
            this.moderadoresRef = db.getReference("moderadores").child(currentUserId);
        } else {
            this.moderadoresRef = null;
        }
    }

    public void iniciarCarregamento(ComentarioUpdateListener listener) {
        postRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Post post = snapshot.getValue(Post.class);
                if (post != null) {
                    autorDoPostId = post.getAutor();
                    if (listener != null) listener.onPostLoaded(post);
                }
                verificarModerador(listener);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) listener.onPostLoadError("Erro ao carregar dados do post");
                verificarModerador(listener);
            }
        });
    }

    private void verificarModerador(ComentarioUpdateListener listener) {
        if (currentUserId == null || moderadoresRef == null) {
            carregarComentarios(listener);
            return;
        }

        moderadoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isModerador = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                carregarComentarios(listener);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                carregarComentarios(listener);
            }
        });
    }

    private void carregarComentarios(ComentarioUpdateListener listener) {
        comentariosListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Comentario> comentarios = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Comentario comentario = ds.getValue(Comentario.class);
                    if (comentario != null) {
                        comentario.setId(ds.getKey());
                        comentario.setPostId(postId);
                        comentarios.add(comentario);
                    }
                }
                if (listener != null) listener.onComentariosLoaded(comentarios, isModerador, autorDoPostId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (listener != null) listener.onComentariosLoadError("Erro ao carregar comentários");
            }
        };
        comentariosRef.addValueEventListener(comentariosListener);
    }

    public void adicionarComentario(String conteudo, ComentarioUpdateListener listener) {
        if (currentUserId == null) return;
        
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = 30000; // 30 segundos
        if (currentTime - lastCommentTimestamp < cooldownMillis) {
            long remainingSeconds = (cooldownMillis - (currentTime - lastCommentTimestamp)) / 1000;
            if (listener != null) listener.onActionFailure("Aguarde " + remainingSeconds + " segundos para comentar novamente.");
            AppLogger.logSpam(currentUserId, "Comentarios");
            return;
        }
        
        String comentarioId = comentariosRef.push().getKey();
        if (comentarioId != null) {
            String data = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            long timestamp = currentTime;
            Comentario comentario = new Comentario(currentUserId, conteudo, data, timestamp);

            comentariosRef.child(comentarioId).setValue(comentario)
                    .addOnSuccessListener(aVoid -> {
                        lastCommentTimestamp = currentTime;
                        incrementarNumeroComentarios();
                        salvarReferenciaPostComentado(currentUserId, postId);
                        if (listener != null) listener.onCommentAdded(conteudo);
                    })
                    .addOnFailureListener(e -> {
                        if (listener != null) listener.onActionFailure("Erro ao enviar comentário");
                        AppLogger.logDbError("Enviar Comentario", e.getMessage());
                    });
        }
    }

    private void salvarReferenciaPostComentado(String userId, String postId) {
        DatabaseReference userPostsComentadosRef = FirebaseDatabase.getInstance().getReference("users")
                .child(userId).child("postsComentados").child(postId);
        userPostsComentadosRef.setValue(true);
    }

    private void incrementarNumeroComentarios() {
        postRef.child("numeroComentarios").runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Integer currentCount = currentData.getValue(Integer.class);
                if (currentCount == null) {
                    currentData.setValue(1);
                } else {
                    currentData.setValue(currentCount + 1);
                }
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
            }
        });
    }

    public void excluirPostagem(ComentarioUpdateListener listener) {
        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/forum/posts/" + postId, null);
        childUpdates.put("/forum/comentarios/" + postId, null);
        if (autorDoPostId != null) {
            childUpdates.put("/users/" + autorDoPostId + "/posts/" + postId, null);
        }

        FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                .addOnSuccessListener(unused -> {
                    if (listener != null) listener.onActionSuccess("Postagem excluída.");
                });
    }

    public void removerListeners() {
        if (comentariosRef != null && comentariosListener != null) {
            comentariosRef.removeEventListener(comentariosListener);
        }
    }

    public String getAutorDoPostId() {
        return autorDoPostId;
    }

    public boolean isModerador() {
        return isModerador;
    }
}
