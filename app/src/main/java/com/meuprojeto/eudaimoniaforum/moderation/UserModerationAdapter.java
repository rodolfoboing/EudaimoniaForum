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
import com.meuprojeto.eudaimoniaforum.profile.User;
import com.meuprojeto.eudaimoniaforum.profile.ViewProfileActivity;

import java.util.List;

public class UserModerationAdapter extends RecyclerView.Adapter<UserModerationAdapter.UsuarioViewHolder> {

    private List<User> userList;
    private UsuarioModeracaoAction callback;

    public interface UsuarioModeracaoAction {
        void onBanirClicado(User user, int position);
    }

    public UserModerationAdapter(List<User> userList, UsuarioModeracaoAction callback) {
        this.userList = userList;
        this.callback = callback;
    }

    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.moderation_user_item, parent, false);
        return new UsuarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        User user = userList.get(position);

        holder.textViewNick.setText(user.getNick());
        holder.textViewUid.setText("UID: " + user.getUid());

        holder.btnVerPerfil.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, ViewProfileActivity.class);
            intent.putExtra("USER_ID", user.getUid());
            context.startActivity(intent);
        });

        holder.btnBanir.setOnClickListener(v -> {
            if (callback != null) {
                callback.onBanirClicado(user, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UsuarioViewHolder extends RecyclerView.ViewHolder {
        TextView textViewNick, textViewUid;
        Button btnBanir;
        Button btnVerPerfil;

        public UsuarioViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewNick = itemView.findViewById(R.id.textViewNickUsuario);
            textViewUid = itemView.findViewById(R.id.textViewUidUsuario);
            btnBanir = itemView.findViewById(R.id.btnBanirUsuario);
            btnVerPerfil = itemView.findViewById(R.id.btnVerPerfil);
        }
    }
}
