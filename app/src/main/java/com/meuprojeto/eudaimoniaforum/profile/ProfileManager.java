package com.meuprojeto.eudaimoniaforum.profile;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProfileManager {

    private final Context context;
    private final FirebaseAuth firebaseAuth;
    private final DatabaseReference rootRef;
    private final FirebaseUser currentUser;
    
    public interface ProfileLoadListener {
        void onProfileLoaded(String nick, String avatar);
        void onError(String error);
    }

    public interface ProfileUpdateListener {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface FullProfileCallback {
        void onProfileDataLoaded(User user, long diasValidos);
        void onError(String erro);
    }

    public interface ProfileStatsCallback {
        void onStatsLoaded(long numPosts, long numComentarios);
    }

    public interface ConquistasCallback {
        void onConquistasLoaded(Set<String> badgetIds);
        void onNenhumaConquista();
    }

    public ProfileManager(Context context) {
        this.context = context.getApplicationContext();
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        rootRef = FirebaseDatabase.getInstance().getReference();
    }

    public FirebaseUser getCurrentUser() {
        return currentUser;
    }

    // ==== Edição e Carregamento Próprio ====

    public void carregarDadosAtuais(ProfileLoadListener listener) {
        if (currentUser == null) {
            if (listener != null) listener.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_unauthenticated));
            return;
        }

        rootRef.child("users").child(currentUser.getUid()).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                User user = snapshot.getValue(User.class);
                if (user != null && listener != null) {
                    listener.onProfileLoaded(user.getNick(), user.getAvatar());
                }
            } else {
                if (listener != null) listener.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_user_data_not_found));
            }
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    public void salvarAlteracoes(String senhaAtual, String novoNick, String nickOriginal, String novaApresentacao, String avatarEscolhido, String avatarOriginal, String novaSenha, ProfileUpdateListener listener) {
        if (currentUser == null) {
            if (listener != null) listener.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_unauthenticated));
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), senhaAtual);
        currentUser.reauthenticate(credential).addOnCompleteListener(authTask -> {
            if (!authTask.isSuccessful()) {
                if (listener != null) listener.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_incorrect_current_password));
            } else {
                if (!TextUtils.isEmpty(novoNick) && !novoNick.equals(nickOriginal)) {
                    DatabaseReference usernamesRef = rootRef.child("usernames");
                    usernamesRef.child(novoNick).get().addOnCompleteListener(nickTask -> {
                        if (nickTask.isSuccessful() && nickTask.getResult().exists()) {
                            if (listener != null) listener.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_nick_in_use));
                        } else {
                            aplicarMudancasFinal(novoNick, nickOriginal, novaApresentacao, avatarEscolhido, avatarOriginal, novaSenha, true, listener);
                        }
                    });
                } else {
                    aplicarMudancasFinal(nickOriginal, nickOriginal, novaApresentacao, avatarEscolhido, avatarOriginal, novaSenha, false, listener);
                }
            }
        });
    }

    private void aplicarMudancasFinal(String nickFinal, String nickOriginal, String apresentacaoFinal, String avatarEscolhido, String avatarOriginal, String novaSenha, boolean mudouNick, ProfileUpdateListener listener) {
        Map<String, Object> updates = new HashMap<>();

        if (mudouNick) {
            updates.put("users/" + currentUser.getUid() + "/nick", nickFinal);
            updates.put("usernames/" + nickFinal, currentUser.getUid());
            if (nickOriginal != null) {
                updates.put("usernames/" + nickOriginal, null); 
            }
        }

        if (!TextUtils.isEmpty(apresentacaoFinal)) {
            updates.put("users/" + currentUser.getUid() + "/sobreMim", apresentacaoFinal);
        }

        if (avatarEscolhido != null && !avatarEscolhido.equals(avatarOriginal)) {
            updates.put("users/" + currentUser.getUid() + "/avatar", avatarEscolhido);
        }

        if (!updates.isEmpty()) {
            rootRef.updateChildren(updates).addOnCompleteListener(dbTask -> {
                if (dbTask.isSuccessful()) {
                    atualizarSenhaSeNecessario(novaSenha, listener);
                } else {
                    if (listener != null) listener.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_saving_db));
                }
            });
        } else {
            atualizarSenhaSeNecessario(novaSenha, listener);
        }
    }

    private void atualizarSenhaSeNecessario(String novaSenha, ProfileUpdateListener listener) {
        if (!TextUtils.isEmpty(novaSenha)) {
            currentUser.updatePassword(novaSenha).addOnCompleteListener(passTask -> {
                if (passTask.isSuccessful()) {
                    if (listener != null) listener.onSuccess(context.getString(com.meuprojeto.eudaimoniaforum.R.string.msg_profile_pass_updated));
                } else {
                    if (listener != null) listener.onError("Dados salvos, mas erro ao trocar senha: " + passTask.getException().getMessage());
                }
            });
        } else {
            if (listener != null) listener.onSuccess(context.getString(com.meuprojeto.eudaimoniaforum.R.string.msg_profile_updated));
        }
    }

    public void deletarConta(String senha, ProfileUpdateListener listener) {
        if (currentUser == null) {
            if (listener != null) listener.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_unauthenticated));
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), senha);
        currentUser.reauthenticate(credential).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                String uid = currentUser.getUid();

                rootRef.child("users").child(uid).child("nick").get().addOnSuccessListener(snapshot -> {
                    String nick = snapshot.getValue(String.class);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("users/" + uid, null);
                    if (nick != null) {
                        updates.put("usernames/" + nick, null);
                    }
                    updates.put("user_conversas/" + uid, null);
                    updates.put("notificacoes/" + uid, null);
                    updates.put("moderadores/" + uid, null);
                    updates.put("banidos/" + uid, null);

                    rootRef.updateChildren(updates)
                            .addOnCompleteListener(dbTask -> {
                                if (dbTask.isSuccessful()) {
                                    currentUser.delete().addOnCompleteListener(deleteTask -> {
                                        if (deleteTask.isSuccessful()) {
                                            if (listener != null) listener.onSuccess(context.getString(com.meuprojeto.eudaimoniaforum.R.string.msg_account_deleted));
                                        } else {
                                            if (listener != null) listener.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_auth_delete_failed) + deleteTask.getException().getMessage());
                                        }
                                    });
                                } else {
                                    if (listener != null) listener.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_delete_db));
                                }
                            });
                }).addOnFailureListener(e -> {
                    if (listener != null) listener.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_retrieve_delete_data));
                });
            } else {
                if (listener != null) listener.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_delete_wrong_password));
            }
        });
    }

    // ==== Visualização de Perfis (Qualquer Usuário) ====

    public void carregarPerfilPorId(String userId, FullProfileCallback callback) {
        rootRef.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    long diasValidos = snapshot.child("checkins").getChildrenCount();
                    callback.onProfileDataLoaded(user, diasValidos);
                } else {
                    callback.onError(context.getString(com.meuprojeto.eudaimoniaforum.R.string.error_user_not_found));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public ValueEventListener monitorarConquistas(String userId, ConquistasCallback callback) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onNenhumaConquista();
                    return;
                }
                
                Set<String> keys = new HashSet<>();
                for(DataSnapshot ds : snapshot.getChildren()) {
                    keys.add(ds.getKey()); // Ex: badge_1_dia
                }
                callback.onConquistasLoaded(keys);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onNenhumaConquista();
            }
        };
        rootRef.child("users").child(userId).child("conquistas").addValueEventListener(listener);
        return listener;
    }

    public void removerConquistasListener(String userId, ValueEventListener listener) {
        if(userId != null && listener != null) {
            rootRef.child("users").child(userId).child("conquistas").removeEventListener(listener);
        }
    }

    public void carregarProgressoEstatisticas(String userId, ProfileStatsCallback callback) {
        rootRef.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long totalPosts = 0;
                    long totalComments = 0;
                    
                    if (snapshot.hasChild("totalPosts")) {
                        totalPosts = snapshot.child("totalPosts").getValue(Long.class);
                    }
                    if (snapshot.hasChild("totalComentarios")) {
                        totalComments = snapshot.child("totalComentarios").getValue(Long.class);
                    }
                    
                    callback.onStatsLoaded(totalPosts, totalComments);
                } else {
                    callback.onStatsLoaded(0, 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onStatsLoaded(0, 0);
            }
        });
    }
}
