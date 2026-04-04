package com.meuprojeto.eudaimoniaforum.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.meuprojeto.eudaimoniaforum.main.MainActivity;
import com.meuprojeto.eudaimoniaforum.R;

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
        setContentView(R.layout.onboarding_activity);

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
                "Seu espaço para recuperação e crescimento pessoal.\n\nAqui você encontra um Fórum de apoio, Chat Privado, Contador de Abstinência e Sistema de Conquistas para superar vícios."));

        onboardingItems.add(new OnboardingItem(
                android.R.drawable.ic_menu_edit,
                "Seu Contador de Abstinência",
                "Na tela principal, defina o vício que deseja superar.\n\nSe recair, use 'Novo Registro' para recomeçar: seu cronômetro e seus compromissos diários zeram juntos, sempre!"));

        onboardingItems.add(new OnboardingItem(
                android.R.drawable.checkbox_on_background,
                "Compromisso Diário",
                "Clique no '✅ Compromisso Diário' todo dia para gerar vitórias contínuas (⭐).\n\nO número de estrelas acompanha e valida seu tempo real, lhe garantindo as tão sonhadas Medalhas!"));

        onboardingItems.add(new OnboardingItem(
                android.R.drawable.ic_dialog_email,
                "Fórum e Chat Privado",
                "Use o Fórum para suporte coletivo e 'Conversas' para pedir ajuda em particular a alguém."));

        onboardingItems.add(new OnboardingItem(
                android.R.drawable.ic_menu_myplaces,
                "Segurança e Moderação",
                "Nossa comunidade virtual é blindada.\n\nVocê é encorajado a nos ajudar: denuncie posts ou comentários nocivos nos botões ao lado direito de cada Post ou Comentário e Bloqueie contas incômodas no seu próprio Chat."));

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
