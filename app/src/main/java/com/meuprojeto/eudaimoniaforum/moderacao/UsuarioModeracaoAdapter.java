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
import com.meuprojeto.eudaimoniaforum.perfil.Usuario;
import com.meuprojeto.eudaimoniaforum.perfil.VisualizarPerfilActivity;

import java.util.List;

public class UsuarioModeracaoAdapter extends RecyclerView.Adapter<UsuarioModeracaoAdapter.UsuarioViewHolder> {

    private List<Usuario> usuarioList;
    private UsuarioModeracaoAction callback;

    public interface UsuarioModeracaoAction {
        void onBanirClicado(Usuario usuario, int position);
    }

    public UsuarioModeracaoAdapter(List<Usuario> usuarioList, UsuarioModeracaoAction callback) {
        this.usuarioList = usuarioList;
        this.callback = callback;
    }

    @NonNull
    @Override
    public UsuarioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.moderacao_usuario_item, parent, false);
        return new UsuarioViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UsuarioViewHolder holder, int position) {
        Usuario usuario = usuarioList.get(position);

        holder.textViewNick.setText(usuario.getNick());
        holder.textViewUid.setText("UID: " + usuario.getUid());

        holder.btnVerPerfil.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, VisualizarPerfilActivity.class);
            intent.putExtra("USER_ID", usuario.getUid());
            context.startActivity(intent);
        });

        holder.btnBanir.setOnClickListener(v -> {
            if (callback != null) {
                callback.onBanirClicado(usuario, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return usuarioList.size();
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
