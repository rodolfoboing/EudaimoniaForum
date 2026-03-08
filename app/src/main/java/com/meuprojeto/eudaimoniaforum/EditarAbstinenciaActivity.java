package com.meuprojeto.eudaimoniaforum;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditarAbstinenciaActivity extends AppCompatActivity {

    private EditText editTextDataInicio;
    private Spinner spinnerVicioEdicao;
    private Button buttonSalvar;

    private static final String PREFS_NAME = "PrefsAbstinencia";
    private static final String KEY_TEMPO_INICIAL = "tempo_inicial";

    private Calendar calendar;
    private DatabaseReference userRef;
    private ArrayAdapter<String> spinnerAdapter;
    private List<String> viciosList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("EditarAbstinencia", "onCreate() chamado. Inicializando EditarAbstinenciaActivity.");
        setContentView(R.layout.tela_edit_abstinencia);

        editTextDataInicio = findViewById(R.id.editTextDataInicio);
        spinnerVicioEdicao = findViewById(R.id.spinnerVicioEdicao);
        buttonSalvar = findViewById(R.id.buttonSalvar);

        calendar = Calendar.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        } else {
            Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupSpinner();
        carregarDadosDoUsuario();

        editTextDataInicio.setOnClickListener(v -> showDateTimePicker());

        buttonSalvar.setOnClickListener(v -> salvarAlteracoes());
    }

    private void setupSpinner() {
        viciosList = new ArrayList<>();
        viciosList.add("Pornografia");
        viciosList.add("Jogos de Azar");
        viciosList.add("Videogame");
        viciosList.add("Álcool");
        viciosList.add("Drogas");
        viciosList.add("Cigarro");

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, viciosList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerVicioEdicao.setAdapter(spinnerAdapter);
    }

    private void carregarDadosDoUsuario() {
        // Carrega tempo atual (abstinência) para o calendário não resetar
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long tempoSalvo = preferences.getLong(KEY_TEMPO_INICIAL, System.currentTimeMillis());
        calendar.setTimeInMillis(tempoSalvo);
        updateLabel();

        // Carrega o vício atual do Firebase
        userRef.child("vicio").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String vicioAtual = snapshot.getValue(String.class);
                    if (vicioAtual != null) {
                        int position = spinnerAdapter.getPosition(vicioAtual);
                        if (position >= 0) {
                            spinnerVicioEdicao.setSelection(position);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(EditarAbstinenciaActivity.this, "Erro ao carregar vício.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDateTimePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                updateLabel();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateLabel() {
        String myFormat = "dd/MM/yyyy HH:mm";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        editTextDataInicio.setText(sdf.format(calendar.getTime()));
    }

    private void salvarAlteracoes() {
        android.util.Log.d("EditarAbstinencia", "salvarAlteracoes() chamado: gravando dados localmente e no Firebase.");
        // 1. Salvar nova data de início
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit().putLong(KEY_TEMPO_INICIAL, calendar.getTimeInMillis()).apply();

        // 2. Salvar novo vício no Firebase
        String novoVicio = spinnerVicioEdicao.getSelectedItem().toString();
        userRef.child("vicio").setValue(novoVicio).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(EditarAbstinenciaActivity.this, "Alterações salvas com sucesso!", Toast.LENGTH_SHORT)
                        .show();
                finish();
            } else {
                Toast.makeText(EditarAbstinenciaActivity.this, "Erro ao salvar o vício.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
