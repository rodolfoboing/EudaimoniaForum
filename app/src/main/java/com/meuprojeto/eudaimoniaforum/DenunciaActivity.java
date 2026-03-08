package com.meuprojeto.eudaimoniaforum;

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

public class DenunciaActivity extends AppCompatActivity {

    private Spinner spinnerMotivo;
    private EditText editTextDetalhes;
    private Button buttonEnviarDenuncia;
    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("DenunciaActivity", "onCreate() chamado. Inicializando DenunciaActivity.");
        setContentView(R.layout.activity_denuncia);

        postId = getIntent().getStringExtra("POST_ID");
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
        String motivoSelecionado = spinnerMotivo.getSelectedItem().toString();
        String detalhes = editTextDetalhes.getText().toString().trim();
        String motivoFinal = motivoSelecionado + (TextUtils.isEmpty(detalhes) ? "" : ": " + detalhes);

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference denunciasRef = FirebaseDatabase.getInstance().getReference("denuncias");

        // Verifica se usuário já denunciou este post para evitar spam
        denunciasRef.orderByChild("postId").equalTo(postId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean jaDenunciou = false;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Denuncia d = ds.getValue(Denuncia.class);
                    if (d != null && userId.equals(d.getDenuncianteId())) {
                        jaDenunciou = true;
                        break;
                    }
                }

                if (jaDenunciou) {
                    Toast.makeText(DenunciaActivity.this, "Você já denunciou este post.", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    String id = denunciasRef.push().getKey();
                    Denuncia denuncia = new Denuncia(id, postId, motivoFinal, userId, System.currentTimeMillis());
                    denunciasRef.child(id).setValue(denuncia).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(DenunciaActivity.this, "Denúncia enviada. Obrigado!", Toast.LENGTH_SHORT)
                                    .show();
                            finish();
                        } else {
                            Toast.makeText(DenunciaActivity.this, "Erro ao enviar denúncia.", Toast.LENGTH_SHORT)
                                    .show();
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
