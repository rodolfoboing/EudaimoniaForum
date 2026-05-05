package com.meuprojeto.eudaimoniaforum.auth;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.profile.SetupProfileActivity;

import java.util.ArrayList;
import java.util.List;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    private AuthManager authManager;

    private EditText editTextEmail;
    private EditText editTextNome;
    private Spinner spinnerVicio;
    private EditText editTextSenhaCadastro;
    private EditText editTextConfirmarSenhaCadastro;
    private android.widget.CheckBox checkBoxMostrarSenha;
    private Button buttonCadastrar;
    private android.widget.CheckBox checkBoxTermos;
    private android.widget.CheckBox checkBoxMaiorIdade;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d(TAG, "onCreate() chamado. Inicializando RegisterActivity.");
        setContentView(R.layout.auth_register_activity);

        authManager = new AuthManager();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextNome = findViewById(R.id.editTextNome);
        spinnerVicio = findViewById(R.id.spinnerVicio);
        editTextSenhaCadastro = findViewById(R.id.editTextSenhaCadastro);
        editTextConfirmarSenhaCadastro = findViewById(R.id.editTextConfirmarSenhaCadastro);
        checkBoxMostrarSenha = findViewById(R.id.checkBoxMostrarSenha);
        buttonCadastrar = findViewById(R.id.buttonCadastrar);
        checkBoxTermos = findViewById(R.id.checkBoxTermos);
        checkBoxMaiorIdade = findViewById(R.id.checkBoxMaiorIdade);

        configurarLinkDosTermos();
        setupSpinner();

        checkBoxMostrarSenha.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                editTextSenhaCadastro.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
                editTextConfirmarSenhaCadastro.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
            } else {
                editTextSenhaCadastro.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
                editTextConfirmarSenhaCadastro.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
            }
            editTextSenhaCadastro.setSelection(editTextSenhaCadastro.getText().length());
            editTextConfirmarSenhaCadastro.setSelection(editTextConfirmarSenhaCadastro.getText().length());
        });

        buttonCadastrar.setOnClickListener(v -> processarRegistro());
    }
    
    private void configurarLinkDosTermos() {
        String textoTermos = getString(R.string.msg_terms_consent_plain);
        android.text.SpannableString ss = new android.text.SpannableString(textoTermos);
        android.text.style.ClickableSpan clickableSpan = new android.text.style.ClickableSpan() {
            @Override
            public void onClick(@androidx.annotation.NonNull android.view.View widget) {
                android.content.Intent browserIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://gist.github.com/rodolfoboing/c68da4a7504b78036166b44b11e8c7ee"));
                startActivity(browserIntent);
            }

            @Override
            public void updateDrawState(@androidx.annotation.NonNull android.text.TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(android.graphics.Color.BLUE);
            }
        };

        // Encontrar a palavra "Termos de Uso e Política de Privacidade" ou adaptar o índice.
        // O texto extraído é "Li e concordo com os Termos de Uso e Política de Privacidade", com 60 chars.
        // Índice de "Termos de Uso" começa próximo ao 21.
        int startIndex = textoTermos.indexOf("Termos");
        if(startIndex == -1) startIndex = 21; // fallback
        
        ss.setSpan(clickableSpan, startIndex, textoTermos.length(), android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        checkBoxTermos.setText(ss);
        checkBoxTermos.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    private void setupSpinner() {
        String[] viciosArray = getResources().getStringArray(R.array.lista_vicios_registro);
        List<String> vicios = new ArrayList<>(java.util.Arrays.asList(viciosArray));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vicios);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVicio.setAdapter(adapter);
    }

    private void processarRegistro() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextSenhaCadastro.getText().toString().trim();
        String confirmPassword = editTextConfirmarSenhaCadastro.getText().toString().trim();
        String nickDesejado = editTextNome.getText().toString().trim();
        String vicioSelecionado = spinnerVicio.getSelectedItem().toString();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || nickDesejado.isEmpty()) {
            Toast.makeText(RegisterActivity.this, getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(RegisterActivity.this, getString(R.string.error_passwords_do_not_match), Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(RegisterActivity.this, "A senha deve ter pelo menos 6 caracteres.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkBoxTermos.isChecked()) {
            Toast.makeText(RegisterActivity.this, getString(R.string.error_accept_terms), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkBoxMaiorIdade.isChecked()) {
            Toast.makeText(RegisterActivity.this, getString(R.string.error_confirm_age), Toast.LENGTH_SHORT).show();
            return;
        }

        if (nickDesejado.length() < 3) {
            Toast.makeText(RegisterActivity.this, getString(R.string.error_nick_min_length), Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialog = new ProgressDialog(RegisterActivity.this);
        progressDialog.setMessage(getString(R.string.msg_checking_nick_register));
        progressDialog.setCancelable(false);
        progressDialog.show();

        authManager.verificarNickERegistrar(email, password, nickDesejado, vicioSelecionado, new AuthManager.RegisterCallback() {
            @Override
            public void onNickExists() {
                dispensarDialogo();
                Toast.makeText(RegisterActivity.this, getString(R.string.error_choose_another_nick), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess() {
                dispensarDialogo();
                Toast.makeText(RegisterActivity.this, getString(R.string.msg_user_registered_success), Toast.LENGTH_SHORT).show();
                irParaSetupPerfil();
            }

            @Override
            public void onError(String errorMessage) {
                dispensarDialogo();
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dispensarDialogo() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void irParaSetupPerfil() {
        Intent intent = new Intent(this, SetupProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
