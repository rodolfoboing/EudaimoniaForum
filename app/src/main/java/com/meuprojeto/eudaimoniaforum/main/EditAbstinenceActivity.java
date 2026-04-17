package com.meuprojeto.eudaimoniaforum.main;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meuprojeto.eudaimoniaforum.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditAbstinenceActivity extends AppCompatActivity {

    private com.google.android.material.textfield.TextInputEditText editTextDataInicio;
    private AutoCompleteTextView spinnerVicioEdicao;
    private Button buttonSalvar;

    private static final String PREFS_NAME = "PrefsAbstinencia";
    private static final String KEY_TEMPO_INICIAL = "tempo_inicial";

    private Calendar calendar;
    private ArrayAdapter<String> spinnerAdapter;
    private List<String> viciosList;
    
    private MainManager mainManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("EditarAbstinencia", "onCreate() chamado. Inicializando EditAbstinenceActivity.");
        setContentView(R.layout.profile_timer_edit_activity);

        editTextDataInicio = findViewById(R.id.editTextDataInicio);
        spinnerVicioEdicao = findViewById(R.id.spinnerVicioEdicao);
        buttonSalvar = findViewById(R.id.buttonSalvar);

        calendar = Calendar.getInstance();
        mainManager = new MainManager(this);

        if (mainManager.getCurrentUserId() == null) {
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

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, viciosList);
        spinnerVicioEdicao.setAdapter(spinnerAdapter);
    }

    private void carregarDadosDoUsuario() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long tempoSalvo = preferences.getLong(KEY_TEMPO_INICIAL, System.currentTimeMillis());
        calendar.setTimeInMillis(tempoSalvo);
        updateLabel();

        mainManager.carregarVicioDoUsuario(new MainManager.DadosUsuarioCallback() {
            @Override
            public void onVicioCarregado(String vicio) {
                if(isFinishing() || isDestroyed()) return;
                if (!vicio.isEmpty()) {
                    spinnerVicioEdicao.setText(vicio, false);
                }
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(EditAbstinenceActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
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
        android.util.Log.d("EditarAbstinencia", "salvarAlteracoes() chamado.");

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long novoTempo = calendar.getTimeInMillis();
        preferences.edit().putLong(KEY_TEMPO_INICIAL, novoTempo).apply();

        String novoVicio = spinnerVicioEdicao.getText().toString();
        
        mainManager.atualizarConfiguracaoAbstinencia(novoVicio, novoTempo, new MainManager.AcaoCallback() {
            @Override
            public void onSuccess() {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(EditAbstinenceActivity.this, "Alterações salvas com sucesso!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(EditAbstinenceActivity.this, "Erro ao salvar: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
