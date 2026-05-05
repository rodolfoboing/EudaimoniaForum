package com.meuprojeto.eudaimoniaforum.profile;

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
import com.meuprojeto.eudaimoniaforum.forum.MyPostActivity;
import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.chat.ChatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class ViewProfileActivity extends AppCompatActivity {

    private TextView textViewNickUsuario, textViewDataEntrada, textViewVicioUsuario, textViewApresentacao;
    private ImageView imageViewPerfilIcon;
    private TextView textViewNumPosts, textViewNumComentarios, textViewDiasAtivos;
    private View buttonEditarPerfil, buttonConversar;
    private Button buttonMinhasPostagens;
    private LinearLayout layoutBadges;

    private ProfileManager profileManager;
    private String targetUserId;
    private ValueEventListener conquistasListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("VisualizPerfilAct", "onCreate() chamado. Inicializando ViewProfileActivity.");
        setContentView(R.layout.profile_activity);

        targetUserId = getIntent().getStringExtra("USER_ID");

        inicializarUI();

        buttonEditarPerfil.setVisibility(View.GONE);

        if (targetUserId != null) {
            profileManager = new ProfileManager(this);
            
            if (profileManager.getCurrentUser() != null && targetUserId.equals(profileManager.getCurrentUser().getUid())) {
                buttonConversar.setVisibility(View.GONE);
                buttonEditarPerfil.setVisibility(View.VISIBLE);
            }

            carregarTudo(targetUserId);

        } else {
            Toast.makeText(this, "Erro: ID do usuário não fornecido.", Toast.LENGTH_SHORT).show();
            finish();
        }

        buttonConversar.setOnClickListener(v -> {
            Intent intent = new Intent(ViewProfileActivity.this, ChatActivity.class);
            intent.putExtra("USER_ID", targetUserId);
            startActivity(intent);
        });

        buttonEditarPerfil.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));

        buttonMinhasPostagens.setText("Postagens do Usuário");
        buttonMinhasPostagens.setOnClickListener(v -> {
            Intent intent = new Intent(ViewProfileActivity.this, MyPostActivity.class);
            intent.putExtra("USER_ID", targetUserId);
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

    private void carregarTudo(String userId) {
        profileManager.carregarPerfilPorId(userId, new ProfileManager.FullProfileCallback() {
            @Override
            public void onProfileDataLoaded(User user, long diasValidos) {
                if(isFinishing() || isDestroyed()) return;
                preencherCabecalho(user, diasValidos);
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(ViewProfileActivity.this, erro, Toast.LENGTH_SHORT).show();
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
            public void onConquistasLoaded(Set<String> badgetIds) {
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

    private void preencherCabecalho(User user, long diasValidos) {
        AvatarUtils.carregarAvatar(ViewProfileActivity.this, imageViewPerfilIcon, user.getAvatar());
        textViewNickUsuario.setText(user.getNick());
        textViewVicioUsuario.setText(user.getVicio() != null ? user.getVicio() : "Não definido");
        textViewApresentacao.setText(user.getSobreMim() != null ? user.getSobreMim() : "Ainda não há informações...");
        textViewDiasAtivos.setText(String.valueOf(diasValidos));

        try {
            long dataEntradaMillis = Long.parseLong(user.getDataEntrada());
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            textViewDataEntrada.setText("📅 Membro desde " + sdf.format(new Date(dataEntradaMillis)));
        } catch (NumberFormatException e) {
            textViewDataEntrada.setText("Data inválida");
        }
    }

    private void preencherConquistas(Set<String> bIds) {
        layoutBadges.removeAllViews();
        if (bIds.contains("badge_1_dia")) adicionarBadge("1 Dia", "🛡️");
        if (bIds.contains("badge_3_dias")) adicionarBadge("3 Dias", "⚔️");
        if (bIds.contains("badge_1_semana")) adicionarBadge("1 Semana", "🏅");
        if (bIds.contains("badge_1_mes")) adicionarBadge("1 Mês", "🎖️");
        if (bIds.contains("badge_3_meses")) adicionarBadge("3 Meses", "🏆");
        if (bIds.contains("badge_6_meses")) adicionarBadge("6 Meses", "🌟");
        if (bIds.contains("badge_1_ano")) adicionarBadge("1 Ano", "👑");
        if (bIds.contains("badge_3_anos")) adicionarBadge("3 Anos", "💎");

        if (layoutBadges.getChildCount() == 0) {
            adicionarBadge("Iniciante", "🌱");
        }
    }

    private void adicionarBadge(String titulo, String emojiIcon) {
        LinearLayout badgeLayout = new LinearLayout(this);
        badgeLayout.setOrientation(LinearLayout.VERTICAL);
        badgeLayout.setGravity(Gravity.CENTER);
        badgeLayout.setPadding(16, 0, 16, 0);

        TextView icon = new TextView(this);
        icon.setText(emojiIcon);
        icon.setTextSize(40f);
        icon.setGravity(Gravity.CENTER);
        icon.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

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
        if(profileManager != null && targetUserId != null && conquistasListener != null) {
            profileManager.removerConquistasListener(targetUserId, conquistasListener);
        }
    }
}
