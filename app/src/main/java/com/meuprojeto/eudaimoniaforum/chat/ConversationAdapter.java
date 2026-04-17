package com.meuprojeto.eudaimoniaforum.chat;

import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.meuprojeto.eudaimoniaforum.R;

import java.util.List;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversaViewHolder> {

    private List<Conversation> conversasList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Conversation conversation);
    }

    public ConversationAdapter(List<Conversation> conversasList, OnItemClickListener listener) {
        this.conversasList = conversasList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.chat_conversation_item, parent, false);
        return new ConversaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversaViewHolder holder, int position) {
        Conversation conversation = conversasList.get(position);
        holder.bind(conversation, listener);
    }

    @Override
    public int getItemCount() {
        return conversasList.size();
    }

    static class ConversaViewHolder extends RecyclerView.ViewHolder {
        private final View barraLateralNaoLida;
        private final TextView textViewNomeUsuario;
        private final TextView textViewHora;
        private final ImageView iconEnvelopeNaoLido;
        private final TextView textViewUltimaMensagem;

        public ConversaViewHolder(@NonNull View itemView) {
            super(itemView);
            barraLateralNaoLida = itemView.findViewById(R.id.barraLateralNaoLida);
            textViewNomeUsuario = itemView.findViewById(R.id.textViewNomeUsuario);
            textViewHora = itemView.findViewById(R.id.textViewHora);
            iconEnvelopeNaoLido = itemView.findViewById(R.id.iconEnvelopeNaoLido);
            textViewUltimaMensagem = itemView.findViewById(R.id.textViewUltimaMensagem);
        }

        public void bind(final Conversation conversation, final OnItemClickListener listener) {
            textViewNomeUsuario.setText(conversation.getOtherUserNick());
            textViewUltimaMensagem.setText(conversation.getLastMessage());

            if (conversation.getLastMessageTimestamp() > 0) {
                textViewHora.setText(DateUtils.getRelativeTimeSpanString(conversation.getLastMessageTimestamp(),
                        System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));
            } else {
                textViewHora.setText("");
            }

            // A lógica agora só mexe na visibilidade e no estilo da fonte
            if (conversation.getUnreadCount() > 0) {
                barraLateralNaoLida.setVisibility(View.VISIBLE);
                iconEnvelopeNaoLido.setVisibility(View.VISIBLE);
                textViewNomeUsuario.setTypeface(null, Typeface.BOLD);
                textViewUltimaMensagem.setTypeface(null, Typeface.BOLD);
            } 
            else {
                barraLateralNaoLida.setVisibility(View.GONE);
                iconEnvelopeNaoLido.setVisibility(View.GONE);
                textViewNomeUsuario.setTypeface(null, Typeface.BOLD); // Manter nome em negrito
                textViewUltimaMensagem.setTypeface(null, Typeface.NORMAL);
            }

            itemView.setOnClickListener(v -> listener.onItemClick(conversation));
        }
    }
}
