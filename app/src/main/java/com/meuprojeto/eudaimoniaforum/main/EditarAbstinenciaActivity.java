package com.meuprojeto.eudaimoniaforum.main;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.meuprojeto.eudaimoniaforum.R;

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
    private ArrayAdapter<String> spinnerAdapter;
    private List<String> viciosList;
    
    private MainManager mainManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("EditarAbstinencia", "onCreate() chamado. Inicializando EditarAbstinenciaActivity.");
        setContentView(R.layout.perfil_timer_editar_activity);

        editTextDataInicio = findViewById(R.id.editTextDataInicio);
        spinnerVicioEdicao = findViewById(R.id.spinnerVicioEdicao);
        buttonSalvar = findViewById(R.id.buttonSalvar);

        calendar = Calendar.getInstance();
        mainManager = new MainManager();

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

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, viciosList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
                    int position = spinnerAdapter.getPosition(vicio);
                    if (position >= 0) {
                        spinnerVicioEdicao.setSelection(position);
                    }
                }
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(EditarAbstinenciaActivity.this, "Erro: " + erro, Toast.LENGTH_SHORT).show();
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

        String novoVicio = spinnerVicioEdicao.getSelectedItem().toString();
        
        mainManager.atualizarVicio(novoVicio, new MainManager.AcaoCallback() {
            @Override
            public void onSuccess() {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(EditarAbstinenciaActivity.this, "Alterações salvas com sucesso!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String erro) {
                if(isFinishing() || isDestroyed()) return;
                Toast.makeText(EditarAbstinenciaActivity.this, "Erro ao salvar: " + erro, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
