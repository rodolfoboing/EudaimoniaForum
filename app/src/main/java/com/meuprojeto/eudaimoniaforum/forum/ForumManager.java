package com.meuprojeto.eudaimoniaforum.forum;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.meuprojeto.eudaimoniaforum.utils.AppLogger;

public class ForumManager {
    private static final String TAG = "ForumManager";

    private final FirebaseAuth mAuth;
    private final DatabaseReference rootRef;

    public interface FeedCallback {
        void onPostsCarregados(List<Post> posts);
        void onError(String erro);
    }

    public interface ModeradorCallback {
        void onCheckComplete(boolean isModerator);
    }

    public interface PostCallback {
        void onSuccess();
        void onWaitDelay(long secondsRemaining);
        void onError(String erro);
    }

    public ForumManager() {
        mAuth = FirebaseAuth.getInstance();
        rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.child("forum").keepSynced(true);
    }

    public String getCurrentUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
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

    public ValueEventListener carregarFeed(String categoriaSelecionada, Query[] activeQuery, FeedCallback callback) {
        DatabaseReference postsRef = rootRef.child("forum/posts");
        Query postsQuery;

        if (categoriaSelecionada == null || categoriaSelecionada.equals("Todos")) {
            postsQuery = postsRef.limitToLast(100);
        } else {
            postsQuery = postsRef.orderByChild("categoria").equalTo(categoriaSelecionada).limitToLast(100);
        }
        
        activeQuery[0] = postsQuery;

        ValueEventListener postsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Post> postList = new ArrayList<>();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    Post post = postSnapshot.getValue(Post.class);
                    if (post != null) {
                        post.setId(postSnapshot.getKey());
                        postList.add(post);
                    }
                }
                Collections.reverse(postList);
                callback.onPostsCarregados(postList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onError(databaseError.getMessage());
            }
        };
        
        postsQuery.addValueEventListener(postsListener);
        return postsListener;
    }

    public void publicarPost(String titulo, String resumo, String categoria, PostCallback callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onError("Usuário não autenticado");
            return;
        }

        DatabaseReference userLastPostRef = rootRef.child("users").child(uid).child("lastPostTimestamp");
        userLastPostRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long lastPostTimestamp = snapshot.getValue(Long.class);
                long currentTime = System.currentTimeMillis();
                long cooldownMillis = 60000;

                if (lastPostTimestamp != null && (currentTime - lastPostTimestamp) < cooldownMillis) {
                    long segundosRestantes = (cooldownMillis - (currentTime - lastPostTimestamp)) / 1000;
                    callback.onWaitDelay(segundosRestantes);
                    AppLogger.logSpam(uid, "Forum");
                } else {
                    executarAtomicWritePost(uid, titulo, resumo, categoria, currentTime, callback);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError("Erro ao verificar status anti-spam: " + error.getMessage());
            }
        });
    }

    private void executarAtomicWritePost(String autor, String titulo, String resumo, String categoria, long data, PostCallback callback) {
        String postId = rootRef.child("forum/posts").push().getKey();
        if (postId == null) {
            callback.onError("Erro ao gerar ID do post");
            return;
        }

        Post post = new Post(postId, titulo, resumo, 0, autor, data, categoria);
        Map<String, Object> childUpdates = new HashMap<>();

        childUpdates.put("/forum/posts/" + postId, post);
        childUpdates.put("/users/" + autor + "/posts/" + postId, true);
        childUpdates.put("/users/" + autor + "/lastPostTimestamp", data);
        childUpdates.put("/users/" + autor + "/totalPosts", com.google.firebase.database.ServerValue.increment(1));

        rootRef.updateChildren(childUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onSuccess();
            } else {
                String errorMsg = task.getException() != null ? task.getException().getMessage() : "Erro desconhecido";
                callback.onError(errorMsg);
                AppLogger.logDbError("Forum_PublicarPost", errorMsg);
            }
        });
    }

    public void carregarMinhasPostagens(String targetUserId, FeedCallback callback) {
        rootRef.child("forum/posts").orderByChild("autor").equalTo(targetUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<Post> postList = new ArrayList<>();
                        for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                            Post post = postSnapshot.getValue(Post.class);
                            if (post != null) {
                                post.setId(postSnapshot.getKey());
                                postList.add(post);
                            }
                        }
                        Collections.sort(postList, (p1, p2) -> Long.compare(p2.getData(), p1.getData()));
                        callback.onPostsCarregados(postList);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void carregarPostagensComentadas(String targetUserId, FeedCallback callback) {
        rootRef.child("users").child(targetUserId).child("postsComentados")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists() && snapshot.hasChildren()) {
                            List<String> postsIds = new ArrayList<>();
                            for (DataSnapshot idSnapshot : snapshot.getChildren()) {
                                postsIds.add(idSnapshot.getKey());
                            }
                            buscarDetalhesPostsComentados(targetUserId, postsIds, callback);
                        } else {
                            callback.onPostsCarregados(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    private void buscarDetalhesPostsComentados(String targetUserId, List<String> postsIds, FeedCallback callback) {
        if (postsIds.isEmpty()) {
            callback.onPostsCarregados(new ArrayList<>());
            return;
        }

        List<Post> postList = new ArrayList<>();
        final int[] loadedCount = {0};
        final int totalPosts = postsIds.size();

        for (String postId : postsIds) {
            rootRef.child("forum/comentarios").child(postId)
                    .orderByChild("autor").equalTo(targetUserId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot comentarioSnapshot) {
                            if (comentarioSnapshot.exists()) {
                                rootRef.child("forum/posts").child(postId).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        Post post = snapshot.getValue(Post.class);
                                        if (post != null) {
                                            post.setId(snapshot.getKey());
                                            postList.add(post);
                                        }
                                        checarFim(++loadedCount[0], totalPosts, postList, callback);
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        checarFim(++loadedCount[0], totalPosts, postList, callback);
                                    }
                                });
                            } else {
                                // Limpeza de sujeira: Usuário não possui mais comentários aqui
                                rootRef.child("users").child(targetUserId).child("postsComentados").child(postId).removeValue();
                                checarFim(++loadedCount[0], totalPosts, postList, callback);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            checarFim(++loadedCount[0], totalPosts, postList, callback);
                        }
                    });
        }
    }

    private void checarFim(int atuais, int total, List<Post> postList, FeedCallback callback) {
        if (atuais >= total) {
            Collections.sort(postList, (p1, p2) -> Long.compare(p2.getData(), p1.getData()));
            callback.onPostsCarregados(postList);
        }
    }
}
