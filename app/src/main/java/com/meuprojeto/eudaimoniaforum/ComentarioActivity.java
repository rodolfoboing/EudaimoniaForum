package com.meuprojeto.eudaimoniaforum;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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

    private String postId;
    private String autorDoPostId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tela_comentarios);

        recyclerViewComentarios = findViewById(R.id.recyclerViewComentarios);
        recyclerViewComentarios.setLayoutManager(new LinearLayoutManager(this));

        editTextComentario = findViewById(R.id.editTextComentario);
        buttonEnviarComentario = findViewById(R.id.buttonEnviarComentario);

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

        if (autorDoPostId != null) {
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (!autorDoPostId.equals(currentUserId)) {
                enviarNotificacaoParaAutor(autorDoPostId, conteudoComentario);
            }
        } else {
            // Fallback caso autorDoPostId seja null por algum motivo raro
            DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("forum/posts").child(postId);
            postRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Post post = snapshot.getValue(Post.class);
                    if (post != null) {
                        String autorPostId = post.getAutor();
                        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        if (!autorPostId.equals(currentUserId)) {
                            enviarNotificacaoParaAutor(autorPostId, conteudoComentario);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    private void enviarNotificacaoParaAutor(String autorPostId, String conteudoComentario) {
        String resumoComentario = conteudoComentario.length() > 30 ? conteudoComentario.substring(0, 30) + "..."
                : conteudoComentario;
        String mensagemNotificacao = "Novo comentário: " + resumoComentario;

        DatabaseReference notificacaoRef = FirebaseDatabase.getInstance().getReference("notificacoes")
                .child(autorPostId).push();
        String notifId = notificacaoRef.getKey();

        Notificacao notificacao = new Notificacao(notifId, "comentario", mensagemNotificacao, postId,
                System.currentTimeMillis());
        notificacaoRef.setValue(notificacao);
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
