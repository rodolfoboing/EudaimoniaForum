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
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class ComentarioAdapter extends RecyclerView.Adapter<ComentarioAdapter.ComentarioViewHolder> {

    private List<Comentario> comentarioList;
    private Context context;
    private boolean isModerador;
    private String donoDoPostId;
    private String currentUserId;

    public ComentarioAdapter(List<Comentario> comentarioList, boolean isModerador, Context context, String donoDoPostId) {
        this.comentarioList = comentarioList;
        this.isModerador = isModerador;
        this.context = context;
        this.donoDoPostId = donoDoPostId;
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    @NonNull
    @Override
    public ComentarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comentario, parent, false);
        return new ComentarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ComentarioViewHolder holder, int position) {
        Comentario comentario = comentarioList.get(position);

        holder.textViewAutor.setOnClickListener(v -> {
            if (comentario.getAutor() != null) {
                Intent intent = new Intent(context, VisualizarPerfilActivity.class);
                intent.putExtra("USER_ID", comentario.getAutor());
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        });

        holder.textViewAutor.setText("Carregando...");
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(comentario.getAutor());

        userRef.child("nick").get().addOnSuccessListener(snapshot -> {
            String nick = snapshot.getValue(String.class);
            holder.textViewAutor.setText(nick != null ? nick : "Usuário desconhecido");
        }).addOnFailureListener(e -> {
            holder.textViewAutor.setText("Erro ao carregar usuário");
            Log.e("ComentarioAdapter", "Erro ao carregar nickname: " + e.getMessage());
        });

        holder.textViewConteudo.setText(comentario.getConteudo());
        holder.textViewData.setText(comentario.getData());

        boolean isDonoPost = (donoDoPostId != null && currentUserId != null && donoDoPostId.equals(currentUserId));
        boolean isAutorComentario = (comentario.getAutor() != null && currentUserId != null && comentario.getAutor().equals(currentUserId));

        holder.btnOpcoes.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(context, holder.btnOpcoes);
            
            boolean podeExcluir = isModerador || isDonoPost || isAutorComentario;
            boolean podeDenunciar = !isAutorComentario;

            if (podeExcluir) {
                popup.getMenu().add(0, 1, 0, "Apagar Comentário");
            }
            if (podeDenunciar) {
                popup.getMenu().add(0, 2, 0, "Denunciar");
            }

            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    new androidx.appcompat.app.AlertDialog.Builder(context)
                            .setTitle("Excluir Comentário")
                            .setMessage("Tem certeza que deseja apagar este comentário?")
                            .setPositiveButton("Sim", (dialog, which) -> {
                                DatabaseReference comentarioRef = FirebaseDatabase.getInstance()
                                        .getReference("forum/comentarios")
                                        .child(comentario.getPostId())
                                        .child(comentario.getId());
                                
                                android.util.Log.d("ComentarioAdapter", "Excluindo comentário: " + comentario.getId() + " do post: " + comentario.getPostId());
                                
                                comentarioRef.removeValue();
                                comentarioList.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, comentarioList.size());
                                
                                decrementarNumeroComentarios(comentario.getPostId());
                            })
                            .setNegativeButton("Não", null)
                            .show();
                    return true;
                } else if (item.getItemId() == 2) {
                    android.util.Log.d("ComentarioAdapter", "Iniciando denúncia para comentário: " + comentario.getId() + " do post: " + comentario.getPostId());
                    Intent intent = new Intent(context, DenunciaActivity.class);
                    intent.putExtra("POST_ID", comentario.getPostId());
                    intent.putExtra("COMENTARIO_ID", comentario.getId());
                    intent.putExtra("TIPO", "comentario");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }
    
    private void decrementarNumeroComentarios(String postId) {
         DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("forum/posts").child(postId);
         postRef.child("numeroComentarios").get().addOnSuccessListener(snapshot -> {
             Long qtd = snapshot.getValue(Long.class);
             if (qtd != null && qtd > 0) {
                 postRef.child("numeroComentarios").setValue(qtd - 1);
             }
         });
    }

    @Override
    public int getItemCount() {
        return comentarioList.size();
    }

    static class ComentarioViewHolder extends RecyclerView.ViewHolder {
        TextView textViewAutor, textViewConteudo, textViewData;
        ImageView btnOpcoes;

        public ComentarioViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewAutor = itemView.findViewById(R.id.textViewAutorComentario);
            textViewConteudo = itemView.findViewById(R.id.textViewConteudoComentario);
            textViewData = itemView.findViewById(R.id.textViewDataComentario);
            btnOpcoes = itemView.findViewById(R.id.btnOpcoesComentario);
        }
    }
}
