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
import com.meuprojeto.eudaimoniaforum.moderacao.DenunciaActivity;
import com.meuprojeto.eudaimoniaforum.R;

import java.util.List;

public class ComentarioActivity extends AppCompatActivity implements 
        ComentarioManager.ComentarioUpdateListener, 
        ComentarioNotificationHelper.NotificacaoStatusListener {

    private static final String TAG = "ComentarioActivity";

    private RecyclerView recyclerViewComentarios;
    private EditText editTextComentario;
    private Button buttonEnviarComentario;
    private ImageView iconMenuOpcoes;

    public static String activePostId = null;

    private String postId;
    private String currentUserId;
    
    // Status local temporário para interagir com o Menu
    private Boolean isAcompanhando = null; 

    private ComentarioManager comentarioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d(TAG, "onCreate() chamado. Inicializando ComentarioActivity.");
        setContentView(R.layout.forum_comentarios_activity);

        postId = getIntent().getStringExtra("POST_ID");
        if (TextUtils.isEmpty(postId)) {
            Toast.makeText(this, "Erro: ID do post não encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = (user != null) ? user.getUid() : null;

        inicializarUI();

        comentarioManager = new ComentarioManager(currentUserId, postId);
        comentarioManager.iniciarCarregamento(this);

        ComentarioNotificationHelper.verificarStatusNotificacao(currentUserId, postId, this);

        buttonEnviarComentario.setOnClickListener(v -> {
            String conteudo = editTextComentario.getText().toString().trim();
            if (TextUtils.isEmpty(conteudo)) {
                Toast.makeText(this, "Por favor, escreva um comentário", Toast.LENGTH_SHORT).show();
                return;
            }
            comentarioManager.adicionarComentario(conteudo, this);
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
        ComentarioNotificationHelper.limparNotificacoesDePost(currentUserId, postId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        activePostId = null;
    }

    // ComentarioManager.ComentarioUpdateListener

    @Override
    public void onPostLoaded(Post post) {
        if(isFinishing() || isDestroyed()) return;
        TextView tvTitulo = findViewById(R.id.textViewTituloPreview);
        TextView tvConteudo = findViewById(R.id.textViewConteudoPreview);
        if (tvTitulo != null && tvConteudo != null) {
            tvTitulo.setText(post.getTitulo());
            tvConteudo.setText(post.getResumo());
        }
    }

    @Override
    public void onPostLoadError(String error) {
        if(isFinishing() || isDestroyed()) return;
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onComentariosLoaded(List<Comentario> comentarios, boolean isModerador, String autorDoPostId) {
        if(isFinishing() || isDestroyed()) return;
        ComentarioAdapter adapter = new ComentarioAdapter(comentarios, isModerador, this, autorDoPostId);
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
        ComentarioNotificationHelper.verificarEEnviarNotificacao(currentUserId, comentarioManager.getAutorDoPostId(), postId, conteudoComentario);
    }

    @Override
    public void onActionSuccess(String message) {
        if(isFinishing() || isDestroyed()) return;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        if ("Postagem excluída.".equals(message)) {
            finish();
        }
    }

    @Override
    public void onActionFailure(String error) {
        if(isFinishing() || isDestroyed()) return;
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    // ComentarioNotificationHelper.NotificacaoStatusListener

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
        
        String autorId = comentarioManager.getAutorDoPostId();
        boolean isOwner = autorId != null && autorId.equals(currentUserId);
        boolean isModerador = comentarioManager.isModerador();

        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, iconMenuOpcoes);
        popup.getMenuInflater().inflate(R.menu.menu_opcoes_postagem, popup.getMenu());

        boolean currentlyReceiving = (isAcompanhando == null) ? isOwner : isAcompanhando;
        popup.getMenu().findItem(R.id.action_acompanhar).setTitle(currentlyReceiving ? "Silenciar Notificações" : "Acompanhar Notificações");

        if (isOwner) popup.getMenu().findItem(R.id.action_denunciar).setVisible(false);
        if (!isOwner && !isModerador) popup.getMenu().findItem(R.id.action_excluir).setVisible(false);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_acompanhar) {
                ComentarioNotificationHelper.alternarNotificacao(currentUserId, autorId, postId, isAcompanhando, this);
                return true;
            } else if (id == R.id.action_denunciar) {
                android.content.Intent intent = new android.content.Intent(this, DenunciaActivity.class);
                intent.putExtra("POST_ID", postId);
                startActivity(intent);
                return true;
            } else if (id == R.id.action_excluir) {
                confirmOp("Excluir Postagem", "Tem certeza que deseja excluir esta postagem?", () -> comentarioManager.excluirPostagem(this));
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
                .setPositiveButton("Sim", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Não", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (comentarioManager != null) {
            comentarioManager.removerListeners();
        }
    }
}
