package com.meuprojeto.eudaimoniaforum.perfil;

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
import com.meuprojeto.eudaimoniaforum.forum.MinhasPostagensActivity;
import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.chat.ChatActivity;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class VisualizarPerfilActivity extends AppCompatActivity {

    private TextView textViewNickUsuario, textViewDataEntrada, textViewVicioUsuario, textViewApresentacao;
    private android.widget.ImageView imageViewPerfilIcon;
    private TextView textViewNumPosts, textViewNumComentarios, textViewDiasAtivos;
    private View buttonEditarPerfil, buttonConversar;
    private android.widget.Button buttonMinhasPostagens;
    private android.widget.LinearLayout layoutBadges;

    private DatabaseReference userRef;
    private DatabaseReference postsRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("VisualizPerfilAct", "onCreate() chamado. Inicializando VisualizarPerfilActivity.");
        setContentView(R.layout.perfil_activity);

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
            carregarConquistasFirebase(userId);
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

        buttonMinhasPostagens.setText("Postagens do Usuário");
        buttonMinhasPostagens.setOnClickListener(v -> {
            Intent intent = new Intent(VisualizarPerfilActivity.this, MinhasPostagensActivity.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
        });
    }

    private void inicializarUI() {
        imageViewPerfilIcon = findViewById(R.id.imageViewPerfilIcon);
        textViewNickUsuario = findViewById(R.id.textViewNickUsuario);
        textViewDataEntrada = findViewById(R.id.textViewDataEntrada);
        textViewVicioUsuario = findViewById(R.id.textViewVicioUsuario);
        textViewApresentacao = findViewById(R.id.textViewApresentacao);
        textViewNumPosts = findViewById(R.id.textViewNumPosts);
        textViewNumComentarios = findViewById(R.id.textViewNumComentarios);
        textViewDiasAtivos = findViewById(R.id.textViewDiasAtivos);
        buttonEditarPerfil = findViewById(R.id.buttonEditarPerfil);
        buttonConversar = findViewById(R.id.buttonConversar);
        buttonMinhasPostagens = findViewById(R.id.buttonMinhasPostagens);
        layoutBadges = findViewById(R.id.layoutBadges);
    }

    private void carregarPerfilUsuario(String userId) {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Usuario usuario = dataSnapshot.getValue(Usuario.class);
                    if (usuario != null) {
                        AvatarUtils.carregarAvatar(VisualizarPerfilActivity.this, imageViewPerfilIcon, usuario.getAvatar());
                        textViewNickUsuario.setText(usuario.getNick());
                        textViewVicioUsuario.setText(usuario.getVicio() != null ? usuario.getVicio() : "Não definido");
                        textViewApresentacao.setText(
                                usuario.getSobreMim() != null ? usuario.getSobreMim() : "Ainda não há informações...");

                        try {
                            long dataEntradaMillis = Long.parseLong(usuario.getDataEntrada());
                            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                            textViewDataEntrada
                                    .setText("📅 Membro desde " + sdf.format(new java.util.Date(dataEntradaMillis)));

                        } catch (NumberFormatException e) {
                            textViewDataEntrada.setText("Data inválida");
                        }

                        long diasValidos = dataSnapshot.child("checkins").getChildrenCount();
                        textViewDiasAtivos.setText(String.valueOf(diasValidos));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        Query postsQuery = postsRef.orderByChild("autor").equalTo(userId);
        postsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                textViewNumPosts.setText(String.valueOf(snapshot.getChildrenCount()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        DatabaseReference comentariosRef = FirebaseDatabase.getInstance().getReference("forum/comentarios");
        comentariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long commentCount = 0;
                for (DataSnapshot postComentarios : snapshot.getChildren()) {
                    for (DataSnapshot commentSnapshot : postComentarios.getChildren()) {
                        if (userId.equals(commentSnapshot.child("autor").getValue(String.class))) {
                            commentCount++;
                        }
                    }
                }
                textViewNumComentarios.setText(String.valueOf(commentCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void carregarConquistasFirebase(String userId) {
        DatabaseReference conquistasRef = FirebaseDatabase.getInstance().getReference("users")
                .child(userId).child("conquistas");

        conquistasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                layoutBadges.removeAllViews();
                if (!snapshot.exists()) {
                    adicionarBadge("Iniciante", "#B0BEC5");
                    return;
                }
                if (snapshot.hasChild("badge_1_dia"))
                    adicionarBadge("1 Dia", "#8BC34A");
                if (snapshot.hasChild("badge_3_dias"))
                    adicionarBadge("3 Dias", "#4CAF50");
                if (snapshot.hasChild("badge_1_semana"))
                    adicionarBadge("1 Semana", "#009688");
                if (snapshot.hasChild("badge_1_mes"))
                    adicionarBadge("1 Mês", "#00BCD4");
                if (snapshot.hasChild("badge_3_meses"))
                    adicionarBadge("3 Meses", "#2196F3");
                if (snapshot.hasChild("badge_6_meses"))
                    adicionarBadge("6 Meses", "#3F51B5");
                if (snapshot.hasChild("badge_1_ano"))
                    adicionarBadge("1 Ano", "#9C27B0");

                if (layoutBadges.getChildCount() == 0) {
                    adicionarBadge("Iniciante", "#B0BEC5");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (layoutBadges.getChildCount() == 0)
                    adicionarBadge("Iniciante", "#B0BEC5");
            }
        });
    }

    private void adicionarBadge(String titulo, String corHex) {
        android.widget.LinearLayout badgeLayout = new android.widget.LinearLayout(this);
        badgeLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        badgeLayout.setGravity(android.view.Gravity.CENTER);
        badgeLayout.setPadding(16, 0, 16, 0);

        android.widget.ImageView icon = new android.widget.ImageView(this);
        icon.setImageResource(android.R.drawable.star_big_on);
        try {
            icon.setColorFilter(android.graphics.Color.parseColor(corHex));
        } catch (IllegalArgumentException e) {
            icon.setColorFilter(android.graphics.Color.GRAY);
        }
        icon.setLayoutParams(new android.widget.LinearLayout.LayoutParams(100, 100));

        TextView text = new TextView(this);
        text.setText(titulo);
        text.setTextSize(12);
        text.setTextColor(android.graphics.Color.parseColor("#424242"));
        text.setGravity(android.view.Gravity.CENTER);
        text.setTypeface(null, android.graphics.Typeface.BOLD);

        badgeLayout.addView(icon);
        badgeLayout.addView(text);
        layoutBadges.addView(badgeLayout);
    }
}
