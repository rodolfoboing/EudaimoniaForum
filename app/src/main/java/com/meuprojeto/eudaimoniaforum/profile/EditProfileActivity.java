package com.meuprojeto.eudaimoniaforum.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.auth.LoginActivity;

public class EditProfileActivity extends AppCompatActivity {

    private EditText editTextNick;
    private EditText editTextApresentacao;
    private EditText editTextNovaSenha;
    private EditText editTextConfirmarNovaSenha;
    private Button buttonSalvarAlteracoes;
    private Button buttonExcluirConta;

    private ProfileManager profileManager;
    private String nickOriginal;
    private String avatarOriginal;
    private String avatarEscolhido;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("EditarPerfilAct", "onCreate() chamado. Inicializando EditProfileActivity.");
        setContentView(R.layout.profile_edit_activity);

        editTextNick = findViewById(R.id.editTextNick);
        editTextApresentacao = findViewById(R.id.editTextApresentacao);
        editTextNovaSenha = findViewById(R.id.editTextNovaSenha);
        editTextConfirmarNovaSenha = findViewById(R.id.editTextConfirmarNovaSenha);
        buttonSalvarAlteracoes = findViewById(R.id.buttonSalvarAlteracoes);
        buttonExcluirConta = findViewById(R.id.buttonExcluirConta);

        profileManager = new ProfileManager(this);

        if (profileManager.getCurrentUser() != null) {
            configurarAvatares();
            carregarDadosAtuais();
        } else {
            Toast.makeText(this, getString(R.string.error_unauthenticated), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        buttonSalvarAlteracoes.setOnClickListener(v -> validarESalvarAlteracoes());
        buttonExcluirConta.setOnClickListener(v -> confirmarExclusaoConta());
    }

    private void carregarDadosAtuais() {
        profileManager.carregarDadosAtuais(new ProfileManager.ProfileLoadListener() {
            @Override
            public void onProfileLoaded(String nick, String avatar) {
                if(isFinishing() || isDestroyed()) return;
                nickOriginal = nick;
                avatarOriginal = avatar;
                avatarEscolhido = avatarOriginal;
                destacarAvatarEscolhido(avatarEscolhido);
            }

            @Override
            public void onError(String error) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private interface PasswordCallback {
        void onPasswordEntered(String password);
    }

    private void mostrarDialogoSenha(String titulo, String mensagem, PasswordCallback callback) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(titulo);
        builder.setMessage(mensagem);

        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(getString(R.string.hint_current_password));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding / 2, padding, padding / 2);
        layout.addView(input);

        builder.setView(layout);

        builder.setPositiveButton(getString(R.string.confirmation_title), (dialog, which) -> {
            String senha = input.getText().toString().trim();
            if (TextUtils.isEmpty(senha)) {
                Toast.makeText(this, getString(R.string.error_password_required), Toast.LENGTH_SHORT).show();
            } else {
                callback.onPasswordEntered(senha);
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void confirmarExclusaoConta() {
        mostrarDialogoSenha(getString(R.string.title_delete_account_dialog),
                getString(R.string.msg_delete_account_dialog),
                senha -> deletarConta(senha));
    }

    private void deletarConta(String senha) {
        profileManager.deletarConta(senha, new ProfileManager.ProfileUpdateListener() {
            @Override
            public void onSuccess(String message) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(EditProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String error) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarAvatares() {
        LinearLayout layoutAvatares = findViewById(R.id.layoutAvatares);
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
                destacarAvatarEscolhido(nomeIcone);
            });

            layoutAvatares.addView(imgView);
        }
    }

    private void destacarAvatarEscolhido(String avatarId) {
        if (avatarId == null) return;
        LinearLayout layoutAvatares = findViewById(R.id.layoutAvatares);
        for (int i = 0; i < layoutAvatares.getChildCount(); i++) {
            View view = layoutAvatares.getChildAt(i);
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

    private void validarESalvarAlteracoes() {
        android.util.Log.d("EditarPerfilAct", "salvarAlteracoes() chamado: tentando salvar novas configurações do profile.");
        String novoNick = editTextNick.getText().toString().trim();
        String novaApresentacao = editTextApresentacao.getText().toString().trim();
        String novaSenha = editTextNovaSenha.getText().toString().trim();
        String confirmarNovaSenha = editTextConfirmarNovaSenha.getText().toString().trim();

        boolean avatarMudou = avatarEscolhido != null && !avatarEscolhido.equals(avatarOriginal);
        if (TextUtils.isEmpty(novoNick) && TextUtils.isEmpty(novaApresentacao) && TextUtils.isEmpty(novaSenha) && !avatarMudou) {
            Toast.makeText(this, getString(R.string.msg_no_changes), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!TextUtils.isEmpty(novaSenha)) {
            if (novaSenha.length() < 6) {
                Toast.makeText(this, "A nova senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!novaSenha.equals(confirmarNovaSenha)) {
                Toast.makeText(this, getString(R.string.error_passwords_do_not_match), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (novoNick.length() > 25) {
            Toast.makeText(this, getString(R.string.error_nick_too_long), Toast.LENGTH_SHORT).show();
            return;
        }

        if (novaApresentacao.length() > 160) {
            Toast.makeText(this, getString(R.string.error_bio_too_long), Toast.LENGTH_SHORT).show();
            return;
        }

        mostrarDialogoSenha(getString(R.string.title_confirm_changes), getString(R.string.msg_confirm_changes), senhaAtual -> {
            profileManager.salvarAlteracoes(senhaAtual, novoNick, nickOriginal, novaApresentacao, avatarEscolhido, avatarOriginal, novaSenha, new ProfileManager.ProfileUpdateListener() {
                @Override
                public void onSuccess(String message) {
                    if(isFinishing() || isDestroyed()) return;
                    Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                    if(!message.contains("Dados salvos, mas erro ao trocar senha")){
                        finish();
                    } else {
                       finish(); 
                    }
                }

                @Override
                public void onError(String error) {
                    if(isFinishing() || isDestroyed()) return;
                    Toast.makeText(EditProfileActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
