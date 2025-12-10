package com.meuprojeto.eudaimoniaforum;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class EditarPerfilActivity extends AppCompatActivity {

    private EditText editTextNick;
    private EditText editTextApresentacao;
    private EditText editTextNovaSenha;
    private EditText editTextSenhaAtual;
    private Button buttonSalvarAlteracoes;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference userRef;
    private String nickOriginal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_edit_perfil);

        editTextNick = findViewById(R.id.editTextNick);
        editTextApresentacao = findViewById(R.id.editTextApresentacao);
        editTextNovaSenha = findViewById(R.id.editTextNovaSenha);
        editTextSenhaAtual = findViewById(R.id.editTextSenhaAtual);
        buttonSalvarAlteracoes = findViewById(R.id.buttonSalvarAlteracoes);

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            carregarDadosAtuais();
        } else {
            Toast.makeText(this, "Erro: Usuário não autenticado!", Toast.LENGTH_SHORT).show();
            finish();
        }

        buttonSalvarAlteracoes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                salvarAlteracoes();
            }
        });
    }

    private void carregarDadosAtuais() {
        // Buscamos o nick original apenas para comparação interna.
        // Não preenchemos os EditTexts para manter os hints "Opcional" visíveis.
        userRef.child("nick").get().addOnSuccessListener(snapshot -> {
            nickOriginal = snapshot.getValue(String.class);
        });
    }

    private void salvarAlteracoes() {
        String novoNick = editTextNick.getText().toString().trim();
        String novaApresentacao = editTextApresentacao.getText().toString().trim();
        String novaSenha = editTextNovaSenha.getText().toString().trim();
        String senhaAtual = editTextSenhaAtual.getText().toString().trim();

        // Verifica se ao menos uma alteração foi solicitada
        if (TextUtils.isEmpty(novoNick) && TextUtils.isEmpty(novaApresentacao) && TextUtils.isEmpty(novaSenha)) {
            Toast.makeText(this, "Nenhuma alteração solicitada.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Exige senha atual para qualquer alteração
        if (TextUtils.isEmpty(senhaAtual)) {
            editTextSenhaAtual.setError("Senha atual é obrigatória para salvar alterações");
            editTextSenhaAtual.requestFocus();
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        // Reautenticação
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), senhaAtual);
        user.reauthenticate(credential).addOnCompleteListener(authTask -> {
            if (!authTask.isSuccessful()) {
                Toast.makeText(this, "Senha atual incorreta. Verifique e tente novamente.", Toast.LENGTH_SHORT).show();
            } else {
                // Senha correta, prosseguir
                processarAtualizacoes(user, novoNick, novaApresentacao, novaSenha);
            }
        });
    }

    private void processarAtualizacoes(FirebaseUser user, String novoNick, String novaApresentacao, String novaSenha) {
        // 1. Verificar alteração de Nick (só se o campo não estiver vazio)
        if (!TextUtils.isEmpty(novoNick) && !novoNick.equals(nickOriginal)) {
            DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference("usernames");
            usernamesRef.child(novoNick).get().addOnCompleteListener(nickTask -> {
                if (nickTask.isSuccessful() && nickTask.getResult().exists()) {
                    Toast.makeText(this, "Este nick já está em uso.", Toast.LENGTH_SHORT).show();
                } else {
                    // Nick livre
                    aplicarMudancas(user, novoNick, novaApresentacao, novaSenha, true);
                }
            });
        } else {
            // Nick não mudou ou campo vazio
            aplicarMudancas(user, nickOriginal, novaApresentacao, novaSenha, false);
        }
    }

    private void aplicarMudancas(FirebaseUser user, String nickFinal, String apresentacaoFinal, String novaSenha, boolean mudouNick) {
        Map<String, Object> updates = new HashMap<>();

        if (mudouNick) {
            updates.put("users/" + user.getUid() + "/nick", nickFinal);
            updates.put("usernames/" + nickFinal, user.getUid());
            if (nickOriginal != null) {
                updates.put("usernames/" + nickOriginal, null); // Remove nick antigo
            }
        }

        // Atualiza apresentação (sobreMim) SOMENTE se houver texto digitado
        if (!TextUtils.isEmpty(apresentacaoFinal)) {
            updates.put("users/" + user.getUid() + "/sobreMim", apresentacaoFinal);
        }

        if (!updates.isEmpty()) {
            FirebaseDatabase.getInstance().getReference().updateChildren(updates).addOnCompleteListener(dbTask -> {
                if (dbTask.isSuccessful()) {
                    atualizarSenhaSeNecessario(user, novaSenha);
                } else {
                    Toast.makeText(this, "Erro ao salvar dados no banco.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            atualizarSenhaSeNecessario(user, novaSenha);
        }
    }

    private void atualizarSenhaSeNecessario(FirebaseUser user, String novaSenha) {
        if (!TextUtils.isEmpty(novaSenha)) {
            user.updatePassword(novaSenha).addOnCompleteListener(passTask -> {
                if (passTask.isSuccessful()) {
                    Toast.makeText(this, "Perfil e senha atualizados com sucesso!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Dados salvos, mas erro ao trocar senha: " + passTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                }
            });
        } else {
            Toast.makeText(this, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
