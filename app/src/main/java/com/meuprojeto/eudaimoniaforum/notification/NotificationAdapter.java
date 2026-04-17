package com.meuprojeto.eudaimoniaforum.notification;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meuprojeto.eudaimoniaforum.R;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificacaoViewHolder> {

    private List<Notification> notificacoes;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notificacoes, OnItemClickListener listener) {
        this.notificacoes = notificacoes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificacaoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_item, parent, false);
        return new NotificacaoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificacaoViewHolder holder, int position) {
        Notification notification = notificacoes.get(position);
        holder.bind(notification, listener);
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

        public void bind(final Notification notification, final OnItemClickListener listener) {
            textNotificationMessage.setText(notification.getMensagem());

            // Formata o timestamp para algo legível como "5 minutos atrás"
            CharSequence tempoAtras = DateUtils.getRelativeTimeSpanString(
                    notification.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            textNotificationTime.setText(tempoAtras);

            // Muda o texto do botão com base no tipo
            if ("chat".equals(notification.getTipo())) {
                buttonViewPost.setText("Ver Chat");
            } else {
                buttonViewPost.setText("Ver Post");
            }

            if (notification.isLida()) {
                rootLayoutNotificacao.setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5")); // Cinza
                                                                                                        // clarinho p/
                                                                                                        // Lida
            } else {
                rootLayoutNotificacao.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD")); // Azul
                                                                                                        // bebezinho p/
                                                                                                        // Não Lida
            }

            buttonViewPost.setOnClickListener(v -> listener.onItemClick(notification));
        }
    }
}
