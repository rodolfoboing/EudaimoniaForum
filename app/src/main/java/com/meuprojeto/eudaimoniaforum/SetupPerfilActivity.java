package com.meuprojeto.eudaimoniaforum;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class SetupPerfilActivity extends AppCompatActivity {

    private ImageView imageViewAvatarPreview;
    private LinearLayout layoutAvatares;
    private TextView textViewNickAtual;
    private TextInputEditText editTextNovoNick;
    private TextInputEditText editTextSobreMim;
    private Button buttonSalvarPerfil;
    private TextView textViewPular;

    private DatabaseReference userRef;
    private String nickOriginal;
    private String avatarEscolhido;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("SetupPerfilActivity", "onCreate() chamado. Inicializando SetupPerfilActivity.");
        setContentView(R.layout.tela_setup_perfil);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Erro: Usuário não autenticado!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        inicializarUI();
        configurarAvatares();
        carregarDadosAtuais();

        buttonSalvarPerfil.setOnClickListener(v -> salvarPerfil());
        textViewPular.setOnClickListener(v -> pularSetup());
    }

    private void inicializarUI() {
        imageViewAvatarPreview = findViewById(R.id.imageViewAvatarPreview);
        layoutAvatares = findViewById(R.id.layoutAvatares);
        textViewNickAtual = findViewById(R.id.textViewNickAtual);
        editTextNovoNick = findViewById(R.id.editTextNovoNick);
        editTextSobreMim = findViewById(R.id.editTextSobreMim);
        buttonSalvarPerfil = findViewById(R.id.buttonSalvarPerfil);
        textViewPular = findViewById(R.id.textViewPular);
    }

    private void carregarDadosAtuais() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Usuario usuario = snapshot.getValue(Usuario.class);
                    if (usuario != null) {
                        nickOriginal = usuario.getNick();
                        textViewNickAtual.setText("Nick atual: " + nickOriginal);

                        if (usuario.getAvatar() != null) {
                            avatarEscolhido = usuario.getAvatar();
                            AvatarUtils.carregarAvatar(SetupPerfilActivity.this, imageViewAvatarPreview, avatarEscolhido);
                            destacarAvatarEscolhido(avatarEscolhido);
                        }

                        if (usuario.getSobreMim() != null && !usuario.getSobreMim().equals("Bem-vindo ao meu perfil!")) {
                            editTextSobreMim.setText(usuario.getSobreMim());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(SetupPerfilActivity.this, "Erro ao carregar dados.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarAvatares() {
        for (int i = 1; i <= AvatarUtils.TOTAL_AVATARES; i++) {
            final String nomeIcone = "ic_avatar_" + i;

            final ImageView imgView = new ImageView(this);
            int tamanho = (int) (65 * getResources().getDisplayMetrics().density);
            int margin = (int) (4 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(tamanho, tamanho);
            params.setMargins(margin, margin, margin, margin);
            imgView.setLayoutParams(params);

            int padding = (int) (12 * getResources().getDisplayMetrics().density);
            imgView.setPadding(padding, padding, padding, padding);

            int resId = AvatarUtils.getAvatarDrawableId(this, nomeIcone);
            imgView.setImageResource(resId);
            imgView.setTag(nomeIcone);

            imgView.setBackgroundResource(R.drawable.profile_circle_background);
            imgView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));

            imgView.setOnClickListener(v -> {
                avatarEscolhido = nomeIcone;
                AvatarUtils.carregarAvatar(SetupPerfilActivity.this, imageViewAvatarPreview, nomeIcone);
                destacarAvatarEscolhido(nomeIcone);
            });

            layoutAvatares.addView(imgView);
        }
    }

    private void destacarAvatarEscolhido(String avatarId) {
        if (avatarId == null) return;
        for (int i = 0; i < layoutAvatares.getChildCount(); i++) {
            android.view.View view = layoutAvatares.getChildAt(i);
            if (view instanceof ImageView) {
                ImageView img = (ImageView) view;
                if (avatarId.equals(img.getTag())) {
                    img.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E0F7FA")));
                    img.setElevation(4f);
                } else {
                    img.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
                    img.setElevation(0f);
                }
            }
        }
    }

    private void salvarPerfil() {
        String novoNick = editTextNovoNick.getText().toString().trim();
        String sobreMim = editTextSobreMim.getText().toString().trim();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        // Se quer mudar o nick, verificar duplicidade
        if (!TextUtils.isEmpty(novoNick) && !novoNick.equals(nickOriginal)) {
            DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference("usernames");
            usernamesRef.child(novoNick).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    Toast.makeText(this, "Este nick já está em uso. Escolha outro!", Toast.LENGTH_SHORT).show();
                } else {
                    aplicarMudancas(user, novoNick, sobreMim, true);
                }
            });
        } else {
            aplicarMudancas(user, nickOriginal, sobreMim, false);
        }
    }

    private void aplicarMudancas(FirebaseUser user, String nickFinal, String sobreMim, boolean mudouNick) {
        Map<String, Object> updates = new HashMap<>();

        if (mudouNick && nickFinal != null) {
            updates.put("users/" + user.getUid() + "/nick", nickFinal);
            updates.put("usernames/" + nickFinal, user.getUid());
            if (nickOriginal != null) {
                updates.put("usernames/" + nickOriginal, null);
            }
        }

        if (!TextUtils.isEmpty(sobreMim)) {
            updates.put("users/" + user.getUid() + "/sobreMim", sobreMim);
        }

        if (avatarEscolhido != null) {
            updates.put("users/" + user.getUid() + "/avatar", avatarEscolhido);
        }

        // Marcar perfil como configurado
        updates.put("users/" + user.getUid() + "/perfilConfigurado", true);

        if (!updates.isEmpty()) {
            FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Perfil salvo com sucesso! 🎉", Toast.LENGTH_SHORT).show();
                            navegarParaOnboarding();
                        } else {
                            Toast.makeText(this, "Erro ao salvar perfil.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Mesmo sem mudanças, marca como configurado
            userRef.child("perfilConfigurado").setValue(true);
            navegarParaOnboarding();
        }
    }

    private void pularSetup() {
        // Marca como configurado mesmo pulando para não perguntar de novo
        userRef.child("perfilConfigurado").setValue(true);
        navegarParaOnboarding();
    }

    private void navegarParaOnboarding() {
        // Independente da flag local no aparelho, se o usuário acabou de passar pelo SetupPerfil, 
        // significa que é uma conta nova (ou que nunca configurou o perfil), então DEVE ver o onboarding.
        Intent intent = new Intent(this, OnboardingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Impede o usuário de voltar (tem que configurar ou pular)
        Toast.makeText(this, "Configure seu perfil ou clique em 'Pular'.", Toast.LENGTH_SHORT).show();
    }
}
