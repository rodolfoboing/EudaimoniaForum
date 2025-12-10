package com.meuprojeto.eudaimoniaforum;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ModeracaoActivity extends AppCompatActivity {

    private RecyclerView recyclerViewUsuarios;
    private UsuarioModeracaoAdapter usuarioAdapter;
    private List<Usuario> usuarioList = new ArrayList<>();
    private List<Usuario> usuarioListExibida = new ArrayList<>();
    private List<String> usuariosBanidos = new ArrayList<>();
    private EditText etBuscarUsuario;
    private Button btnBuscar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_moderacao);

        // Configurar UI
        recyclerViewUsuarios = findViewById(R.id.rvListaUsuarios);
        etBuscarUsuario = findViewById(R.id.etBuscarUsuario);
        btnBuscar = findViewById(R.id.btnBuscar);

        recyclerViewUsuarios.setLayoutManager(new LinearLayoutManager(this));

        usuarioAdapter = new UsuarioModeracaoAdapter(usuarioListExibida);
        recyclerViewUsuarios.setAdapter(usuarioAdapter);

        // Carregar dados do Firebase
        carregarBanidos();
        carregarUsuarios();

        // Busca
        etBuscarUsuario.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarUsuarios(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnBuscar.setOnClickListener(v -> filtrarUsuarios(etBuscarUsuario.getText().toString()));
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
                // Atualiza a lista exibida sempre que a lista de banidos mudar
                filtrarUsuarios(etBuscarUsuario.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
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
                        usuario.setDataEntrada(safelyGetString(userSnapshot, "dataEntrada"));
                        usuario.setSobreMim(safelyGetString(userSnapshot, "sobreMim"));
                        usuario.setVicio(safelyGetString(userSnapshot, "vicio"));

                        Object timestampObj = userSnapshot.child("lastLoginTimestamp").getValue();
                        if (timestampObj instanceof Long) {
                            usuario.setLastLoginTimestamp((Long) timestampObj);
                        } else {
                            usuario.setLastLoginTimestamp(0);
                        }

                        usuarioList.add(usuario);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                filtrarUsuarios(etBuscarUsuario.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void filtrarUsuarios(String query) {
        usuarioListExibida.clear();
        
        // Lista intermediária apenas com não banidos
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
