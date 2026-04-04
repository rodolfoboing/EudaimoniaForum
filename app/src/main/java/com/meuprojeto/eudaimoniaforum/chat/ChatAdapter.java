package com.meuprojeto.eudaimoniaforum.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.meuprojeto.eudaimoniaforum.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<ChatMessage> chatMessages;
    private String currentUserId;

    public ChatAdapter(List<ChatMessage> chatMessages) {
        this.chatMessages = chatMessages;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = chatMessages.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_item_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage chatMessage = chatMessages.get(position);
        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).bind(chatMessage);
        } else {
            ((ReceivedMessageViewHolder) holder).bind(chatMessage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(timestamp);
    }

    // ViewHolder para mensagens ENVIADAS
    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        ImageView messageStatus;

        SentMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textViewMessageSend);
            timeText = itemView.findViewById(R.id.textViewTimeSend);
            messageStatus = itemView.findViewById(R.id.imageViewStatus);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessageText());
            timeText.setText(formatTimestamp(message.getTimestamp()));

            // Lógica de visibilidade para o status "lido"
            if ("lido".equals(message.getStatus())) {
                messageStatus.setVisibility(View.VISIBLE); // Torna visível
                messageStatus.setImageResource(R.drawable.ic_message_read); // Usa o seu ícone
            } else {
                messageStatus.setVisibility(View.GONE); // Desaparece se não for lido
            }
        }
    }

    // ViewHolder para mensagens RECEBIDAS
    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText, timeText;

        ReceivedMessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.textViewMessageReceive);
            timeText = itemView.findViewById(R.id.textViewTimeReceive);
        }

        void bind(ChatMessage message) {
            messageText.setText(message.getMessageText());
            timeText.setText(formatTimestamp(message.getTimestamp()));
        }
    }
}
