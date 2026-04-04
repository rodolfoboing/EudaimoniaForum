package com.meuprojeto.eudaimoniaforum.forum;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.moderacao.DenunciaActivity;
import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.perfil.VisualizarPerfilActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private static final Map<String, String> userNickCache = new HashMap<>();

    private List<Post> postList;
    private Context context;
    private boolean isModerador;
    private boolean showActions; // Nova flag para controlar exibição de Delete/Report
    private String currentUserId;

    public PostAdapter(Context context, List<Post> postList) {
        this(context, postList, false, false);
    }

    public PostAdapter(Context context, List<Post> postList, boolean isModerador) {
        this(context, postList, isModerador, false);
    }

    public PostAdapter(Context context, List<Post> postList, boolean isModerador, boolean showActions) {
        android.util.Log.d("PostAdapter",
                "PostAdapter inicializado. Itens: " + (postList != null ? postList.size() : 0));
        this.context = context;
        this.postList = postList;
        this.isModerador = isModerador;
        this.showActions = showActions;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.forum_item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.textViewTituloPost.setText(post.getTitulo());
        holder.textViewResumoPost.setText(post.getResumo());

        String categoria = post.getCategoria();
        if (categoria != null && !categoria.isEmpty()) {
            holder.textViewCategoria.setVisibility(View.VISIBLE);
            holder.textViewCategoria.setText(categoria);
        } else {
            holder.textViewCategoria.setVisibility(View.GONE);
        }

        holder.textViewAutor.setOnClickListener(v -> {
            if (post.getAutor() != null) {
                Intent intent = new Intent(context, VisualizarPerfilActivity.class);
                intent.putExtra("USER_ID", post.getAutor());
                context.startActivity(intent);
            }
        });

        int comentarios = post.getNumeroComentarios() != null ? post.getNumeroComentarios() : 0;
        holder.textViewComentarios.setText("Comentários (" + comentarios + ")");

        String autorId = post.getAutor();
        if (autorId != null) {
            if (userNickCache.containsKey(autorId)) {
                holder.textViewAutor.setText("Autor: " + userNickCache.get(autorId));
            } else {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(autorId);
                userRef.child("nick").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String nick = snapshot.getValue(String.class);
                        if (nick != null) {
                            userNickCache.put(autorId, nick);
                            holder.textViewAutor.setText("Autor: " + nick);
                        } else {
                            holder.textViewAutor.setText("Autor: Desconhecido");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        holder.textViewAutor.setText("Autor: Desconhecido");
                    }
                });
            }
        } else {
            holder.textViewAutor.setText("Autor: Desconhecido");
        }

        if (post.getData() > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy",
                    java.util.Locale.getDefault());
            String dataFormatada = sdf.format(new java.util.Date(post.getData()));
            holder.textViewData.setText("Data: " + dataFormatada);
        } else {
            holder.textViewData.setText("Data: Desconhecida");
        }

        holder.buttonAddComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, ComentarioActivity.class);
            intent.putExtra("POST_ID", post.getId());
            intent.putExtra("FOCUS_COMMENT", true);
            context.startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ComentarioActivity.class);
            intent.putExtra("POST_ID", post.getId());
            context.startActivity(intent);
        });

        // Lógica de Menu de Opções condicional à flag showActions
        if (showActions) {
            holder.iconMenuOpcoes.setVisibility(View.VISIBLE);

            holder.iconMenuOpcoes.setOnClickListener(v -> {
                boolean isOwner = currentUserId != null && currentUserId.equals(post.getAutor());

                // Verifica status de notificação antes de abrir o menu
                DatabaseReference seguidorRef = FirebaseDatabase.getInstance().getReference("forum/posts")
                        .child(post.getId()).child("seguidores")
                        .child(currentUserId != null ? currentUserId : "visitante");

                seguidorRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isAcompanhando = false;
                        if (snapshot.exists()) {
                            isAcompanhando = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                        } else {
                            isAcompanhando = isOwner;
                        }

                        PopupMenu popup = new PopupMenu(context, holder.iconMenuOpcoes);
                        popup.getMenuInflater().inflate(R.menu.menu_opcoes_postagem, popup.getMenu());

                        popup.getMenu().findItem(R.id.action_acompanhar)
                                .setTitle(isAcompanhando ? "Silenciar Notificações" : "Acompanhar Notificações");

                        if (isOwner) {
                            popup.getMenu().findItem(R.id.action_denunciar).setVisible(false);
                        }
                        if (!isOwner && !isModerador) {
                            popup.getMenu().findItem(R.id.action_excluir).setVisible(false);
                        }

                        final boolean statusAtual = isAcompanhando;
                        popup.setOnMenuItemClickListener(item -> {
                            int id = item.getItemId();
                            if (id == R.id.action_acompanhar) {
                                boolean novoStatus = !statusAtual;
                                if (currentUserId != null) {
                                    seguidorRef.setValue(novoStatus);
                                    Toast.makeText(context,
                                            novoStatus ? "Notificações ativadas" : "Notificações silenciadas",
                                            Toast.LENGTH_SHORT).show();
                                }
                                return true;
                            } else if (id == R.id.action_denunciar) {
                                android.util.Log.d("PostAdapter", "Iniciando denúncia para o post: " + post.getId());
                                Intent intent = new Intent(context, DenunciaActivity.class);
                                intent.putExtra("POST_ID", post.getId());
                                context.startActivity(intent);
                                return true;
                            } else if (id == R.id.action_excluir) {
                                new AlertDialog.Builder(context)
                                        .setTitle("Excluir Postagem")
                                        .setMessage("Tem certeza que deseja excluir esta postagem?")
                                        .setPositiveButton("Sim", (dialog, which) -> {
                                            String postId = post.getId();
                                            String autorId = post.getAutor();
                                            Map<String, Object> childUpdates = new java.util.HashMap<>();

                                            childUpdates.put("/forum/posts/" + postId, null);
                                            childUpdates.put("/forum/comentarios/" + postId, null);
                                            if (autorId != null) {
                                                childUpdates.put("/users/" + autorId + "/posts/" + postId, null);
                                            }

                                            android.util.Log.d("PostAdapter", "Executando exclusão atômica do post: " + postId);

                                            FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                                                    .addOnSuccessListener(aVoid -> {
                                                        android.util.Log.d("PostAdapter", "Postagem excluída com sucesso: " + postId);
                                                        Toast.makeText(context, "Postagem excluída", Toast.LENGTH_SHORT).show();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        android.util.Log.e("PostAdapter", "Erro ao excluir postagem: " + postId, e);
                                                    });
                                        })
                                        .setNegativeButton("Não", null)
                                        .show();
                                return true;
                            }
                            return false;
                        });
                        popup.show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
            });
        } else {
            holder.iconMenuOpcoes.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTituloPost, textViewResumoPost, textViewComentarios, textViewAutor, textViewData, textViewCategoria;
        Button buttonAddComment;
        ImageView iconMenuOpcoes;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTituloPost = itemView.findViewById(R.id.textViewTituloPost);
            textViewResumoPost = itemView.findViewById(R.id.textViewResumoPost);
            textViewComentarios = itemView.findViewById(R.id.textViewComentarios);
            buttonAddComment = itemView.findViewById(R.id.buttonAddComment);
            textViewAutor = itemView.findViewById(R.id.textViewAutor);
            textViewData = itemView.findViewById(R.id.textViewData);
            textViewCategoria = itemView.findViewById(R.id.textViewCategoria);
            iconMenuOpcoes = itemView.findViewById(R.id.iconMenuOpcoes);
        }
    }
}