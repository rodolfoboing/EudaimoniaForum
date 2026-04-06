package com.meuprojeto.eudaimoniaforum.main;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.notification.Notificacao;

import java.util.HashMap;
import java.util.Map;

public class MainManager {

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

    public MainManager() {
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
            callback.onError("Usuário não autenticado");
            return;
        }

        rootRef.child("users").child(uid).child("vicio").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String vicio = snapshot.getValue(String.class);
                callback.onVicioCarregado(vicio != null && !vicio.isEmpty() ? vicio : "");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
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
            callback.onError("Não autenticado");
            return;
        }

        Map<String, Object> resetUpdates = new HashMap<>();
        resetUpdates.put("streakAtual", 0);
        resetUpdates.put("ultimoCheckIn", null);
        resetUpdates.put("checkins", null);

        rootRef.child("users").child(uid).updateChildren(resetUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError(task.getException() != null ? task.getException().getMessage() : "Erro ao zerar contadores");
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
                    Notificacao notif = doc.getValue(Notificacao.class);
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

    public void atualizarVicio(String novoVicio, AcaoCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("Não autenticado");
            return;
        }

        rootRef.child("users").child(uid).child("vicio").setValue(novoVicio).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError(task.getException() != null ? task.getException().getMessage() : "Erro ao salvar no banco");
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
