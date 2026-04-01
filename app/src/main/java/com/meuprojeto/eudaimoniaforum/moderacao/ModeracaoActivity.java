package com.meuprojeto.eudaimoniaforum.moderacao;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.perfil.Usuario;

import java.util.ArrayList;
import java.util.List;

public class ModeracaoActivity extends AppCompatActivity {

    private RecyclerView recyclerViewUsuarios;
    private RecyclerView recyclerViewDenuncias;
    private UsuarioModeracaoAdapter usuarioAdapter;
    private DenunciaAdapter denunciaAdapter;

    private List<Usuario> usuarioList = new ArrayList<>();
    private List<Usuario> usuarioListExibida = new ArrayList<>();
    private List<String> usuariosBanidos = new ArrayList<>();

    private List<Denuncia> denunciaList = new ArrayList<>();

    private EditText etBuscarUsuario;
    private Button btnBuscar;

    // Tab buttons
    private Button btnTabUsuarios, btnTabDenuncias;
    private LinearLayout layoutBuscaUsuarios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ModeracaoActivity", "onCreate() chamado. Inicializando ModeracaoActivity.");
        setContentView(R.layout.moderacao_activity);

        // Configurar UI
        recyclerViewUsuarios = findViewById(R.id.rvListaUsuarios);
        recyclerViewDenuncias = findViewById(R.id.rvListaDenuncias);

        etBuscarUsuario = findViewById(R.id.etBuscarUsuario);
        btnBuscar = findViewById(R.id.btnBuscar);

        btnTabUsuarios = findViewById(R.id.btnTabUsuarios);
        btnTabDenuncias = findViewById(R.id.btnTabDenuncias);
        layoutBuscaUsuarios = findViewById(R.id.layoutBuscaUsuarios);

        recyclerViewUsuarios.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDenuncias.setLayoutManager(new LinearLayoutManager(this));

        usuarioAdapter = new UsuarioModeracaoAdapter(usuarioListExibida);
        recyclerViewUsuarios.setAdapter(usuarioAdapter);

        denunciaAdapter = new DenunciaAdapter(denunciaList, this);
        recyclerViewDenuncias.setAdapter(denunciaAdapter);

        // Carregar dados
        carregarBanidos();
        carregarUsuarios();
        carregarDenuncias();

        // Busca de usuários
        etBuscarUsuario.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarUsuarios(s.toString());
            }

            public void afterTextChanged(Editable s) {
            }
        });
        btnBuscar.setOnClickListener(v -> filtrarUsuarios(etBuscarUsuario.getText().toString()));

        // Abas
        btnTabUsuarios.setOnClickListener(v -> mostrarAbaUsuarios());
        btnTabDenuncias.setOnClickListener(v -> mostrarAbaDenuncias());

        mostrarAbaDenuncias(); // Inicia na aba de denúncias que é mais urgente
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

    private void carregarDenuncias() {
        DatabaseReference denunciasRef = FirebaseDatabase.getInstance().getReference("denuncias");
        denunciasRef.orderByChild("status").equalTo("pendente").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                denunciaList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Denuncia d = ds.getValue(Denuncia.class);
                    if (d != null) {
                        denunciaList.add(d);
                    }
                }
                denunciaAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void carregarBanidos() {
        DatabaseReference banidosRef = FirebaseDatabase.getInstance().getReference("banidos");
        banidosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usuariosBanidos.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    usuariosBanidos.add(ds.getKey());
                }
                filtrarUsuarios(etBuscarUsuario.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void carregarUsuarios() {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usuarioList.clear();
                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    try {
                        Usuario usuario = new Usuario();
                        usuario.setUid(userSnapshot.getKey());
                        usuario.setNick(safelyGetString(userSnapshot, "nick"));

                        usuarioList.add(usuario);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                filtrarUsuarios(etBuscarUsuario.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void filtrarUsuarios(String query) {
        usuarioListExibida.clear();

        List<Usuario> naoBanidos = new ArrayList<>();
        for (Usuario u : usuarioList) {
            if (!usuariosBanidos.contains(u.getUid())) {
                naoBanidos.add(u);
            }
        }

        if (query == null || query.isEmpty()) {
            usuarioListExibida.addAll(naoBanidos);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Usuario u : naoBanidos) {
                boolean matchNick = u.getNick() != null && u.getNick().toLowerCase().contains(lowerQuery);
                boolean matchUid = u.getUid() != null && u.getUid().toLowerCase().contains(lowerQuery);
                if (matchNick || matchUid) {
                    usuarioListExibida.add(u);
                }
            }
        }
        usuarioAdapter.notifyDataSetChanged();
    }

    private String safelyGetString(DataSnapshot snapshot, String key) {
        if (snapshot.hasChild(key)) {
            Object value = snapshot.child(key).getValue();
            return value != null ? String.valueOf(value) : "";
        }
        return "";
    }
}
