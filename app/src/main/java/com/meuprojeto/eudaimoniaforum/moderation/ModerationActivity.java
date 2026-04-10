package com.meuprojeto.eudaimoniaforum.moderation;

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
import com.meuprojeto.eudaimoniaforum.profile.User;

import java.util.ArrayList;
import java.util.List;

public class ModerationActivity extends AppCompatActivity {

    private RecyclerView recyclerViewUsuarios;
    private RecyclerView recyclerViewDenuncias;
    private UserModerationAdapter usuarioAdapter;
    private ReportAdapter reportAdapter;

    private List<User> userListBase = new ArrayList<>();
    private List<User> userListExibida = new ArrayList<>();
    private List<Report> reportList = new ArrayList<>();

    private EditText etBuscarUsuario;
    private Button btnBuscar;

    private Button btnTabUsuarios, btnTabDenuncias;
    private LinearLayout layoutBuscaUsuarios;

    private ModerationManager moderationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ModerationActivity", "onCreate() chamado. Inicializando ModerationActivity.");
        setContentView(R.layout.moderacao_activity);

        moderationManager = new ModerationManager();

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
        usuarioAdapter = new UserModerationAdapter(userListExibida, (usuario, position) -> {
            moderationManager.banirUsuario(usuario.getUid(), new ModerationManager.AcaoCallback() {
                @Override
                public void onSuccess() {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(ModerationActivity.this, "Usuário " + usuario.getNick() + " banido.", Toast.LENGTH_SHORT).show();
                    // O recarregamento via listener cuidará de removê-lo da lista
                }

                @Override
                public void onError(String erro) {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(ModerationActivity.this, "Erro ao banir: " + erro, Toast.LENGTH_SHORT).show();
                }
            });
        });
        recyclerViewUsuarios.setAdapter(usuarioAdapter);

        reportAdapter = new ReportAdapter(reportList, this, new ReportAdapter.DenunciaAction() {
            @Override
            public void onResolverClicado(Report report) {
                moderationManager.resolverDenuncia(report.getId(), new ModerationManager.AcaoCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(ModerationActivity.this, "Denúncia resolvida", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String erro) {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(ModerationActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onApagarPostClicado(Report report) {
                moderationManager.apagarConteudoOfensivoEResolver(report, new ModerationManager.AcaoCallback() {
                    @Override
                    public void onSuccess() {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(ModerationActivity.this, "Conteúdo apagado com sucesso!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String erro) {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(ModerationActivity.this, "Erro ao apagar: " + erro, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        recyclerViewDenuncias.setAdapter(reportAdapter);
    }

    private void carregarDados() {
        moderationManager.carregarDenunciasPendentes(denuncias -> {
            if(isFinishing() || isDestroyed()) return;
            reportList.clear();
            reportList.addAll(denuncias);
            reportAdapter.notifyDataSetChanged();
        });

        moderationManager.carregarUsuariosAtivos((listNaoBanidos, listBannedIds) -> {
            if(isFinishing() || isDestroyed()) return;
            userListBase.clear();
            userListBase.addAll(listNaoBanidos);
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
        userListExibida.clear();

        if (query == null || query.isEmpty()) {
            userListExibida.addAll(userListBase);
        } else {
            String lowerQuery = query.toLowerCase();
            for (User u : userListBase) {
                boolean matchNick = u.getNick() != null && u.getNick().toLowerCase().contains(lowerQuery);
                boolean matchUid = u.getUid() != null && u.getUid().toLowerCase().contains(lowerQuery);
                if (matchNick || matchUid) {
                    userListExibida.add(u);
                }
            }
        }
        usuarioAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (moderationManager != null) {
            moderationManager.destruir();
        }
    }
}
