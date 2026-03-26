package com.meuprojeto.eudaimoniaforum;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PerfilActivity extends AppCompatActivity {

    private TextView textViewNickUsuario, textViewDataEntrada, textViewVicioUsuario, textViewApresentacao;
    private ImageView imageViewPerfilIcon;
    private TextView textViewNumPosts, textViewNumComentarios, textViewDiasAtivos;
    private View buttonEditarPerfil, buttonConversar;
    private Button buttonMinhasPostagens;
    private LinearLayout layoutBadges;
    private View badgesSection;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;
    private DatabaseReference postsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("PerfilActivity", "onCreate() chamado. Inicializando PerfilActivity.");
        setContentView(R.layout.tela_perfil);

        inicializarUI();

        // No perfil do próprio usuário, esconde o botão de conversar
        buttonConversar.setVisibility(View.GONE);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            postsRef = FirebaseDatabase.getInstance().getReference("forum/posts");
            carregarDadosPerfil(userId);
            // Carrega conquistas verificadas do Firebase
            carregarConquistasFirebase(userId);
        } else {
            Toast.makeText(this, "Erro: Usuário não autenticado!", Toast.LENGTH_SHORT).show();
            finish();
        }

        buttonEditarPerfil.setOnClickListener(v -> {
            startActivity(new Intent(this, EditarPerfilActivity.class));
        });

        buttonMinhasPostagens.setOnClickListener(v -> {
            startActivity(new Intent(this, MinhasPostagensActivity.class));
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
        badgesSection = findViewById(R.id.badgesSection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firebaseAuth.getCurrentUser() != null) {
            String currentUserId = firebaseAuth.getCurrentUser().getUid();
            carregarDadosPerfil(currentUserId);
            carregarConquistasFirebase(currentUserId);
        }
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

                // Ordem sugerida: 1 dia, 3 dias, 1 semana, 1 mes, 3 meses, 6 meses, 1 ano
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
                // Em caso de erro, tenta carregar iniciante
                if (layoutBadges.getChildCount() == 0) {
                    adicionarBadge("Iniciante", "#B0BEC5");
                }
            }
        });
    }

    private void adicionarBadge(String titulo, String corHex) {
        LinearLayout badgeLayout = new LinearLayout(this);
        badgeLayout.setOrientation(LinearLayout.VERTICAL);
        badgeLayout.setGravity(Gravity.CENTER);
        badgeLayout.setPadding(16, 0, 16, 0);

        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.star_big_on);
        try {
            icon.setColorFilter(Color.parseColor(corHex));
        } catch (IllegalArgumentException e) {
            icon.setColorFilter(Color.GRAY);
        }
        icon.setLayoutParams(new LinearLayout.LayoutParams(100, 100)); // Tamanho fixo

        TextView text = new TextView(this);
        text.setText(titulo);
        text.setTextSize(12);
        text.setTextColor(Color.parseColor("#424242"));
        text.setGravity(Gravity.CENTER);
        text.setTypeface(null, Typeface.BOLD);

        badgeLayout.addView(icon);
        badgeLayout.addView(text);

        layoutBadges.addView(badgeLayout);
    }

    private void carregarDadosPerfil(String userId) {
        if (firebaseAuth.getCurrentUser() != null && !userId.equals(firebaseAuth.getCurrentUser().getUid())) {
            badgesSection.setVisibility(View.GONE);
        } else {
            badgesSection.setVisibility(View.VISIBLE);
        }

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Usuario usuario = dataSnapshot.getValue(Usuario.class);
                    if (usuario != null) {
                        AvatarUtils.carregarAvatar(PerfilActivity.this, imageViewPerfilIcon, usuario.getAvatar());
                        textViewNickUsuario.setText(usuario.getNick());
                        textViewVicioUsuario.setText(usuario.getVicio() != null ? usuario.getVicio() : "Não definido");
                        textViewApresentacao.setText(
                                usuario.getSobreMim() != null ? usuario.getSobreMim() : "Ainda não há informações...");

                        try {
                            long dataEntradaMillis = Long.parseLong(usuario.getDataEntrada());
                            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                            textViewDataEntrada.setText("📅 Membro desde " + sdf.format(new Date(dataEntradaMillis)));
                        } catch (Exception e) {
                            textViewDataEntrada.setText("Data inválida");
                        }
                        
                        long diasValidos = dataSnapshot.child("checkins").getChildrenCount();
                        textViewDiasAtivos.setText(String.valueOf(diasValidos));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PerfilActivity.this, "Falha ao carregar dados.", Toast.LENGTH_SHORT).show();
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
}
