package com.meuprojeto.eudaimoniaforum.perfil;

import android.content.Intent;
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

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.forum.MinhasPostagensActivity;
import com.meuprojeto.eudaimoniaforum.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PerfilActivity extends AppCompatActivity {

    private TextView textViewNickUsuario, textViewDataEntrada, textViewVicioUsuario, textViewApresentacao;
    private ImageView imageViewPerfilIcon;
    private TextView textViewNumPosts, textViewNumComentarios, textViewDiasAtivos;
    private View buttonEditarPerfil, buttonConversar;
    private Button buttonMinhasPostagens;
    private LinearLayout layoutBadges;
    private View badgesSection;

    private ProfileManager profileManager;
    private ValueEventListener conquistasListener;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("PerfilActivity", "onCreate() chamado. Inicializando PerfilActivity.");
        setContentView(R.layout.perfil_activity);

        inicializarUI();

        buttonConversar.setVisibility(View.GONE);

        profileManager = new ProfileManager();
        if (profileManager.getCurrentUser() != null) {
            currentUserId = profileManager.getCurrentUser().getUid();
            carregarTudo(currentUserId);
        } else {
            Toast.makeText(this, "Erro: Usuário não autenticado!", Toast.LENGTH_SHORT).show();
            finish();
        }

        buttonEditarPerfil.setOnClickListener(v -> startActivity(new Intent(this, EditarPerfilActivity.class)));
        buttonMinhasPostagens.setOnClickListener(v -> startActivity(new Intent(this, MinhasPostagensActivity.class)));
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
        if (currentUserId != null) {
            carregarTudo(currentUserId);
        }
    }

    private void carregarTudo(String userId) {
        if (currentUserId != null && !userId.equals(currentUserId)) {
            badgesSection.setVisibility(View.GONE);
        } else {
            badgesSection.setVisibility(View.VISIBLE);
        }

        profileManager.carregarPerfilPorId(userId, new ProfileManager.FullProfileCallback() {
            @Override
            public void onProfileDataLoaded(Usuario usuario, long diasValidos) {
                if(isFinishing() || isDestroyed()) return;
                preencherCabecalho(usuario, diasValidos);
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(PerfilActivity.this, erro, Toast.LENGTH_SHORT).show();
            }
        });

        profileManager.carregarProgressoEstatisticas(userId, (numPosts, numComentarios) -> {
            if(isFinishing() || isDestroyed()) return;
            textViewNumPosts.setText(String.valueOf(numPosts));
            textViewNumComentarios.setText(String.valueOf(numComentarios));
        });

        if(conquistasListener != null) {
            profileManager.removerConquistasListener(userId, conquistasListener);
        }

        conquistasListener = profileManager.monitorarConquistas(userId, new ProfileManager.ConquistasCallback() {
            @Override
            public void onConquistasLoaded(List<String> badgetIds) {
                if(isFinishing() || isDestroyed()) return;
                preencherConquistas(badgetIds);
            }

            @Override
            public void onNenhumaConquista() {
                if(isFinishing() || isDestroyed()) return;
                layoutBadges.removeAllViews();
                adicionarBadge("Iniciante", "#B0BEC5");
            }
        });
    }

    private void preencherCabecalho(Usuario usuario, long diasValidos) {
        AvatarUtils.carregarAvatar(PerfilActivity.this, imageViewPerfilIcon, usuario.getAvatar());
        textViewNickUsuario.setText(usuario.getNick());
        textViewVicioUsuario.setText(usuario.getVicio() != null ? usuario.getVicio() : "Não definido");
        textViewApresentacao.setText(usuario.getSobreMim() != null ? usuario.getSobreMim() : "Ainda não há informações...");
        textViewDiasAtivos.setText(String.valueOf(diasValidos));

        try {
            long dataEntradaMillis = Long.parseLong(usuario.getDataEntrada());
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            textViewDataEntrada.setText("📅 Membro desde " + sdf.format(new Date(dataEntradaMillis)));
        } catch (Exception e) {
            textViewDataEntrada.setText("Data inválida");
        }
    }

    private void preencherConquistas(List<String> bIds) {
        layoutBadges.removeAllViews();
        if (bIds.contains("badge_1_dia")) adicionarBadge("1 Dia", "#8BC34A");
        if (bIds.contains("badge_3_dias")) adicionarBadge("3 Dias", "#4CAF50");
        if (bIds.contains("badge_1_semana")) adicionarBadge("1 Semana", "#009688");
        if (bIds.contains("badge_1_mes")) adicionarBadge("1 Mês", "#00BCD4");
        if (bIds.contains("badge_3_meses")) adicionarBadge("3 Meses", "#2196F3");
        if (bIds.contains("badge_6_meses")) adicionarBadge("6 Meses", "#3F51B5");
        if (bIds.contains("badge_1_ano")) adicionarBadge("1 Ano", "#9C27B0");

        if (layoutBadges.getChildCount() == 0) {
            adicionarBadge("Iniciante", "#B0BEC5");
        }
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
        icon.setLayoutParams(new LinearLayout.LayoutParams(100, 100));

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(profileManager != null && currentUserId != null && conquistasListener != null) {
            profileManager.removerConquistasListener(currentUserId, conquistasListener);
        }
    }
}
