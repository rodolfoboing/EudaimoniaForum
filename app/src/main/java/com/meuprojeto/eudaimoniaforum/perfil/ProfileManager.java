package com.meuprojeto.eudaimoniaforum.perfil;

import android.text.TextUtils;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class ProfileManager {

    private final FirebaseAuth firebaseAuth;
    private final DatabaseReference userRef;
    private final FirebaseUser currentUser;
    
    public interface ProfileLoadListener {
        void onProfileLoaded(String nick, String avatar);
        void onError(String error);
    }

    public interface ProfileUpdateListener {
        void onSuccess(String message);
        void onError(String error);
    }

    public ProfileManager() {
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        } else {
            userRef = null;
        }
    }

    public FirebaseUser getCurrentUser() {
        return currentUser;
    }

    public void carregarDadosAtuais(ProfileLoadListener listener) {
        if (userRef == null) {
            if (listener != null) listener.onError("Usuário não autenticado");
            return;
        }

        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Usuario usuario = snapshot.getValue(Usuario.class);
                if (usuario != null && listener != null) {
                    listener.onProfileLoaded(usuario.getNick(), usuario.getAvatar());
                }
            } else {
                if (listener != null) listener.onError("Dados do usuário não encontrados");
            }
        }).addOnFailureListener(e -> {
            if (listener != null) listener.onError(e.getMessage());
        });
    }

    public void deletarConta(String senha, ProfileUpdateListener listener) {
        if (currentUser == null) {
            if (listener != null) listener.onError("Usuário não autenticado");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), senha);
        currentUser.reauthenticate(credential).addOnCompleteListener(authTask -> {
            if (authTask.isSuccessful()) {
                String uid = currentUser.getUid();

                userRef.child("nick").get().addOnSuccessListener(snapshot -> {
                    String nick = snapshot.getValue(String.class);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("users/" + uid, null);
                    if (nick != null) {
                        updates.put("usernames/" + nick, null);
                    }

                    FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                            .addOnCompleteListener(dbTask -> {
                                if (dbTask.isSuccessful()) {
                                    currentUser.delete().addOnCompleteListener(deleteTask -> {
                                        if (deleteTask.isSuccessful()) {
                                            if (listener != null) listener.onSuccess("Conta excluída com sucesso.");
                                        } else {
                                            if (listener != null) listener.onError("Erro ao excluir conta de autenticação: " + deleteTask.getException().getMessage());
                                        }
                                    });
                                } else {
                                    if (listener != null) listener.onError("Erro ao apagar dados do banco.");
                                }
                            });
                }).addOnFailureListener(e -> {
                    if (listener != null) listener.onError("Erro ao recuperar dados para exclusão.");
                });
            } else {
                if (listener != null) listener.onError("Senha atual incorreta. Não foi possível excluir.");
            }
        });
    }

    public void salvarAlteracoes(String senhaAtual, String novoNick, String nickOriginal, String novaApresentacao, String avatarEscolhido, String avatarOriginal, String novaSenha, ProfileUpdateListener listener) {
        if (currentUser == null) {
            if (listener != null) listener.onError("Usuário não autenticado");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), senhaAtual);
        currentUser.reauthenticate(credential).addOnCompleteListener(authTask -> {
            if (!authTask.isSuccessful()) {
                if (listener != null) listener.onError("Senha atual incorreta. Verifique e tente novamente.");
            } else {
                if (!TextUtils.isEmpty(novoNick) && !novoNick.equals(nickOriginal)) {
                    DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference("usernames");
                    usernamesRef.child(novoNick).get().addOnCompleteListener(nickTask -> {
                        if (nickTask.isSuccessful() && nickTask.getResult().exists()) {
                            if (listener != null) listener.onError("Este nick já está em uso.");
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
            FirebaseDatabase.getInstance().getReference().updateChildren(updates).addOnCompleteListener(dbTask -> {
                if (dbTask.isSuccessful()) {
                    atualizarSenhaSeNecessario(novaSenha, listener);
                } else {
                    if (listener != null) listener.onError("Erro ao salvar dados no banco.");
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
                    if (listener != null) listener.onSuccess("Perfil e senha atualizados com sucesso!");
                } else {
                    if (listener != null) listener.onError("Dados salvos, mas erro ao trocar senha: " + passTask.getException().getMessage());
                }
            });
        } else {
            if (listener != null) listener.onSuccess("Perfil atualizado com sucesso!");
        }
    }
}
