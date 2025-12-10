package com.meuprojeto.eudaimoniaforum;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PerfilActivity extends AppCompatActivity {

    private TextView textViewNickUsuario, textViewDataEntrada, textViewVicioUsuario, textViewApresentacao;
    private TextView textViewNumPosts, textViewNumComentarios, textViewDiasAtivos;
    private View buttonEditarPerfil, buttonConversar; // Usando View que é a classe pai de LinearLayout

    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;
    private DatabaseReference postsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_perfil);

        // Inicialização dos componentes da UI
        inicializarUI();

        // No perfil do próprio usuário, escondemos o botão de conversar
        buttonConversar.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            postsRef = FirebaseDatabase.getInstance().getReference("forum/posts");
            carregarDadosPerfil(userId);
        } else {
            Toast.makeText(this, "Erro: Usuário não autenticado!", Toast.LENGTH_SHORT).show();
            finish();
        }

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

    @Override
    protected void onResume() {
        super.onResume();
        // Recarrega os dados sempre que a tela se torna visível
        if (firebaseAuth.getCurrentUser() != null) {
            carregarDadosPerfil(firebaseAuth.getCurrentUser().getUid());
        }
    }

    private void carregarDadosPerfil(String userId) {
        // Carrega dados básicos do usuário
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Usuario usuario = dataSnapshot.getValue(Usuario.class);
                    if (usuario != null) {
                        textViewNickUsuario.setText(usuario.getNick());
                        textViewVicioUsuario.setText(usuario.getVicio() != null ? usuario.getVicio() : "Não definido");
                        textViewApresentacao.setText(usuario.getSobreMim() != null ? usuario.getSobreMim() : "Ainda não há informações...");

                        // Calcula e exibe data de entrada e dias ativos
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
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PerfilActivity.this, "Falha ao carregar dados.", Toast.LENGTH_SHORT).show();
            }
        });

        // Contar posts do usuário
        Query postsQuery = postsRef.orderByChild("autor").equalTo(userId);
        postsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                textViewNumPosts.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Contar comentários do usuário (lógica mais complexa, iterando sobre todos os posts)
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
