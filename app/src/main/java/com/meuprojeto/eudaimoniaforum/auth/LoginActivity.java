package com.meuprojeto.eudaimoniaforum.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meuprojeto.eudaimoniaforum.R;
import com.meuprojeto.eudaimoniaforum.main.MainActivity;
import com.meuprojeto.eudaimoniaforum.profile.SetupProfileActivity;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText editTextUsuario;
    private EditText editTextSenha;
    private Button buttonEntrar;
    private Button buttonCadastrar;
    private TextView textViewEsqueciSenha;
    private android.widget.CheckBox checkBoxMostrarSenhaLogin;

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d(TAG, "onCreate() chamado. Inicializando LoginActivity.");

        authManager = new AuthManager();

        authManager.verificarEstadoAutoLogin(new AuthManager.AutoLoginCallback() {
            @Override
            public void onIrParaMain() {
                if (!isFinishing() && !isDestroyed()) {
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                }
            }

            @Override
            public void onIrParaSetupPerfil() {
                if (!isFinishing() && !isDestroyed()) {
                    startActivity(new Intent(LoginActivity.this, SetupProfileActivity.class));
                    finish();
                }
            }

            @Override
            public void onPermaneceNoLogin() {
                if (!isFinishing() && !isDestroyed()) {
                    carregarInterfaceDeAutenticacao();
                }
            }
        });
    }

    private void carregarInterfaceDeAutenticacao() {
        setContentView(R.layout.auth_login_activity);
        inicializarUI();
        configurarListeners();
    }

    private void inicializarUI() {
        editTextUsuario = findViewById(R.id.editTextUsuario);
        editTextSenha = findViewById(R.id.editTextSenha);
        buttonEntrar = findViewById(R.id.buttonEntrar);
        buttonCadastrar = findViewById(R.id.buttonCadastrar);
        textViewEsqueciSenha = findViewById(R.id.textViewEsqueciSenha);
        checkBoxMostrarSenhaLogin = findViewById(R.id.checkBoxMostrarSenhaLogin);
    }

    private void configurarListeners() {
        buttonEntrar.setOnClickListener(v -> {
            String email = editTextUsuario.getText().toString().trim();
            String password = editTextSenha.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, getString(R.string.error_fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }
            
            realizarLogin(email, password);
        });

        checkBoxMostrarSenhaLogin.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int selectionStart = editTextSenha.getSelectionStart();
            int selectionEnd = editTextSenha.getSelectionEnd();
            if (isChecked) {
                editTextSenha.setTransformationMethod(android.text.method.HideReturnsTransformationMethod.getInstance());
            } else {
                editTextSenha.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
            }
            if(selectionStart >= 0) {
               editTextSenha.setSelection(selectionStart, selectionEnd);
            }
        });

        textViewEsqueciSenha.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RecoverPasswordActivity.class));
        });

        buttonCadastrar.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void realizarLogin(String email, String password) {
        authManager.login(email, password, new AuthManager.LoginCallback() {
            @Override
            public void onSuccess(boolean irParaSetup) {
                if (isFinishing() || isDestroyed()) return;
                
                if (irParaSetup) {
                    Toast.makeText(LoginActivity.this, getString(R.string.msg_setup_profile_hint), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, SetupProfileActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                } else {
                    Toast.makeText(LoginActivity.this, getString(R.string.msg_login_success), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                }
                finish();
            }

            @Override
            public void onUserBanned() {
                if (isFinishing() || isDestroyed()) return;
                
                Toast.makeText(LoginActivity.this, getString(R.string.msg_user_banned), Toast.LENGTH_SHORT).show();
                editTextSenha.setText("");
            }

            @Override
            public void onError(String errorMessage) {
                if (isFinishing() || isDestroyed()) return;
                
                editTextSenha.setText("");
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
