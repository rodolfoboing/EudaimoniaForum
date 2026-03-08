package com.meuprojeto.eudaimoniaforum;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificacaoAdapter extends RecyclerView.Adapter<NotificacaoAdapter.NotificacaoViewHolder> {

    private List<Notificacao> notificacoes;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Notificacao notificacao);
    }

    public NotificacaoAdapter(List<Notificacao> notificacoes, OnItemClickListener listener) {
        this.notificacoes = notificacoes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificacaoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notificacao, parent, false);
        return new NotificacaoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificacaoViewHolder holder, int position) {
        Notificacao notificacao = notificacoes.get(position);
        holder.bind(notificacao, listener);
    }

    @Override
    public int getItemCount() {
        return notificacoes.size();
    }

    static class NotificacaoViewHolder extends RecyclerView.ViewHolder {
        TextView textNotificationMessage;
        TextView textNotificationTime;
        Button buttonViewPost;
        LinearLayout rootLayoutNotificacao;

        public NotificacaoViewHolder(@NonNull View itemView) {
            super(itemView);
            textNotificationMessage = itemView.findViewById(R.id.textNotificationMessage);
            textNotificationTime = itemView.findViewById(R.id.textNotificationTime);
            buttonViewPost = itemView.findViewById(R.id.buttonViewPost);
            rootLayoutNotificacao = itemView.findViewById(R.id.rootLayoutNotificacao);
        }

        public void bind(final Notificacao notificacao, final OnItemClickListener listener) {
            textNotificationMessage.setText(notificacao.getMensagem());

            // Formata o timestamp para algo legível como "5 minutos atrás"
            CharSequence tempoAtras = DateUtils.getRelativeTimeSpanString(
                    notificacao.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            textNotificationTime.setText(tempoAtras);

            // Muda o texto do botão com base no tipo
            if ("chat".equals(notificacao.getTipo())) {
                buttonViewPost.setText("Ver Chat");
            } else {
                buttonViewPost.setText("Ver Post");
            }

            if (notificacao.isLida()) {
                rootLayoutNotificacao.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5")); // Cinza
                                                                                                        // clarinho p/
                                                                                                        // Lida
            } else {
                rootLayoutNotificacao.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD")); // Azul
                                                                                                        // bebezinho p/
                                                                                                        // Não Lida
            }

            buttonViewPost.setOnClickListener(v -> listener.onItemClick(notificacao));
        }
    }
}
