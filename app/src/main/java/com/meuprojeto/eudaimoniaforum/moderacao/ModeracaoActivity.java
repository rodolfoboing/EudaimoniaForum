package com.meuprojeto.eudaimoniaforum.moderacao;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.perfil.Usuario;

import java.util.ArrayList;
import java.util.List;

public class ModeracaoActivity extends AppCompatActivity {

    private RecyclerView recyclerViewUsuarios;
    private RecyclerView recyclerViewDenuncias;
    private UsuarioModeracaoAdapter usuarioAdapter;
    private DenunciaAdapter denunciaAdapter;

    private List<Usuario> usuarioListBase = new ArrayList<>();
    private List<Usuario> usuarioListExibida = new ArrayList<>();
    private List<Denuncia> denunciaList = new ArrayList<>();

    private EditText etBuscarUsuario;
    private Button btnBuscar;

    private Button btnTabUsuarios, btnTabDenuncias;
    private LinearLayout layoutBuscaUsuarios;

    private ModeracaoManager moderacaoManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ModeracaoActivity", "onCreate() chamado. Inicializando ModeracaoActivity.");
        setContentView(R.layout.moderacao_activity);

        moderacaoManager = new ModeracaoManager();

        recyclerViewUsuarios = findViewById(R.id.rvListaUsuarios);
        recyclerViewDenuncias = findViewById(R.id.rvListaDenuncias);
        etBuscarUsuario = findViewById(R.id.etBuscarUsuario);
        btnBuscar = findViewById(R.id.btnBuscar);
        btnTabUsuarios = findViewById(R.id.btnTabUsuarios);
        btnTabDenuncias = findViewById(R.id.btnTabDenuncias);
        layoutBuscaUsuarios = findViewById(R.id.layoutBuscaUsuarios);

        recyclerViewUsuarios.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDenuncias.setLayoutManager(new LinearLayoutManager(this));

        configurarAdapters();

        etBuscarUsuario.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarUsuarios(s.toString());
            }
            public void afterTextChanged(Editable s) {}
        });

        btnBuscar.setOnClickListener(v -> filtrarUsuarios(etBuscarUsuario.getText().toString()));

        btnTabUsuarios.setOnClickListener(v -> mostrarAbaUsuarios());
        btnTabDenuncias.setOnClickListener(v -> mostrarAbaDenuncias());

        carregarDados();
        mostrarAbaDenuncias();
    }

    private void configurarAdapters() {
        usuarioAdapter = new UsuarioModeracaoAdapter(usuarioListExibida, (usuario, position) -> {
            moderacaoManager.banirUsuario(usuario.getUid(), new ModeracaoManager.AcaoCallback() {
                @Override
                public void onSuccess() {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(ModeracaoActivity.this, "Usuário " + usuario.getNick() + " banido.", Toast.LENGTH_SHORT).show();
                    // O recarregamento via listener cuidará de removê-lo da lista
                }

                @Override
                public void onError(String erro) {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(ModeracaoActivity.this, "Erro ao banir: " + erro, Toast.LENGTH_SHORT).show();
                }
            });
        });
        recyclerViewUsuarios.setAdapter(usuarioAdapter);

        denunciaAdapter = new DenunciaAdapter(denunciaList, this, new DenunciaAdapter.DenunciaAction() {
            @Override
            public void onResolverClicado(Denuncia denuncia) {
                moderacaoManager.resolverDenuncia(denuncia.getId(), new ModeracaoManager.AcaoCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(ModeracaoActivity.this, "Denúncia resolvida", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String erro) {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(ModeracaoActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onApagarPostClicado(Denuncia denuncia) {
                moderacaoManager.apagarConteudoOfensivoEResolver(denuncia, new ModeracaoManager.AcaoCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(ModeracaoActivity.this, "Conteúdo apagado com sucesso!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String erro) {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(ModeracaoActivity.this, "Erro ao apagar: " + erro, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        recyclerViewDenuncias.setAdapter(denunciaAdapter);
    }

    private void carregarDados() {
        moderacaoManager.carregarDenunciasPendentes(denuncias -> {
            if(isFinishing() || isDestroyed()) return;
            denunciaList.clear();
            denunciaList.addAll(denuncias);
            denunciaAdapter.notifyDataSetChanged();
        });

        moderacaoManager.carregarUsuariosAtivos((listNaoBanidos, listBannedIds) -> {
            if(isFinishing() || isDestroyed()) return;
            usuarioListBase.clear();
            usuarioListBase.addAll(listNaoBanidos);
            filtrarUsuarios(etBuscarUsuario.getText().toString());
        });
    }

    private void mostrarAbaUsuarios() {
        recyclerViewUsuarios.setVisibility(View.VISIBLE);
        layoutBuscaUsuarios.setVisibility(View.VISIBLE);
        recyclerViewDenuncias.setVisibility(View.GONE);
        btnTabUsuarios.setAlpha(1.0f);
        btnTabDenuncias.setAlpha(0.5f);
    }

    private void mostrarAbaDenuncias() {
        recyclerViewUsuarios.setVisibility(View.GONE);
        layoutBuscaUsuarios.setVisibility(View.GONE);
        recyclerViewDenuncias.setVisibility(View.VISIBLE);
        btnTabUsuarios.setAlpha(0.5f);
        btnTabDenuncias.setAlpha(1.0f);
    }

    private void filtrarUsuarios(String query) {
        usuarioListExibida.clear();

        if (query == null || query.isEmpty()) {
            usuarioListExibida.addAll(usuarioListBase);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Usuario u : usuarioListBase) {
                boolean matchNick = u.getNick() != null && u.getNick().toLowerCase().contains(lowerQuery);
                boolean matchUid = u.getUid() != null && u.getUid().toLowerCase().contains(lowerQuery);
                if (matchNick || matchUid) {
                    usuarioListExibida.add(u);
                }
            }
        }
        usuarioAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (moderacaoManager != null) {
            moderacaoManager.destruir();
        }
    }
}
