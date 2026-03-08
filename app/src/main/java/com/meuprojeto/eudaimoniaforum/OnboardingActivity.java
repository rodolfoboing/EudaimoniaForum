package com.meuprojeto.eudaimoniaforum;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter onboardingAdapter;
    private LinearLayout layoutIndicators;
    private Button buttonNext;
    private Button buttonSkip;
    private ViewPager2 viewPagerOnboarding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        layoutIndicators = findViewById(R.id.layoutIndicators);
        buttonNext = findViewById(R.id.buttonNext);
        buttonSkip = findViewById(R.id.buttonSkip);
        viewPagerOnboarding = findViewById(R.id.viewPagerOnboarding);

        setupOnboardingItems();
        setupIndicators();
        setCurrentIndicator(0);

        viewPagerOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);
                if (position == onboardingAdapter.getItemCount() - 1) {
                    buttonNext.setText("Começar");
                } else {
                    buttonNext.setText("Próximo");
                }
            }
        });

        buttonNext.setOnClickListener(v -> {
            int currentItem = viewPagerOnboarding.getCurrentItem();
            if (currentItem + 1 < onboardingAdapter.getItemCount()) {
                viewPagerOnboarding.setCurrentItem(currentItem + 1);
            } else {
                finishOnboarding();
            }
        });

        buttonSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        onboardingItems.add(new OnboardingItem(
                R.drawable.ic_eudaimoniaforum, // App logo
                "Bem-vindo ao Eudaimonia",
                "Seu espaço para recuperação e crescimento pessoal.\n\nAqui você encontra um Fórum de apoio, Chat privado e ferramentas para superar vícios."));

        onboardingItems.add(new OnboardingItem(
                android.R.drawable.ic_menu_edit,
                "Seu Contador e Conquistas",
                "Na tela principal, defina o vício que deseja superar.\n\nUse 'Novo Registro' para iniciar/reiniciar, e 'Editar' para ajustar a data se necessário."));

        onboardingItems.add(new OnboardingItem(
                android.R.drawable.checkbox_on_background,
                "Compromisso Diário",
                "Clique no botão 'Compromisso Diário' todos os dias para reafirmar sua sobriedade.\n\nEssa ação valida seu progresso e desbloqueia Medalhas e Conquistas no seu perfil!"));

        onboardingItems.add(new OnboardingItem(
                android.R.drawable.ic_dialog_email,
                "Comunidade e Chat",
                "Acesse o Fórum para compartilhar, ajudar e ser ajudado.\n\nUse a aba 'Conversas' no rodapé para trocar mensagens privadas de incentivo com outros membros."));

        onboardingItems.add(new OnboardingItem(
                android.R.drawable.ic_menu_myplaces,
                "Menu e Perfil",
                "Acesse o Menu inferior para ver seu Perfil, acompanhar suas estatísticas e encontrar mais orientações.\n\nEstamos juntos nessa jornada!"));

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
        viewPagerOnboarding.setAdapter(onboardingAdapter);
    }

    private void setupIndicators() {
        ImageView[] indicators = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(8, 0, 8, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(), R.drawable.indicator_inactive));
            indicators[i].setLayoutParams(layoutParams);
            layoutIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int index) {
        int childCount = layoutIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutIndicators.getChildAt(i);
            if (i == index) {
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.indicator_active));
            } else {
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.indicator_inactive));
            }
        }
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences("PrefsAbstinencia", MODE_PRIVATE);
        prefs.edit().putBoolean("onboarding_complete", true).apply();

        // Verifica se veio do menu "Ajuda" (extra) ou é fluxo inicial
        boolean isHelpMode = getIntent().getBooleanExtra("IS_HELP_MODE", false);

        if (!isHelpMode) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        finish();
    }
}
