package com.meuprojeto.eudaimoniaforum.forum;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.meuprojeto.eudaimoniaforum.moderation.ReportActivity;
import com.meuprojeto.eudaimoniaforum.R;

import java.util.List;

public class CommentActivity extends AppCompatActivity implements
        CommentManager.ComentarioUpdateListener,
        CommentNotificationHelper.NotificacaoStatusListener {

    private static final String TAG = "CommentActivity";

    private RecyclerView recyclerViewComentarios;
    private EditText editTextComentario;
    private Button buttonEnviarComentario;
    private ImageView iconMenuOpcoes;

    public static String activePostId = null;

    private String postId;
    private String currentUserId;
    
    // Status local temporário para interagir com o Menu
    private Boolean isAcompanhando = null; 

    private CommentManager commentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d(TAG, "onCreate() chamado. Inicializando CommentActivity.");
        setContentView(R.layout.forum_comment_activity);

        postId = getIntent().getStringExtra("POST_ID");
        if (TextUtils.isEmpty(postId)) {
            Toast.makeText(this, getString(R.string.error_post_id_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : null;

        inicializarUI();

        commentManager = new CommentManager(currentUserId, postId);
        commentManager.iniciarCarregamento(this);

        CommentNotificationHelper.verificarStatusNotificacao(currentUserId, postId, this);

        buttonEnviarComentario.setOnClickListener(v -> {
            String conteudo = editTextComentario.getText().toString().trim();
            if (TextUtils.isEmpty(conteudo)) {
                Toast.makeText(this, getString(R.string.error_empty_comment), Toast.LENGTH_SHORT).show();
                return;
            }
            commentManager.adicionarComentario(conteudo, this);
        });

        iconMenuOpcoes.setOnClickListener(v -> abrirMenuOpcoes());

        if (getIntent().getBooleanExtra("FOCUS_COMMENT", false)) {
            editTextComentario.requestFocus();
            editTextComentario.postDelayed(() -> {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                        android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(editTextComentario, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }, 200);
        }
    }

    private void inicializarUI() {
        recyclerViewComentarios = findViewById(R.id.recyclerViewComentarios);
        recyclerViewComentarios.setLayoutManager(new LinearLayoutManager(this));
        editTextComentario = findViewById(R.id.editTextComentario);
        buttonEnviarComentario = findViewById(R.id.buttonEnviarComentario);
        iconMenuOpcoes = findViewById(R.id.iconMenuOpcoes);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activePostId = postId;
        CommentNotificationHelper.limparNotificacoesDePost(currentUserId, postId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        activePostId = null;
    }

    // CommentManager.ComentarioUpdateListener

    @Override
    public void onPostLoaded(Post post) {
        if(isFinishing() || isDestroyed()) return;
        TextView tvTitulo = findViewById(R.id.textViewTituloPreview);
        TextView tvConteudo = findViewById(R.id.textViewConteudoPreview);
        if (tvTitulo != null && tvConteudo != null) {
            tvTitulo.setText(post.getTitulo());
            tvConteudo.setText(post.getResumo());

            android.view.View.OnClickListener expandListener = v -> {
                TextView tv = (TextView) v;
                int currentMax = tv.getMaxLines();
                if (currentMax == Integer.MAX_VALUE || currentMax == -1) {
                    tv.setMaxLines(tv.getId() == R.id.textViewTituloPreview ? 2 : 3);
                    tv.setEllipsize(android.text.TextUtils.TruncateAt.END);
                } else {
                    tv.setMaxLines(Integer.MAX_VALUE);
                    tv.setEllipsize(null);
                }
            };

            tvTitulo.setOnClickListener(expandListener);
            tvConteudo.setOnClickListener(expandListener);
        }
    }

    @Override
    public void onPostLoadError(String error) {
        if(isFinishing() || isDestroyed()) return;
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onComentariosLoaded(List<Comment> comments, boolean isModerador, String autorDoPostId) {
        if(isFinishing() || isDestroyed()) return;
        CommentAdapter adapter = new CommentAdapter(comments, isModerador, this, autorDoPostId);
        recyclerViewComentarios.setAdapter(adapter);
    }

    @Override
    public void onComentariosLoadError(String error) {
        if(isFinishing() || isDestroyed()) return;
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCommentAdded(String conteudoComentario) {
        if(isFinishing() || isDestroyed()) return;
        editTextComentario.setText("");
        CommentNotificationHelper.verificarEEnviarNotificacao(currentUserId, commentManager.getAutorDoPostId(), postId, conteudoComentario);
    }

    @Override
    public void onActionSuccess(String message) {
        if(isFinishing() || isDestroyed()) return;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        // A checagem abaixo é um anti-pattern baseado em string human-readable. 
        // Foi mantido provisoriamente pois demandaria ajustar a interface e as outras passagens do ChatManager/CommentManager.
        if (message.equals(getString(R.string.msg_post_deleted)) || message.contains("excluída")) {
            finish();
        }
    }

    @Override
    public void onActionFailure(String error) {
        if(isFinishing() || isDestroyed()) return;
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    // CommentNotificationHelper.NotificacaoStatusListener

    @Override
    public void onStatusLoaded(Boolean isAcompanhando) {
        if(isFinishing() || isDestroyed()) return;
        this.isAcompanhando = isAcompanhando;
    }

    @Override
    public void onStatusToggled(boolean willReceive, String message) {
        if(isFinishing() || isDestroyed()) return;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Outros métodos e menus

    private void abrirMenuOpcoes() {
        if (currentUserId == null) return;
        
        String autorId = commentManager.getAutorDoPostId();
        boolean isOwner = autorId != null && autorId.equals(currentUserId);
        boolean isModerador = commentManager.isModerador();

        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, iconMenuOpcoes);
        popup.getMenuInflater().inflate(R.menu.menu_opcoes_postagem, popup.getMenu());

        boolean currentlyReceiving = (isAcompanhando == null) ? isOwner : isAcompanhando;
        popup.getMenu().findItem(R.id.action_acompanhar).setTitle(currentlyReceiving ? getString(R.string.action_mute_notifications) : getString(R.string.action_follow_notifications));

        if (isOwner) popup.getMenu().findItem(R.id.action_denunciar).setVisible(false);
        if (!isOwner && !isModerador) popup.getMenu().findItem(R.id.action_excluir).setVisible(false);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_acompanhar) {
                CommentNotificationHelper.alternarNotificacao(currentUserId, autorId, postId, isAcompanhando, this);
                return true;
            } else if (id == R.id.action_denunciar) {
                android.content.Intent intent = new android.content.Intent(this, ReportActivity.class);
                intent.putExtra("POST_ID", postId);
                startActivity(intent);
                return true;
            } else if (id == R.id.action_excluir) {
                confirmOp(getString(R.string.action_delete_post), getString(R.string.msg_confirm_delete_post), () -> commentManager.excluirPostagem(this));
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void confirmOp(String title, String message, Runnable onConfirm) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_yes, (dialog, which) -> onConfirm.run())
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (commentManager != null) {
            commentManager.removerListeners();
        }
    }
}
