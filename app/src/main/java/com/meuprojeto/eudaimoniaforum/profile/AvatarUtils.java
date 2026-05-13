package com.meuprojeto.eudaimoniaforum.profile;

import android.content.Context;
import android.widget.ImageView;

public class AvatarUtils {

    public static final int TOTAL_AVATARES = 45;

    /**
     * Retorna o ID do recurso em drawable baseado na string do avatar salva no banco.
     * Exemplo: "ic_avatar_1" -> R.drawable.ic_avatar_1
     */
    public static int getAvatarDrawableId(Context context, String avatarName) {
        if (avatarName == null || avatarName.isEmpty() || !avatarName.startsWith("ic_avatar_")) {
            // Retorna um avatar padrão caso não tenha nada (ex: ic_menu_myplaces)
            return android.R.drawable.ic_menu_myplaces; 
        }

        int drawableId = context.getResources().getIdentifier(avatarName, "drawable", context.getPackageName());
        if (drawableId == 0) {
            return android.R.drawable.ic_menu_myplaces;
        }
        return drawableId;
    }

    /**
     * Aplica facilmente a imagem do avatar a um ImageView
     */
    public static void carregarAvatar(Context context, ImageView imageView, String avatarName) {
        if (imageView != null) {
            imageView.setImageResource(getAvatarDrawableId(context, avatarName));
            // As imagens padrao já sao da cor do app. Para os SVG novos pode manter originais,
            // ou se quiserem usar o #3F51B5 como tint padrao, a cor já tá dentro do XML (-path)
        }
    }
}
