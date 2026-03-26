package com.meuprojeto.eudaimoniaforum;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ComentarioActivity extends AppCompatActivity {

    private static final String TAG = "ComentarioActivity";

    private RecyclerView recyclerViewComentarios;
    private ComentarioAdapter comentarioAdapter;
    private List<Comentario> comentarios;
    private DatabaseReference comentariosRef;
    private boolean isModerador = false;
    private EditText editTextComentario;
    private Button buttonEnviarComentario;
    private ImageView iconMenuOpcoes;

    public static String activePostId = null;

    private String postId;
    private String autorDoPostId;
    private Boolean isAcompanhando = null; // null = status padrão (Autor segue, resto não)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ComentarioActivity", "onCreate() chamado. Inicializando ComentarioActivity.");
        setContentView(R.layout.tela_comentarios);

        recyclerViewComentarios = findViewById(R.id.recyclerViewComentarios);
        recyclerViewComentarios.setLayoutManager(new LinearLayoutManager(this));

        editTextComentario = findViewById(R.id.editTextComentario);
        buttonEnviarComentario = findViewById(R.id.buttonEnviarComentario);
        iconMenuOpcoes = findViewById(R.id.iconMenuOpcoes);

        comentarios = new ArrayList<>();

        postId = getIntent().getStringExtra("POST_ID");
        if (TextUtils.isEmpty(postId)) {
            Toast.makeText(this, "Erro: ID do post não encontrado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Comentários em coleção separada
        comentariosRef = FirebaseDatabase.getInstance().getReference("forum/comentarios").child(postId);

        // Inicia o fluxo de carregamento: Post -> Moderador -> Comentários
        carregarDadosDoPost();

        buttonEnviarComentario.setOnClickListener(v -> adicionarComentario());
        iconMenuOpcoes.setOnClickListener(v -> abrirMenuOpcoes());

        // Verifica se deve focar na caixa de comentários
        if (getIntent().getBooleanExtra("FOCUS_COMMENT", false)) {
            editTextComentario.requestFocus();
            // Opcional: Abrir teclado automaticamente (pode precisar de um pequeno delay)
            editTextComentario.postDelayed(() -> {
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                        android.content.Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editTextComentario, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }, 200);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        activePostId = postId;
        limparNotificacoesDePost();
    }

    @Override
    protected void onPause() {
        super.onPause();
        activePostId = null;
    }

    private void limparNotificacoesDePost() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || postId == null)
            return;

        Log.d(TAG, "Silenciador de Comentarios: Buscando alertas referentes ao Post " + postId);
        DatabaseReference notificacoesRef = FirebaseDatabase.getInstance().getReference("notificacoes")
                .child(user.getUid());

        // Apaga do nó todas as notificações que referenciam ESTE post exatamente
        notificacoesRef.orderByChild("idReferencia").equalTo(postId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int excluidos = 0;
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            ds.getRef().removeValue();
                            excluidos++;
                        }
                        Log.d(TAG, "Silenciador de Comentarios: Finalizado. " + excluidos
                                + " notificação(ões) relacionada(s) foram apagadas local/servidor.");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Silenciador de ComentariosErro na varredura " + error.getMessage());
                    }
                });
    }

    private void carregarDadosDoPost() {
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("forum/posts").child(postId);
        postRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Post post = snapshot.getValue(Post.class);
                if (post != null) {
                    autorDoPostId = post.getAutor();

                    // Atualiza Preview
                    TextView tvTitulo = findViewById(R.id.textViewTituloPreview);
                    TextView tvConteudo = findViewById(R.id.textViewConteudoPreview);
                    if (tvTitulo != null && tvConteudo != null) {
                        tvTitulo.setText(post.getTitulo());
                        tvConteudo.setText(post.getResumo());
                    }
                    verificarStatusNotificacao();
                }
                verificarModerador();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ComentarioActivity.this, "Erro ao carregar dados do post", Toast.LENGTH_SHORT).show();
                verificarModerador(); // Tenta continuar mesmo com erro
            }
        });
    }

    private void verificarModerador() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            carregarComentarios(); // Usuario não logado, carrega como visitante
            return;
        }

        String userId = user.getUid();
        DatabaseReference moderadoresRef = FirebaseDatabase.getInstance().getReference("moderadores").child(userId);

        moderadoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                isModerador = snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                carregarComentarios();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ComentarioActivity.this, "Erro ao verificar moderador", Toast.LENGTH_SHORT).show();
                carregarComentarios(); // Carrega mesmo com erro na verificação
            }
        });
    }

    private void carregarComentarios() {
        comentariosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                comentarios.clear();
                for (DataSnapshot comentarioSnapshot : snapshot.getChildren()) {
                    Comentario comentario = comentarioSnapshot.getValue(Comentario.class);
                    if (comentario != null) {
                        comentario.setId(comentarioSnapshot.getKey());
                        comentario.setPostId(postId);
                        comentarios.add(comentario);
                    }
                }
                // Passando autorDoPostId para o adapter
                comentarioAdapter = new ComentarioAdapter(comentarios, isModerador, ComentarioActivity.this,
                        autorDoPostId);
                recyclerViewComentarios.setAdapter(comentarioAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ComentarioActivity.this, "Erro ao carregar comentários", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void adicionarComentario() {
        String conteudoComentario = editTextComentario.getText().toString().trim();
        if (TextUtils.isEmpty(conteudoComentario)) {
            Toast.makeText(this, "Por favor, escreva um comentário", Toast.LENGTH_SHORT).show();
            return;
        }

        String comentarioId = comentariosRef.push().getKey();
        if (comentarioId != null) {
            String autor = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String data = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
            long timestamp = System.currentTimeMillis();

            // Usando o novo construtor com timestamp
            Comentario comentario = new Comentario(autor, conteudoComentario, data, timestamp);

            comentariosRef.child(comentarioId).setValue(comentario)
                    .addOnSuccessListener(aVoid -> {
                        editTextComentario.setText("");
                        incrementarNumeroComentarios();
                        verificarEEnviarNotificacao(postId, conteudoComentario);
                        salvarReferenciaPostComentado(autor, postId);
                    })
                    .addOnFailureListener(e -> Toast
                            .makeText(ComentarioActivity.this, "Erro ao enviar comentário", Toast.LENGTH_SHORT).show());
        }
    }

    private void salvarReferenciaPostComentado(String userId, String postId) {
        DatabaseReference userPostsComentadosRef = FirebaseDatabase.getInstance().getReference("users")
                .child(userId).child("postsComentados").child(postId);
        userPostsComentadosRef.setValue(true);
    }

    private void verificarEEnviarNotificacao(String postId, String conteudoComentario) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference seguidoresRef = FirebaseDatabase.getInstance().getReference("forum/posts")
                .child(postId).child("seguidores");

        seguidoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean autorGerenciado = false;

                // Envia para quem pediu explicitamente (e ignora quem marcou explicitamente
                // como false)
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String userId = ds.getKey();
                    Boolean status = ds.getValue(Boolean.class);

                    if (autorDoPostId != null && autorDoPostId.equals(userId)) {
                        autorGerenciado = true;
                    }

                    if (Boolean.TRUE.equals(status) && !userId.equals(currentUserId)) {
                        enviarNotificacaoParaUsuario(userId, conteudoComentario);
                    }
                }

                // Fallback para o autor se ele não tem preferência explícita configurada
                // (status null)
                if (!autorGerenciado && autorDoPostId != null && !autorDoPostId.equals(currentUserId)) {
                    enviarNotificacaoParaUsuario(autorDoPostId, conteudoComentario);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void enviarNotificacaoParaUsuario(String userId, String conteudoComentario) {
        String resumoComentario = conteudoComentario.length() > 30 ? conteudoComentario.substring(0, 30) + "..."
                : conteudoComentario;
        String mensagemNotificacao = "Novo comentário: " + resumoComentario;

        DatabaseReference notificacaoRef = FirebaseDatabase.getInstance().getReference("notificacoes")
                .child(userId).push();
        String notifId = notificacaoRef.getKey();

        Notificacao notificacao = new Notificacao(notifId, "comentario", mensagemNotificacao, postId,
                System.currentTimeMillis());
        notificacaoRef.setValue(notificacao);
    }

    private void verificarStatusNotificacao() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;

        DatabaseReference seguidorRef = FirebaseDatabase.getInstance().getReference("forum/posts")
                .child(postId).child("seguidores").child(user.getUid());

        seguidorRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isAcompanhando = snapshot.getValue(Boolean.class);
                } else {
                    isAcompanhando = null;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void abrirMenuOpcoes() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;

        boolean isOwner = autorDoPostId != null && autorDoPostId.equals(user.getUid());

        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, iconMenuOpcoes);
        popup.getMenuInflater().inflate(R.menu.menu_opcoes_postagem, popup.getMenu());

        boolean currentlyReceiving = false;
        if (isAcompanhando == null) {
            currentlyReceiving = isOwner;
        } else {
            currentlyReceiving = isAcompanhando;
        }
        popup.getMenu().findItem(R.id.action_acompanhar)
                .setTitle(currentlyReceiving ? "Silenciar Notificações" : "Acompanhar Notificações");

        if (isOwner) {
            popup.getMenu().findItem(R.id.action_denunciar).setVisible(false);
        }
        if (!isOwner && !isModerador) {
            popup.getMenu().findItem(R.id.action_excluir).setVisible(false);
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_acompanhar) {
                alternarNotificacao();
                return true;
            } else if (id == R.id.action_denunciar) {
                android.content.Intent intent = new android.content.Intent(ComentarioActivity.this,
                        DenunciaActivity.class);
                intent.putExtra("POST_ID", postId);
                startActivity(intent);
                return true;
            } else if (id == R.id.action_excluir) {
                excluirPostagem();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void excluirPostagem() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Excluir Postagem")
                .setMessage("Tem certeza que deseja excluir esta postagem?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    java.util.Map<String, Object> childUpdates = new java.util.HashMap<>();
                    
                    childUpdates.put("/forum/posts/" + postId, null);
                    childUpdates.put("/forum/comentarios/" + postId, null);
                    if (autorDoPostId != null) {
                        childUpdates.put("/users/" + autorDoPostId + "/posts/" + postId, null);
                    }

                    FirebaseDatabase.getInstance().getReference().updateChildren(childUpdates)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(ComentarioActivity.this, "Postagem excluída.", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void alternarNotificacao() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null)
            return;

        boolean currentlyReceiving = false;
        String curUser = user.getUid();

        if (isAcompanhando == null) {
            currentlyReceiving = (autorDoPostId != null && autorDoPostId.equals(curUser));
        } else {
            currentlyReceiving = isAcompanhando;
        }

        boolean willReceive = !currentlyReceiving;

        DatabaseReference seguidorRef = FirebaseDatabase.getInstance().getReference("forum/posts")
                .child(postId).child("seguidores").child(curUser);

        seguidorRef.setValue(willReceive);

        if (willReceive) {
            Toast.makeText(this, "Você será notificado sobre novos comentários.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Notificações silenciadas para esta postagem.", Toast.LENGTH_SHORT).show();
        }
    }

    private void incrementarNumeroComentarios() {
        DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("forum/posts").child(postId);
        postRef.child("numeroComentarios").runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @NonNull
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(
                    @NonNull com.google.firebase.database.MutableData currentData) {
                Integer currentCount = currentData.getValue(Integer.class);
                if (currentCount == null) {
                    currentData.setValue(1);
                } else {
                    currentData.setValue(currentCount + 1);
                }
                return com.google.firebase.database.Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                    @Nullable DataSnapshot currentData) {
            }
        });
    }
}
