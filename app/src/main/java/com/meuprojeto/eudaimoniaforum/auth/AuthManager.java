package com.meuprojeto.eudaimoniaforum.auth;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.profile.User;

import java.util.HashMap;
import java.util.Map;

public class AuthManager {

    private static final String TAG = "AuthManager";

    private final FirebaseAuth mAuth;
    private final DatabaseReference usersRef;
    private final DatabaseReference usernamesRef;
    private final DatabaseReference banidosRef;

    public interface AutoLoginCallback {
        void onIrParaMain();
        void onIrParaSetupPerfil();
        void onPermaneceNoLogin();
    }

    public interface LoginCallback {
        void onSuccess(boolean irParaSetup);
        void onUserBanned();
        void onError(String errorMessage);
    }

    public interface RegisterCallback {
        void onNickExists();
        void onSuccess();
        void onError(String errorMessage);
    }

    public interface PasswordResetCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public AuthManager() {
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        usersRef = database.getReference("users");
        usernamesRef = database.getReference("usernames");
        banidosRef = database.getReference("banidos");
    }

    public void verificarEstadoAutoLogin(AutoLoginCallback callback) {
        FirebaseUser autoLoginUser = mAuth.getCurrentUser();
        if (autoLoginUser != null) {
            String uid = autoLoginUser.getUid();
            usersRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Boolean configurado = snapshot.child("perfilConfigurado").getValue(Boolean.class);
                    boolean isUsuarioAntigo = snapshot.child("sobreMim").exists()
                            || snapshot.child("checkins").exists()
                            || snapshot.child("conquistas").exists();

                    if ((configurado != null && configurado) || isUsuarioAntigo) {
                        callback.onIrParaMain();
                    } else {
                        callback.onIrParaSetupPerfil();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    callback.onIrParaMain();
                }
            });
        } else {
            callback.onPermaneceNoLogin();
        }
    }

    public void login(String email, String password, LoginCallback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        verificarBanimento(user, callback);
                    } else {
                        Log.e(TAG, "Falha na autenticação: ", task.getException());
                        callback.onError("Falha na autenticação. Verifique suas credenciais.");
                    }
                });
    }

    private void verificarBanimento(FirebaseUser user, LoginCallback callback) {
        if (user == null) {
            callback.onError("Erro ao obter usuário autenticado.");
            return;
        }

        banidosRef.child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    mAuth.signOut();
                    callback.onUserBanned();
                } else {
                    atualizarSessao(user, callback);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Erro ao verificar banimento: ", error.toException());
                callback.onError("Erro ao verificar banimento.");
            }
        });
    }

    private void atualizarSessao(FirebaseUser user, LoginCallback callback) {
        DatabaseReference userRef = usersRef.child(user.getUid());
        userRef.child("lastLoginTimestamp").setValue(System.currentTimeMillis());

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean configurado = snapshot.child("perfilConfigurado").getValue(Boolean.class);
                boolean isUsuarioAntigo = snapshot.child("sobreMim").exists()
                        || snapshot.child("checkins").exists()
                        || snapshot.child("conquistas").exists();

                if ((configurado != null && configurado) || isUsuarioAntigo) {
                    callback.onSuccess(false); // Retorna falso para 'irParaSetup'
                } else {
                    callback.onSuccess(true); // Requere ir para Setup
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onSuccess(false);
            }
        });
    }

    public void verificarNickERegistrar(String email, String password, String nick, String vicio, RegisterCallback callback) {
        usernamesRef.child(nick).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                callback.onNickExists();
            } else if (task.isSuccessful()) {
                realizarRegistro(email, password, nick, vicio, callback);
            } else {
                callback.onError(task.getException() != null ? task.getException().getMessage() : "Erro desconhecido ao verificar nick");
            }
        });
    }

    private void realizarRegistro(String email, String password, String nick, String vicio, RegisterCallback callback) {
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                salvarDadosUsuarioBase(user, nick, vicio, callback);
            } else {
                callback.onError("Falha ao criar conta: " + (task.getException() != null ? task.getException().getMessage() : ""));
            }
        });
    }

    private void salvarDadosUsuarioBase(FirebaseUser user, String nick, String vicio, RegisterCallback callback) {
        if (user == null) {
            callback.onError("Erro ao obter usuário autenticado pós-registro.");
            return;
        }

        String userId = user.getUid();
        long dataEntradaTimestamp = System.currentTimeMillis();
        String sobreMim = "Bem-vindo ao meu profile!";

        User novoUser = new User(userId, nick, String.valueOf(dataEntradaTimestamp), sobreMim, vicio);

        Map<String, Object> updates = new HashMap<>();
        updates.put("users/" + userId, novoUser);
        updates.put("usernames/" + nick, userId);

        FirebaseDatabase.getInstance().getReference().updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError("Falha ao salvar dados no banco: " + (task.getException() != null ? task.getException().getMessage() : ""));
            }
        });
    }

    public void recuperarSenha(String email, PasswordResetCallback callback) {
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                callback.onError(task.getException() != null ? task.getException().getMessage() : "Erro desconhecido");
            }
        });
    }

}
