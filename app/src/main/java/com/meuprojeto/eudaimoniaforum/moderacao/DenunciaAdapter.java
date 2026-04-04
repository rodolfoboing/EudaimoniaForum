package com.meuprojeto.eudaimoniaforum.moderacao;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.forum.ComentarioActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DenunciaAdapter extends RecyclerView.Adapter<DenunciaAdapter.DenunciaViewHolder> {

    private List<Denuncia> denunciaList;
    private Context context;
    private DenunciaAction callback;

    public interface DenunciaAction {
        void onResolverClicado(Denuncia denuncia);
        void onApagarPostClicado(Denuncia denuncia);
    }

    public DenunciaAdapter(List<Denuncia> denunciaList, Context context, DenunciaAction callback) {
        this.denunciaList = denunciaList;
        this.context = context;
        this.callback = callback;
    }

    @NonNull
    @Override
    public DenunciaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.moderacao_denuncia_item, parent, false);
        return new DenunciaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DenunciaViewHolder holder, int position) {
        Denuncia denuncia = denunciaList.get(position);

        holder.textMotivo.setText("Motivo: " + denuncia.getMotivo());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.textData.setText(sdf.format(new Date(denuncia.getTimestamp())));
        if ("comentario".equals(denuncia.getTipo()) && denuncia.getComentarioId() != null) {
            holder.textPostId.setText("Comentário ID: " + denuncia.getComentarioId() + "\n(Post: " + denuncia.getPostId() + ")");
        } else {
            holder.textPostId.setText("Post ID: " + denuncia.getPostId());
        }

        holder.btnVerPostBotao.setOnClickListener(v -> {
            Intent intent = new Intent(context, ComentarioActivity.class);
            intent.putExtra("POST_ID", denuncia.getPostId());
            context.startActivity(intent);
        });

        holder.btnResolver.setOnClickListener(v -> {
            if (callback != null) {
                callback.onResolverClicado(denuncia);
            }
        });

        holder.btnApagarPost.setOnClickListener(v -> {
            if (callback != null) {
                callback.onApagarPostClicado(denuncia);
            }
        });
    }

    @Override
    public int getItemCount() {
        return denunciaList.size();
    }

    static class DenunciaViewHolder extends RecyclerView.ViewHolder {
        TextView textMotivo, textData, textPostId;
        Button btnVerPostBotao, btnResolver, btnApagarPost;

        public DenunciaViewHolder(@NonNull View itemView) {
            super(itemView);
            textMotivo = itemView.findViewById(R.id.textMotivo);
            textData = itemView.findViewById(R.id.textData);
            textPostId = itemView.findViewById(R.id.textPostId);
            btnVerPostBotao = itemView.findViewById(R.id.btnVerPostBotao);
            btnResolver = itemView.findViewById(R.id.btnResolver);
            btnApagarPost = itemView.findViewById(R.id.btnApagarPost);
        }
    }
}
