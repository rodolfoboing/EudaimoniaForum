package com.meuprojeto.eudaimoniaforum.perfil;

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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileManager {

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
        void onProfileDataLoaded(Usuario usuario, long diasValidos);
        void onError(String erro);
    }

    public interface ProfileStatsCallback {
        void onStatsLoaded(long numPosts, long numComentarios);
    }

    public interface ConquistasCallback {
        void onConquistasLoaded(List<String> badgetIds);
        void onNenhumaConquista();
    }

    public ProfileManager() {
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
            if (listener != null) listener.onError("Usuário não autenticado");
            return;
        }

        rootRef.child("users").child(currentUser.getUid()).get().addOnSuccessListener(snapshot -> {
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
                    DatabaseReference usernamesRef = rootRef.child("usernames");
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
            rootRef.updateChildren(updates).addOnCompleteListener(dbTask -> {
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

    public void deletarConta(String senha, ProfileUpdateListener listener) {
        if (currentUser == null) {
            if (listener != null) listener.onError("Usuário não autenticado");
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

                    rootRef.updateChildren(updates)
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

    // ==== Visualização de Perfis (Qualquer Usuário) ====

    public void carregarPerfilPorId(String userId, FullProfileCallback callback) {
        rootRef.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Usuario usuario = snapshot.getValue(Usuario.class);
                    long diasValidos = snapshot.child("checkins").getChildrenCount();
                    callback.onProfileDataLoaded(usuario, diasValidos);
                } else {
                    callback.onError("Usuário não encontrado.");
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
                
                List<String> keys = new ArrayList<>();
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
        Query postsQuery = rootRef.child("forum/posts").orderByChild("autor").equalTo(userId);
        postsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot postsSnapshot) {
                long totalPosts = postsSnapshot.getChildrenCount();

                // Busca contagem de comentários (fallback no client-side)
                rootRef.child("forum/comentarios").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot commentsContainerSnapshot) {
                        long totalComments = 0;
                        for (DataSnapshot postComentarios : commentsContainerSnapshot.getChildren()) {
                            for (DataSnapshot comment : postComentarios.getChildren()) {
                                if (userId.equals(comment.child("autor").getValue(String.class))) {
                                    totalComments++;
                                }
                            }
                        }
                        callback.onStatsLoaded(totalPosts, totalComments);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onStatsLoaded(totalPosts, 0);
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onStatsLoaded(0, 0); // Graceful fallback
            }
        });
    }
}
