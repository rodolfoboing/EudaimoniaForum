package com.meuprojeto.eudaimoniaforum.moderation;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.profile.User;
import com.meuprojeto.eudaimoniaforum.utils.AppLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModerationManager {

    private final DatabaseReference rootRef;
    private final String currentUserId;
    private ValueEventListener denunciasListener;
    private ValueEventListener banidosListener;
    private ValueEventListener usuariosListener;

    public interface FormDenunciaCallback {
        void onSuccess();
        void onSpamDetectado();
        void onError(String erro);
    }

    public interface DenunciasFeedCallback {
        void onDenunciasLoaded(List<Report> reports);
    }

    public interface UsuariosFeedCallback {
        void onUsuariosLoaded(List<User> listNaoBanidos, Set<String> listBannedIds);
    }

    public interface AcaoCallback {
        void onSuccess();
        void onError(String erro);
    }

    public ModerationManager() {
        this.rootRef = FirebaseDatabase.getInstance().getReference();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null 
                             ? FirebaseAuth.getInstance().getCurrentUser().getUid() 
                             : null;
    }

    public void enviarDenuncia(String postId, String comentarioId, String tipo, String motivoFinal, FormDenunciaCallback callback) {
        if (currentUserId == null) {
            callback.onError("Não autenticado.");
            return;
        }

        DatabaseReference denunciasRef = rootRef.child("denuncias");
        denunciasRef.orderByChild("postId").equalTo(postId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean jaDenunciou = false;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Report d = ds.getValue(Report.class);
                    if (d != null && currentUserId.equals(d.getDenuncianteId())) {
                        boolean isComentario = "comentario".equals(tipo) || "comment".equals(tipo);
                        boolean dIsComentario = "comentario".equals(d.getTipo()) || "comment".equals(d.getTipo());
                        if (isComentario && dIsComentario && comentarioId != null && comentarioId.equals(d.getComentarioId())) {
                            jaDenunciou = true;
                            break;
                        } else if (("post".equals(tipo) || tipo == null) && ("post".equals(d.getTipo()) || d.getTipo() == null)) {
                            jaDenunciou = true;
                            break;
                        }
                    }
                }

                if (jaDenunciou) {
                    callback.onSpamDetectado();
                } else {
                    String id = denunciasRef.push().getKey();
                    if (id == null) {
                        callback.onError("Erro ao criar identificador da denúncia");
                        return;
                    }

                    // Buscar o conteúdo denunciado para dar contexto ao moderador
                    DatabaseReference conteudoRef;
                    if (("comentario".equals(tipo) || "comment".equals(tipo)) && comentarioId != null) {
                        conteudoRef = rootRef.child("forum/comentarios").child(postId).child(comentarioId).child("conteudo");
                    } else {
                        conteudoRef = rootRef.child("forum/posts").child(postId).child("resumo");
                    }

                    conteudoRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot conteudoSnap) {
                            String previa = conteudoSnap.getValue(String.class);
                            Report report = new Report(id, postId, comentarioId, tipo, motivoFinal, currentUserId, System.currentTimeMillis());
                            report.setConteudoDenunciado(previa != null ? previa : "Conteúdo indisponível");
                            denunciasRef.child(id).setValue(report).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    AppLogger.logModAlert("Denuncia_Enviada", "Usuário UID " + currentUserId + " reportou um [" + tipo + "] com ID " + postId + "/" + comentarioId + " Motivo: " + motivoFinal);
                                    callback.onSuccess();
                                } else {
                                    callback.onError("Erro ao registrar denúncia");
                                    if (task.getException() != null) AppLogger.logDbError("Moderacao_EnviarDenuncia", task.getException().getMessage());
                                }
                            });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            callback.onError("Erro ao buscar conteúdo: " + error.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void carregarDenunciasPendentes(DenunciasFeedCallback callback) {
        DatabaseReference denunciasRef = rootRef.child("denuncias");
        denunciasListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Report> reportList = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Report d = ds.getValue(Report.class);
                    if (d != null) reportList.add(d);
                }
                callback.onDenunciasLoaded(reportList);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        denunciasRef.orderByChild("status").equalTo("pendente").addValueEventListener(denunciasListener);
    }

    public void carregarUsuariosAtivos(UsuariosFeedCallback callback) {
        // Como o carregamento e cruzamento dessas duas listas pode sofrer problemas se um deles carregar e o outro não,
        // gerenciaremos isso criando uma versão segura: carregue banidos, e depois carregue os usuários e filtre.
        rootRef.child("banidos").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot banidosSnapshot) {
                Set<String> bIds = new HashSet<>();
                for (DataSnapshot ds : banidosSnapshot.getChildren()) {
                    bIds.add(ds.getKey());
                }
                
                // Em seguida escuta os usuários cruzando com essa lista
                rootRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot usersSnapshot) {
                        List<User> usuariosAtivos = new ArrayList<>();
                        for (DataSnapshot uDs : usersSnapshot.getChildren()) {
                            // Extraimos de forma segura em caso de models corrompidos
                            if(uDs.hasChild("nick")) {
                                Object oNick = uDs.child("nick").getValue();
                                String nick = oNick != null ? oNick.toString() : "";
                                String uid = uDs.getKey();
                                
                                if(!bIds.contains(uid)) {
                                    User u = new User();
                                    u.setUid(uid);
                                    u.setNick(nick);
                                    usuariosAtivos.add(u);
                                }
                            }
                        }
                        callback.onUsuariosLoaded(usuariosAtivos, bIds);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public void resolverDenuncia(String denunciaId, AcaoCallback callback) {
        rootRef.child("denuncias").child(denunciaId).child("status").setValue("resolvido")
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void apagarConteudoOfensivoEResolver(Report report, AcaoCallback callback) {
        boolean isComentario = "comentario".equals(report.getTipo()) || "comment".equals(report.getTipo());
        if (isComentario && report.getComentarioId() != null) {
            DatabaseReference targetRef = rootRef.child("forum/comentarios").child(report.getPostId()).child(report.getComentarioId());
            targetRef.removeValue().addOnSuccessListener(aVoid -> {
                // Ao apagar um comentário ofensivo, diminuir o contador de comentários do post
                rootRef.child("forum/posts").child(report.getPostId()).child("numeroComentarios")
                        .setValue(com.google.firebase.database.ServerValue.increment(-1));
                resolverDenuncia(report.getId(), callback);
            }).addOnFailureListener(e -> callback.onError(e.getMessage()));
        } else {
            DatabaseReference targetRef = rootRef.child("forum/posts").child(report.getPostId());
            targetRef.removeValue().addOnSuccessListener(aVoid -> {
                // Ao apagar um post inteiro, limpar também a arvore de comentários dele para evitar orfãos
                rootRef.child("forum/comentarios").child(report.getPostId()).removeValue();
                resolverDenuncia(report.getId(), callback);
            }).addOnFailureListener(e -> callback.onError(e.getMessage()));
        }
    }

    public void banirUsuario(String uid, AcaoCallback callback) {
        rootRef.child("banidos").child(uid).setValue(true)
            .addOnSuccessListener(aVoid -> {
                AppLogger.logModAlert("Banimento", "Usuário UID: " + uid + " recebeu suspensão.");
                callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                callback.onError(e.getMessage());
                AppLogger.logDbError("Moderacao_Banir", e.getMessage());
            });
    }

    public void destruir() {
        if(denunciasListener != null) {
            rootRef.child("denuncias").removeEventListener(denunciasListener);
        }
        // Nota: as otimizações de listeners de banidos e usuários deveriam possuir tracking individual para limpeza 
        // caso esse componente vá escutar em ValueEvent em tempo real. Pelo fluxo eles rodam e as activities sobrevivem.
    }
}
