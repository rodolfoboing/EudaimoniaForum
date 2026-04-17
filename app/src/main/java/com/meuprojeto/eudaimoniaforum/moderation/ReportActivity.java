package com.meuprojeto.eudaimoniaforum.moderation;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meuprojeto.eudaimoniaforum.R;

public class ReportActivity extends AppCompatActivity {

    private Spinner spinnerMotivo;
    private EditText editTextDetalhes;
    private Button buttonEnviarDenuncia;
    private String postId;
    private String comentarioId;
    private String tipo;
    private ModerationManager moderationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ReportActivity", "onCreate() chamado. Inicializando ReportActivity.");
        setContentView(R.layout.moderation_report_activity);

        postId = getIntent().getStringExtra("POST_ID");
        comentarioId = getIntent().getStringExtra("COMENTARIO_ID");
        tipo = getIntent().getStringExtra("TIPO");
        if (tipo == null) tipo = "post";

        if (postId == null) {
            finish();
            return;
        }

        moderationManager = new ModerationManager();
        spinnerMotivo = findViewById(R.id.spinnerMotivo);
        editTextDetalhes = findViewById(R.id.editTextDetalhes);
        buttonEnviarDenuncia = findViewById(R.id.buttonEnviarDenuncia);

        configurarSpinner();
        buttonEnviarDenuncia.setOnClickListener(v -> enviarDenuncia());
    }

    private void configurarSpinner() {
        String[] motivos = { "Conteúdo Ofensivo", "Spam", "Desinformação", "Assédio", "Outro" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, motivos);
        spinnerMotivo.setAdapter(adapter);
    }

    private void enviarDenuncia() {
        buttonEnviarDenuncia.setEnabled(false);
        buttonEnviarDenuncia.setText("Enviando...");

        String motivoSelecionado = spinnerMotivo.getSelectedItem().toString();
        String detalhes = editTextDetalhes.getText().toString().trim();
        String motivoFinal = motivoSelecionado + (TextUtils.isEmpty(detalhes) ? "" : ": " + detalhes);

        moderationManager.enviarDenuncia(postId, comentarioId, tipo, motivoFinal, new ModerationManager.FormDenunciaCallback() {
            @Override
            public void onSuccess() {
                if(isFinishing() || isDestroyed()) return;
                exibirDialogSucesso();
            }

            @Override
            public void onSpamDetectado() {
                if(isFinishing() || isDestroyed()) return;
                exibirDialogSpam();
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(ReportActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
                buttonEnviarDenuncia.setEnabled(true);
                buttonEnviarDenuncia.setText("Enviar Denúncia Novamente");
            }
        });
    }

    private void exibirDialogSpam() {
        android.app.Dialog dialog = new android.app.Dialog(ReportActivity.this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(ReportActivity.this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 40);
        layout.setGravity(android.view.Gravity.CENTER);

        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setCornerRadius(32f);
        shape.setColor(android.graphics.Color.parseColor("#1F2937"));
        layout.setBackground(shape);

        android.widget.TextView title = new android.widget.TextView(ReportActivity.this);
        title.setText("Aviso");
        title.setTextSize(20);
        title.setTextColor(android.graphics.Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(title);

        android.widget.TextView msg = new android.widget.TextView(ReportActivity.this);
        msg.setText("\nVocê já fez uma denúncia para este conteúdo.\n\nNossa equipe já recebeu sua avaliação. Não é necessário enviar novamente!");
        msg.setTextColor(android.graphics.Color.parseColor("#D1D5DB"));
        msg.setGravity(android.view.Gravity.CENTER);
        layout.addView(msg);

        com.google.android.material.button.MaterialButton btn = new com.google.android.material.button.MaterialButton(ReportActivity.this);
        btn.setText("VOLTAR");
        btn.setTextColor(android.graphics.Color.WHITE);
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#374151")));
        btn.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        layout.addView(btn);

        dialog.setContentView(layout);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }

    private void exibirDialogSucesso() {
        android.app.Dialog dialog = new android.app.Dialog(ReportActivity.this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(ReportActivity.this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 40);
        layout.setGravity(android.view.Gravity.CENTER);

        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setCornerRadius(32f);
        shape.setColor(android.graphics.Color.parseColor("#1F2937"));
        layout.setBackground(shape);

        android.widget.TextView title = new android.widget.TextView(ReportActivity.this);
        title.setText("Denúncia Registrada ✔️");
        title.setTextSize(20);
        title.setTextColor(android.graphics.Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(title);

        android.widget.TextView msg = new android.widget.TextView(ReportActivity.this);
        msg.setText("\nSua denúncia foi recebida com sucesso pela equipe de moderação.\n\nAgradecemos por ajudar a manter o ambiente seguro!");
        msg.setTextColor(android.graphics.Color.parseColor("#D1D5DB"));
        msg.setGravity(android.view.Gravity.CENTER);
        layout.addView(msg);

        com.google.android.material.button.MaterialButton btn = new com.google.android.material.button.MaterialButton(ReportActivity.this);
        btn.setText("CONCLUÍDO");
        btn.setTextColor(android.graphics.Color.WHITE);
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4B5563")));
        btn.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        layout.addView(btn);

        dialog.setContentView(layout);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }
}
