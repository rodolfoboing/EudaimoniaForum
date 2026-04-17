package com.meuprojeto.eudaimoniaforum.main;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.notification.Notification;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;

public class MainManager {

    private final Context context;
    private final FirebaseAuth firebaseAuth;
    private final DatabaseReference rootRef;
    
    private ValueEventListener notificacoesListener;
    private DatabaseReference notificacoesRef;

    public interface DadosUsuarioCallback {
        void onVicioCarregado(String vicio);
        void onError(String erro);
    }

    public interface ModeradorCallback {
        void onCheckComplete(boolean isModerator);
    }

    public interface AcaoCallback {
        void onSuccess();
        void onError(String erro);
    }

    public interface NotificacaoCallback {
        void onStatusNaoLidaStatusChanged(boolean temNaoLida);
    }

    public MainManager(Context context) {
        this.context = context.getApplicationContext();
        firebaseAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();
        
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            rootRef.child("users").child(user.getUid()).keepSynced(true);
        }
    }

    public String getCurrentUserId() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public void deslogar() {
        firebaseAuth.signOut();
    }

    public void carregarVicioDoUsuario(DadosUsuarioCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_unauthenticated));
            return;
        }

        rootRef.child("users").child(uid).child("vicio").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String vicio = snapshot.getValue(String.class);
                callback.onVicioCarregado(vicio != null && !vicio.isEmpty() ? vicio : "");
            }

            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(com.meuprojeto.eudaimoniaforum.utils.FirebaseErrorHandler.getFriendlyMessage(context, error));
            }
        });
    }

    public void verificarModerador(ModeradorCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onCheckComplete(false);
            return;
        }

        rootRef.child("moderadores").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                callback.onCheckComplete(snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class)));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onCheckComplete(false);
            }
        });
    }

    public void zerarContadorRastreamento(AcaoCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_unauthenticated));
            return;
        }

        Map<String, Object> resetUpdates = new HashMap<>();
        resetUpdates.put("streakAtual", 0);
        resetUpdates.put("ultimoCheckIn", null);
        resetUpdates.put("checkins", null);
        resetUpdates.put("tempoInicial", System.currentTimeMillis());

        rootRef.child("users").child(uid).updateChildren(resetUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError(com.meuprojeto.eudaimoniaforum.utils.FirebaseErrorHandler.getFriendlyMessage(context, task.getException()));
            }
        });
    }

    public void monitorarNotificacoesNaoLidas(NotificacaoCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) return;

        notificacoesRef = rootRef.child("notificacoes").child(uid);
        notificacoesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean temNaoLida = false;
                for (DataSnapshot doc : snapshot.getChildren()) {
                    Notification notif = doc.getValue(Notification.class);
                    if (notif != null && !notif.isLida()) {
                        temNaoLida = true;
                        break;
                    }
                }
                callback.onStatusNaoLidaStatusChanged(temNaoLida);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        notificacoesRef.addValueEventListener(notificacoesListener);
    }

    public void marcarNotificacaoComoLidaPorReferencia(String tipo, String idReferencia) {
        String uid = getCurrentUserId();
        if (uid == null || tipo == null || idReferencia == null) return;

        rootRef.child("notificacoes").child(uid).orderByChild("idReferencia").equalTo(idReferencia)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    for (DataSnapshot no : snapshot.getChildren()) {
                        Notification notif = no.getValue(Notification.class);
                        if (notif != null && tipo.equals(notif.getTipo()) && !notif.isLida()) {
                            no.getRef().child("lida").setValue(true);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    public void atualizarConfiguracaoAbstinencia(String novoVicio, long novoTempo, AcaoCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_unauthenticated));
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("vicio", novoVicio);
        if (novoTempo > 0) {
            updates.put("tempoInicial", novoTempo);
        }

        rootRef.child("users").child(uid).updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError(com.meuprojeto.eudaimoniaforum.utils.FirebaseErrorHandler.getFriendlyMessage(context, task.getException()));
            }
        });
    }


    public void removerListeners() {
        if (notificacoesRef != null && notificacoesListener != null) {
            notificacoesRef.removeEventListener(notificacoesListener);
            notificacoesListener = null;
        }
    }
}
