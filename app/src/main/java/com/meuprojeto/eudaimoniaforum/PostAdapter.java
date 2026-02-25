package com.meuprojeto.eudaimoniaforum;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    // Cache para armazenar nicks de usuários e evitar requisições repetidas
    private static final Map<String, String> userNickCache = new HashMap<>();

    private List<Post> postList;
    private Context context;
    private boolean isModerador;
    private String currentUserId;

    public PostAdapter(Context context, List<Post> postList) {
        this.context = context;
        this.postList = postList;
        this.isModerador = false; // Valor padrão
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    // Construtor sobrecarregado para lidar com o status de moderador
    public PostAdapter(Context context, List<Post> postList, boolean isModerador) {
        this.context = context;
        this.postList = postList;
        this.isModerador = isModerador;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.textViewTituloPost.setText(post.getTitulo());
        holder.textViewResumoPost.setText(post.getResumo());

        // Configura o clique no nome do autor para abrir o perfil
        holder.textViewAutor.setOnClickListener(v -> {
            if (post.getAutor() != null) {
                Intent intent = new Intent(context, VisualizarPerfilActivity.class);
                intent.putExtra("USER_ID", post.getAutor());
                context.startActivity(intent);
            }
        });

        // Exibe o número de comentários diretamente do objeto Post
        int comentarios = post.getNumeroComentarios() != null ? post.getNumeroComentarios() : 0;
        holder.textViewComentarios.setText("Comentários (" + comentarios + ")");

        // Busca o nick do autor com cache simples
        String autorId = post.getAutor();
        if (autorId != null) {
            if (userNickCache.containsKey(autorId)) {
                holder.textViewAutor.setText("Autor: " + userNickCache.get(autorId));
            } else {
                // Define um valor temporário ou loading se desejar, ou mantém "Autor: ..."
                // holder.textViewAutor.setText("Autor: Carregando...");

                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(autorId);
                userRef.child("nick").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String nick = snapshot.getValue(String.class);
                        if (nick != null) {
                            userNickCache.put(autorId, nick);
                            // Verifica se o holder ainda corresponde ao mesmo autor (item não foi reciclado
                            // para outro post de outro autor)
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

        // Formatar a data para exibição
        if (post.getData() > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy",
                    java.util.Locale.getDefault());
            String dataFormatada = sdf.format(new java.util.Date(post.getData()));
            holder.textViewData.setText("Data: " + dataFormatada);
        } else {
            holder.textViewData.setText("Data: Desconhecida");
        }

        // Ação do botão de comentar (Foca na caixa de texto)
        holder.buttonAddComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, ComentarioActivity.class);
            intent.putExtra("POST_ID", post.getId());
            intent.putExtra("FOCUS_COMMENT", true); // Flag para focar no input
            context.startActivity(intent);
        });

        // Clique no card (Apenas abre o post)
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ComentarioActivity.class);
            intent.putExtra("POST_ID", post.getId());
            context.startActivity(intent);
        });

        // Lógica de Denúncia
        holder.iconReport.setOnClickListener(v -> {
            Intent intent = new Intent(context, DenunciaActivity.class);
            intent.putExtra("POST_ID", post.getId());
            context.startActivity(intent);
        });

        // Visibilidade e ação do botão de deletar (Moderador ou Dono)
        boolean isOwner = currentUserId != null && currentUserId.equals(post.getAutor());

        // Esconder botão de denúncia se for o dono
        if (isOwner) {
            holder.iconReport.setVisibility(View.GONE);
        } else {
            holder.iconReport.setVisibility(View.VISIBLE);
        }

        if (isOwner || isModerador) {
            holder.imageViewDeletePost.setVisibility(View.VISIBLE);
            holder.imageViewDeletePost.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                        .setTitle("Excluir Postagem")
                        .setMessage("Tem certeza que deseja excluir esta postagem?")
                        .setPositiveButton("Sim", (dialog, which) -> {
                            // Criar referência para o post no Firebase e remover
                            DatabaseReference postRef = FirebaseDatabase.getInstance()
                                    .getReference("forum/posts") // Caminho corrigido para forum/posts
                                    .child(post.getId());
                            postRef.removeValue();
                        })
                        .setNegativeButton("Não", null)
                        .show();
            });
        } else {
            holder.imageViewDeletePost.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTituloPost, textViewResumoPost, textViewComentarios, buttonAddComment, textViewAutor,
                textViewData;
        ImageView imageViewDeletePost, iconReport;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTituloPost = itemView.findViewById(R.id.textViewTituloPost);
            textViewResumoPost = itemView.findViewById(R.id.textViewResumoPost);
            textViewComentarios = itemView.findViewById(R.id.textViewComentarios);
            buttonAddComment = itemView.findViewById(R.id.buttonAddComment);
            textViewAutor = itemView.findViewById(R.id.textViewAutor);
            textViewData = itemView.findViewById(R.id.textViewData);
            imageViewDeletePost = itemView.findViewById(R.id.imageViewDeletePost);
            iconReport = itemView.findViewById(R.id.iconReport);
        }
    }
}