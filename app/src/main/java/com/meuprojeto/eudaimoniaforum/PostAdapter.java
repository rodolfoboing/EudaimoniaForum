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

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList; // Lista de posts
    private Context context; // Contexto da aplicação
    private boolean isModerador;
    private String currentUserId;

    // Construtor do adapter
    public PostAdapter(List<Post> postList, Context context, boolean isModerador) {
        this.postList = postList;
        this.context = context;
        this.isModerador = isModerador;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    // Método para atualizar a lista de posts
    public void updatePosts(List<Post> newPostList) {
        this.postList = newPostList;
        notifyDataSetChanged(); // Notificar o adapter sobre a mudança
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflar o layout do item do post
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        // Obter o post atual
        Post post = postList.get(position);

        // Exibir título e resumo do post
        holder.textViewTituloPost.setText(post.getTitulo());
        holder.textViewResumoPost.setText(post.getResumo());

        // Configurar o nome do autor como clicável
        holder.textViewAutor.setOnClickListener(v -> {
            if (post.getAutor() != null) {
                Intent intent = new Intent(context, VisualizarPerfilActivity.class);
                intent.putExtra("USER_ID", post.getAutor()); // Passar o ID do autor
                context.startActivity(intent);
            }
        });

        // Atualizar o número de comentários em tempo real
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("forum/posts").child(post.getId());
        postRef.child("numeroComentarios").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long numeroComentarios = snapshot.getValue(Long.class);
                if (numeroComentarios != null) {
                    holder.textViewComentarios.setText("Comentários (" + numeroComentarios + ")");
                } else {
                    holder.textViewComentarios.setText("Comentários (0)");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("PostAdapter", "Erro ao carregar número de comentários: " + error.getMessage());
                holder.textViewComentarios.setText("Comentários (erro)");
            }
        });

        // Buscar o nickname do autor diretamente como String
        DatabaseReference usuariosRef = FirebaseDatabase.getInstance().getReference("users").child(post.getAutor());
        usuariosRef.child("nick").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nick = snapshot.getValue(String.class);
                if (nick != null && !nick.isEmpty()) {
                    holder.textViewAutor.setText("Autor: " + nick);
                } else {
                    holder.textViewAutor.setText("Autor: desconhecido");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                holder.textViewAutor.setText("Autor: erro ao carregar");
                Log.e("PostAdapter", "Erro ao buscar o autor: " + error.getMessage());
            }
        });

        // Exibir a data do post
        holder.textViewData.setText("Data: " + post.getData());

        // Configurar clique no botão "Adicionar Comentário"
        holder.buttonAddComment.setOnClickListener(v -> {
            if (post.getId() != null) {
                Intent intent = new Intent(v.getContext(), ComentarioActivity.class);
                intent.putExtra("POST_ID", post.getId());
                v.getContext().startActivity(intent);
            } else {
                Log.e("PostAdapter", "Post ID is null");
            }
        });

        // Lógica de exclusão
        boolean isDonoPost = (post.getAutor() != null && currentUserId != null && post.getAutor().equals(currentUserId));

        if (isModerador || isDonoPost) {
            holder.imageViewDeletePost.setVisibility(View.VISIBLE);
            holder.imageViewDeletePost.setOnClickListener(v -> {
                new AlertDialog.Builder(context)
                    .setTitle("Excluir Postagem")
                    .setMessage("Tem certeza que deseja excluir esta postagem?")
                    .setPositiveButton("Sim", (dialog, which) -> {
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("forum/posts").child(post.getId());
                        ref.removeValue();
                        // A activity deve atualizar a lista automaticamente
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
        return postList.size(); // Retorna o número de posts na lista
    }

    // Classe interna para o ViewHolder
    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTituloPost; // Título do post
        TextView textViewResumoPost; // Resumo do post
        TextView textViewComentarios; // Número de comentários
        TextView buttonAddComment; // Botão para adicionar comentário
        TextView textViewAutor; // Autor do post
        TextView textViewData; // Data do post
        ImageView imageViewDeletePost; // Botão de excluir

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            // Inicializar os elementos do layout
            textViewTituloPost = itemView.findViewById(R.id.textViewTituloPost);
            textViewResumoPost = itemView.findViewById(R.id.textViewResumoPost);
            textViewComentarios = itemView.findViewById(R.id.textViewComentarios);
            buttonAddComment = itemView.findViewById(R.id.buttonAddComment);
            textViewAutor = itemView.findViewById(R.id.textViewAutor);
            textViewData = itemView.findViewById(R.id.textViewData);
            imageViewDeletePost = itemView.findViewById(R.id.imageViewDeletePost);
        }
    }

}
