package com.meuprojeto.eudaimoniaforum.moderation;

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
import com.meuprojeto.eudaimoniaforum.forum.CommentActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.DenunciaViewHolder> {

    private List<Report> reportList;
    private Context context;
    private DenunciaAction callback;

    public interface DenunciaAction {
        void onResolverClicado(Report report);
        void onApagarPostClicado(Report report);
    }

    public ReportAdapter(List<Report> reportList, Context context, DenunciaAction callback) {
        this.reportList = reportList;
        this.context = context;
        this.callback = callback;
    }

    @NonNull
    @Override
    public DenunciaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.moderation_report_item, parent, false);
        return new DenunciaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DenunciaViewHolder holder, int position) {
        Report report = reportList.get(position);

        holder.textMotivo.setText("Motivo: " + report.getMotivo());

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.textData.setText(sdf.format(new Date(report.getTimestamp())));
        if ("comentario".equals(report.getTipo()) && report.getComentarioId() != null) {
            holder.textPostId.setText("Comentário ID: " + report.getComentarioId() + "\n(Post: " + report.getPostId() + ")");
        } else {
            holder.textPostId.setText("Post ID: " + report.getPostId());
        }

        // Exibir prévia do conteúdo denunciado
        String conteudo = report.getConteudoDenunciado();
        if (conteudo != null && !conteudo.isEmpty()) {
            holder.textConteudoDenunciado.setText("\"" + conteudo + "\"");
            holder.textConteudoDenunciado.setVisibility(View.VISIBLE);
        } else {
            holder.textConteudoDenunciado.setVisibility(View.GONE);
        }

        // Texto dinâmico do botão de apagar
        boolean isComentario = "comentario".equals(report.getTipo()) || "comment".equals(report.getTipo());
        if (isComentario) {
            holder.btnApagarConteudo.setText("Apagar Comentário");
        } else {
            holder.btnApagarConteudo.setText("Apagar Post");
        }

        holder.btnVerPostBotao.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentActivity.class);
            intent.putExtra("POST_ID", report.getPostId());
            context.startActivity(intent);
        });

        holder.btnResolver.setOnClickListener(v -> {
            if (callback != null) {
                callback.onResolverClicado(report);
            }
        });

        holder.btnApagarConteudo.setOnClickListener(v -> {
            if (callback != null) {
                callback.onApagarPostClicado(report);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    static class DenunciaViewHolder extends RecyclerView.ViewHolder {
        TextView textMotivo, textData, textPostId, textConteudoDenunciado;
        Button btnVerPostBotao, btnResolver, btnApagarConteudo;

        public DenunciaViewHolder(@NonNull View itemView) {
            super(itemView);
            textMotivo = itemView.findViewById(R.id.textMotivo);
            textData = itemView.findViewById(R.id.textData);
            textPostId = itemView.findViewById(R.id.textPostId);
            textConteudoDenunciado = itemView.findViewById(R.id.textConteudoDenunciado);
            btnVerPostBotao = itemView.findViewById(R.id.btnVerPostBotao);
            btnResolver = itemView.findViewById(R.id.btnResolver);
            btnApagarConteudo = itemView.findViewById(R.id.btnApagarConteudo);
        }
    }
}
