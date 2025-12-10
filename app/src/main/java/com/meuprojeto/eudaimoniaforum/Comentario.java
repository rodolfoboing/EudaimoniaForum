package com.meuprojeto.eudaimoniaforum;

public class Comentario {
    private String id;
    private String postId;
    private String autor;
    private String conteudo;
    private String data;
    private long timestamp; // Novo campo

    public Comentario() {
        // Construtor vazio para o Firebase
    }

    // Construtor antigo mantido por compatibilidade, se necessário
    public Comentario(String autor, String conteudo, String data) {
        this.autor = autor;
        this.conteudo = conteudo;
        this.data = data;
        this.timestamp = System.currentTimeMillis(); // Padrão para dados antigos
    }

    // Novo construtor preferencial
    public Comentario(String autor, String conteudo, String data, long timestamp) {
        this.autor = autor;
        this.conteudo = conteudo;
        this.data = data;
        this.timestamp = timestamp;
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

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
