package com.meuprojeto.eudaimoniaforum.moderacao;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.meuprojeto.eudaimoniaforum.R;

public class DenunciaActivity extends AppCompatActivity {

    private Spinner spinnerMotivo;
    private EditText editTextDetalhes;
    private Button buttonEnviarDenuncia;
    private String postId;
    private String comentarioId;
    private String tipo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("DenunciaActivity", "onCreate() chamado. Inicializando DenunciaActivity.");
        setContentView(R.layout.moderacao_denuncia_activity);

        postId = getIntent().getStringExtra("POST_ID");
        comentarioId = getIntent().getStringExtra("COMENTARIO_ID");
        tipo = getIntent().getStringExtra("TIPO");
        if (tipo == null) tipo = "post";

        if (postId == null) {
            finish();
            return;
        }

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
        // Trava de segurança: impede duplo-clique e spam na mesma sessão
        buttonEnviarDenuncia.setEnabled(false);
        buttonEnviarDenuncia.setText("Enviando...");

        String motivoSelecionado = spinnerMotivo.getSelectedItem().toString();
        String detalhes = editTextDetalhes.getText().toString().trim();
        String motivoFinal = motivoSelecionado + (TextUtils.isEmpty(detalhes) ? "" : ": " + detalhes);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference denunciasRef = FirebaseDatabase.getInstance().getReference("denuncias");

        // Verifica se usuário já denunciou este item para evitar spam
        denunciasRef.orderByChild("postId").equalTo(postId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean jaDenunciou = false;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Denuncia d = ds.getValue(Denuncia.class);
                    if (d != null && userId.equals(d.getDenuncianteId())) {
                        if ("comentario".equals(tipo) && "comentario".equals(d.getTipo()) && comentarioId != null && comentarioId.equals(d.getComentarioId())) {
                            jaDenunciou = true;
                            break;
                        } else if ("post".equals(tipo) && ("post".equals(d.getTipo()) || d.getTipo() == null)) {
                            jaDenunciou = true;
                            break;
                        }
                    }
                }

                if (jaDenunciou) {
                    android.util.Log.d("DenunciaActivity", "Usuário já denunciou anteriormente. Abortando.");
                    
                    // Dialog Customizado Escuro para Aviso de Spam
                    android.app.Dialog dialog = new android.app.Dialog(DenunciaActivity.this);
                    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
                    android.widget.LinearLayout layout = new android.widget.LinearLayout(DenunciaActivity.this);
                    layout.setOrientation(android.widget.LinearLayout.VERTICAL);
                    layout.setPadding(60, 60, 60, 40);
                    layout.setGravity(android.view.Gravity.CENTER);
                    
                    android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
                    shape.setCornerRadius(32f);
                    shape.setColor(android.graphics.Color.parseColor("#1F2937"));
                    layout.setBackground(shape);

                    android.widget.TextView title = new android.widget.TextView(DenunciaActivity.this);
                    title.setText("Aviso");
                    title.setTextSize(20);
                    title.setTextColor(android.graphics.Color.WHITE);
                    title.setTypeface(null, android.graphics.Typeface.BOLD);
                    layout.addView(title);

                    android.widget.TextView msg = new android.widget.TextView(DenunciaActivity.this);
                    msg.setText("\nVocê já fez uma denúncia para este conteúdo.\n\nNossa equipe já recebeu sua avaliação. Não é necessário enviar novamente!");
                    msg.setTextColor(android.graphics.Color.parseColor("#D1D5DB"));
                    msg.setGravity(android.view.Gravity.CENTER);
                    layout.addView(msg);

                    com.google.android.material.button.MaterialButton btn = new com.google.android.material.button.MaterialButton(DenunciaActivity.this);
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

                } else {
                    String id = denunciasRef.push().getKey();
                    android.util.Log.d("DenunciaActivity", "Salvando nova denúncia: " + id);
                    Denuncia denuncia = new Denuncia(id, postId, comentarioId, tipo, motivoFinal, userId, System.currentTimeMillis());
                    denunciasRef.child(id).setValue(denuncia).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            android.util.Log.d("DenunciaActivity", "Denúncia salva com sucesso: " + id);
                            
                            // Dialog Customizado Escuro para Sucesso
                            android.app.Dialog dialog = new android.app.Dialog(DenunciaActivity.this);
                            dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
                            android.widget.LinearLayout layout = new android.widget.LinearLayout(DenunciaActivity.this);
                            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
                            layout.setPadding(60, 60, 60, 40);
                            layout.setGravity(android.view.Gravity.CENTER);
                            
                            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
                            shape.setCornerRadius(32f);
                            shape.setColor(android.graphics.Color.parseColor("#1F2937"));
                            layout.setBackground(shape);

                            android.widget.TextView title = new android.widget.TextView(DenunciaActivity.this);
                            title.setText("Denúncia Registrada ✔️");
                            title.setTextSize(20);
                            title.setTextColor(android.graphics.Color.WHITE);
                            title.setTypeface(null, android.graphics.Typeface.BOLD);
                            layout.addView(title);

                            android.widget.TextView msg = new android.widget.TextView(DenunciaActivity.this);
                            msg.setText("\nSua denúncia foi recebida com sucesso pela equipe de moderação.\n\nAgradecemos por ajudar a manter o ambiente seguro!");
                            msg.setTextColor(android.graphics.Color.parseColor("#D1D5DB"));
                            msg.setGravity(android.view.Gravity.CENTER);
                            layout.addView(msg);

                            com.google.android.material.button.MaterialButton btn = new com.google.android.material.button.MaterialButton(DenunciaActivity.this);
                            btn.setText("CONCLUÍDO");
                            btn.setTextColor(android.graphics.Color.WHITE); // Botão Branco!
                            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4B5563")));
                            btn.setOnClickListener(v -> {
                                dialog.dismiss();
                                finish();
                            });
                            layout.addView(btn);

                            dialog.setContentView(layout);
                            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                            dialog.show();

                        } else {
                            android.util.Log.e("DenunciaActivity", "Erro ao salvar denúncia no banco de dados.", task.getException());
                            Toast.makeText(DenunciaActivity.this, "Erro de conexão. Tente novamente.", Toast.LENGTH_SHORT).show();
                            buttonEnviarDenuncia.setEnabled(true);
                            buttonEnviarDenuncia.setText("Enviar Denúncia Novamente");
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        });
    }
}
