package com.meuprojeto.eudaimoniaforum.notification;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificacaoManager {

    private final DatabaseReference rootRef;
    private final String currentUserId;
    private ValueEventListener notificacoesListener;

    public interface FeedCallback {
        void onLoaded(List<Notificacao> notificacoes);
        void onError(String erro);
    }

    public interface AcaoCallback {
        void onSuccess();
        void onError(String erro);
    }

    public NotificacaoManager() {
        this.rootRef = FirebaseDatabase.getInstance().getReference();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null 
                             ? FirebaseAuth.getInstance().getCurrentUser().getUid() 
                             : null;
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    public void monitorarNotificacoes(FeedCallback callback) {
        if (currentUserId == null) {
            callback.onError("Não autenticado");
            return;
        }

        DatabaseReference notificacoesRef = rootRef.child("notificacoes").child(currentUserId);
        notificacoesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Notificacao> notificacoes = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Notificacao notificacao = snapshot.getValue(Notificacao.class);
                    if (notificacao != null) {
                        notificacao.setId(snapshot.getKey());
                        notificacoes.add(notificacao);
                    }
                }
                Collections.reverse(notificacoes);
                callback.onLoaded(notificacoes);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };
        notificacoesRef.addValueEventListener(notificacoesListener);
    }

    public void marcarComoLida(String idNotificacao) {
        if (currentUserId == null || idNotificacao == null) return;
        rootRef.child("notificacoes").child(currentUserId).child(idNotificacao).child("lida").setValue(true);
    }

    public void limparTodas(AcaoCallback callback) {
        if (currentUserId == null) {
            callback.onError("Não autenticado");
            return;
        }
        rootRef.child("notificacoes").child(currentUserId).removeValue()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void removerListeners() {
        if (notificacoesListener != null && currentUserId != null) {
            rootRef.child("notificacoes").child(currentUserId).removeEventListener(notificacoesListener);
        }
    }
}
