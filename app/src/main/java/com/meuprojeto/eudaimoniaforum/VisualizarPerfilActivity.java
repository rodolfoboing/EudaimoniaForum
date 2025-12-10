package com.meuprojeto.eudaimoniaforum;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class VisualizarPerfilActivity extends AppCompatActivity {

    private TextView textViewNickUsuario, textViewDataEntrada, textViewVicioUsuario, textViewApresentacao;
    private TextView textViewNumPosts, textViewNumComentarios, textViewDiasAtivos;
    private View buttonEditarPerfil, buttonConversar;

    private DatabaseReference userRef;
    private DatabaseReference postsRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_perfil);

        userId = getIntent().getStringExtra("USER_ID");

        inicializarUI();

        // No perfil de outro usuário, escondemos o botão de editar
        buttonEditarPerfil.setVisibility(View.GONE);

        if (userId != null) {
            // Esconder o botão de conversar se o usuário estiver vendo o próprio perfil
            if (userId.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
                buttonConversar.setVisibility(View.GONE);
                buttonEditarPerfil.setVisibility(View.VISIBLE); // Mostra o de editar nesse caso
            }

            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            postsRef = FirebaseDatabase.getInstance().getReference("forum/posts");
            carregarPerfilUsuario(userId);
        } else {
            Toast.makeText(this, "Erro: ID do usuário não fornecido.", Toast.LENGTH_SHORT).show();
            finish();
        }

        buttonConversar.setOnClickListener(v -> {
            Intent intent = new Intent(VisualizarPerfilActivity.this, ChatActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });

        buttonEditarPerfil.setOnClickListener(v -> {
            startActivity(new Intent(this, EditarPerfilActivity.class));
        });
    }

    private void inicializarUI() {
        textViewNickUsuario = findViewById(R.id.textViewNickUsuario);
        textViewDataEntrada = findViewById(R.id.textViewDataEntrada);
        textViewVicioUsuario = findViewById(R.id.textViewVicioUsuario);
        textViewApresentacao = findViewById(R.id.textViewApresentacao);
        textViewNumPosts = findViewById(R.id.textViewNumPosts);
        textViewNumComentarios = findViewById(R.id.textViewNumComentarios);
        textViewDiasAtivos = findViewById(R.id.textViewDiasAtivos);
        buttonEditarPerfil = findViewById(R.id.buttonEditarPerfil);
        buttonConversar = findViewById(R.id.buttonConversar);
    }

    private void carregarPerfilUsuario(String userId) {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Usuario usuario = dataSnapshot.getValue(Usuario.class);
                    if (usuario != null) {
                        textViewNickUsuario.setText(usuario.getNick());
                        textViewVicioUsuario.setText(usuario.getVicio() != null ? usuario.getVicio() : "Não definido");
                        textViewApresentacao.setText(usuario.getSobreMim() != null ? usuario.getSobreMim() : "Ainda não há informações...");

                        try {
                            long dataEntradaMillis = Long.parseLong(usuario.getDataEntrada());
                            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                            textViewDataEntrada.setText("📅 Membro desde " + sdf.format(new java.util.Date(dataEntradaMillis)));

                            long diff = System.currentTimeMillis() - dataEntradaMillis;
                            long dias = TimeUnit.MILLISECONDS.toDays(diff);
                            textViewDiasAtivos.setText(String.valueOf(dias > 0 ? dias : 1));
                        } catch (NumberFormatException e) {
                            textViewDataEntrada.setText("Data inválida");
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        Query postsQuery = postsRef.orderByChild("autor").equalTo(userId);
        postsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                textViewNumPosts.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        postsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long commentCount = 0;
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    if (postSnapshot.hasChild("comentarios")) {
                        for (DataSnapshot commentSnapshot : postSnapshot.child("comentarios").getChildren()) {
                            if (userId.equals(commentSnapshot.child("autor").getValue(String.class))) {
                                commentCount++;
                            }
                        }
                    }
                }
                textViewNumComentarios.setText(String.valueOf(commentCount));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
