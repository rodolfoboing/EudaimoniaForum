package com.meuprojeto.eudaimoniaforum.notification;

public class Notificacao {
    private String id; // ID da própria notificação
    private String tipo; // "comentario" ou "chat"
    private String mensagem;
    private String idReferencia; // Pode ser o postId ou o chatId
    private long timestamp;
    private boolean lida; // Para futuras implementações

    public Notificacao() {
        // Construtor vazio para Firebase
    }

    public Notificacao(String id, String tipo, String mensagem, String idReferencia, long timestamp) {
        this.id = id;
        this.tipo = tipo;
        this.mensagem = mensagem;
        this.idReferencia = idReferencia;
        this.timestamp = timestamp;
        this.lida = false;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getIdReferencia() {
        return idReferencia;
    }

    public void setIdReferencia(String idReferencia) {
        this.idReferencia = idReferencia;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isLida() {
        return lida;
    }

    public void setLida(boolean lida) {
        this.lida = lida;
    }
}
