package com.meuprojeto.eudaimoniaforum;

public class Post {
    private String id;
    private String titulo;
    private String resumo;
    private int numeroComentarios;
    private String autor;
    private String data;
    private String categoria; // Novo campo

    // Construtor vazio necessário para o Firebase
    public Post() {
    }

    public Post(String id, String titulo, String resumo, int numeroComentarios, String autor, String data, String categoria) {
        this.id = id;
        this.titulo = titulo;
        this.resumo = resumo;
        this.numeroComentarios = numeroComentarios;
        this.autor = autor;
        this.data = data;
        this.categoria = categoria; // Novo campo
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getResumo() { return resumo; }
    public void setResumo(String resumo) { this.resumo = resumo; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public Integer getNumeroComentarios() { return numeroComentarios; }
    public void setNumeroComentarios(Integer numeroComentarios) { this.numeroComentarios = numeroComentarios; }

    public String getCategoria() { return categoria; } // Novo getter
    public void setCategoria(String categoria) { this.categoria = categoria; } // Novo setter
}