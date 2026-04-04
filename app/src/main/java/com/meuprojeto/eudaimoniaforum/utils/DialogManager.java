package com.meuprojeto.eudaimoniaforum.utils;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.meuprojeto.eudaimoniaforum.R;

public class DialogManager {

    public static void mostrarDialogoContato(Context context) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 40);
        layout.setGravity(Gravity.CENTER);
        
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(32f);
        shape.setColor(Color.parseColor("#1F2937")); // Cinza bem escuro
        layout.setBackground(shape);

        TextView title = new TextView(context);
        title.setText("Contato & Feedback");
        title.setTextSize(22);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText("\nPara dúvidas, sugestões ou suporte técnico geral, envie um e-mail direto para nossa equipe:\n");
        subtitle.setTextSize(15);
        subtitle.setTextColor(Color.parseColor("#D1D5DB")); // Cinza clarinho
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        TextView email = new TextView(context);
        email.setText("rodolfo.bm.reserva@gmail.com");
        email.setTextSize(16);
        email.setTextColor(Color.WHITE);
        email.setTypeface(null, Typeface.BOLD);
        email.setGravity(Gravity.CENTER);
        email.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);
        email.setMovementMethod(LinkMovementMethod.getInstance());
        email.setLinkTextColor(Color.WHITE); 
        layout.addView(email);

        View spacer = new View(context);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 40));
        layout.addView(spacer);

        MaterialButton btnPrivacy = new MaterialButton(context);
        btnPrivacy.setText("📜 Política de Privacidade");
        btnPrivacy.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#374151")));
        btnPrivacy.setTextColor(Color.WHITE);
        btnPrivacy.setCornerRadius(16);
        btnPrivacy.setOnClickListener(v -> {
            context.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://gist.github.com/rodolfoboing/c68da4a7504b78036166b44b11e8c7ee")));
        });
        layout.addView(btnPrivacy);

        MaterialButton btnFechar = new MaterialButton(context, null, com.google.android.material.R.attr.borderlessButtonStyle);
        btnFechar.setText("FECHAR");
        btnFechar.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams fecharParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fecharParams.setMargins(0, 20, 0, 0);
        btnFechar.setLayoutParams(fecharParams);
        btnFechar.setOnClickListener(v -> dialog.dismiss());
        layout.addView(btnFechar);

        dialog.setContentView(layout);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    (int) (context.getResources().getDisplayMetrics().widthPixels * 0.90),
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }
        dialog.show();
    }

    public static void exibirDialogConquista(Context context, String titulo, String mensagem, Runnable onVerPerfilClick) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.main_conquista_dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT
            );
        }

        TextView textTitulo = dialog.findViewById(R.id.textViewConquistaNome);
        TextView textMensagem = dialog.findViewById(R.id.textViewConquistaMensagem);
        textTitulo.setText(titulo);
        textMensagem.setText(mensagem);

        View dialogRoot = dialog.findViewById(android.R.id.content);
        if (dialogRoot != null) {
            Animation bounceIn = AnimationUtils.loadAnimation(context, R.anim.conquista_bounce_in);
            dialogRoot.startAnimation(bounceIn);
        }

        ImageView trofeu = dialog.findViewById(R.id.imageViewTrofeu);
        if (trofeu != null) {
            Animation pulse = AnimationUtils.loadAnimation(context, R.anim.pulse_trophy);
            trofeu.startAnimation(pulse);
        }

        View glow = dialog.findViewById(R.id.viewGlowBackground);
        if (glow != null) {
            ObjectAnimator rotation = ObjectAnimator.ofFloat(glow, "rotation", 0f, 360f);
            rotation.setDuration(8000);
            rotation.setRepeatCount(ObjectAnimator.INFINITE);
            rotation.setInterpolator(new LinearInterpolator());
            rotation.start();
        }

        dialog.findViewById(R.id.buttonVerPerfil).setOnClickListener(v -> {
            dialog.dismiss();
            if (onVerPerfilClick != null) {
                onVerPerfilClick.run();
            }
        });

        dialog.findViewById(R.id.textViewFechar).setOnClickListener(v -> dialog.dismiss());

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
