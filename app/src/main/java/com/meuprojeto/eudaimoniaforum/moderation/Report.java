package com.meuprojeto.eudaimoniaforum.moderation;

public class Report {
    private String id;
    private String postId;
    private String motivo;
    private String denuncianteId;
    private long timestamp;
    private String status; // "pendente", "resolvido", "ignorado"

    private String tipo; // "post" ou "comentario"
    private String comentarioId; // Pode ser null se for post
    private String conteudoDenunciado; // Prévia do conteúdo reportado

    public Report() {
    }

    public Report(String id, String postId, String motivo, String denuncianteId, long timestamp) {
        this(id, postId, null, "post", motivo, denuncianteId, timestamp);
    }
    
    public Report(String id, String postId, String comentarioId, String tipo, String motivo, String denuncianteId, long timestamp) {
        this.id = id;
        this.postId = postId;
        this.comentarioId = comentarioId;
        this.tipo = tipo;
        this.motivo = motivo;
        this.denuncianteId = denuncianteId;
        this.timestamp = timestamp;
        this.status = "pendente"; // Padrão
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDenuncianteId() {
        return denuncianteId;
    }

    public void setDenuncianteId(String denuncianteId) {
        this.denuncianteId = denuncianteId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getComentarioId() {
        return comentarioId;
    }

    public void setComentarioId(String comentarioId) {
        this.comentarioId = comentarioId;
    }

    public String getConteudoDenunciado() {
        return conteudoDenunciado;
    }

    public void setConteudoDenunciado(String conteudoDenunciado) {
        this.conteudoDenunciado = conteudoDenunciado;
    }
}
