package com.meuprojeto.eudaimoniaforum;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Post {
    private String id;
    private String titulo;
    private String resumo;
    private int numeroComentarios;
    private String autor;
    private long data;
    private String categoria;

    // Construtor vazio necessário para o Firebase
    public Post() {
    }

    public Post(String id, String titulo, String resumo, int numeroComentarios, String autor, long data, String categoria) {
        this.id = id;
        this.titulo = titulo;
        this.resumo = resumo;
        this.numeroComentarios = numeroComentarios;
        this.autor = autor;
        this.data = data;
        this.categoria = categoria;
    }

    // Getters
    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getResumo() { return resumo; }
    public String getAutor() { return autor; }
    public long getData() { return data; }
    public Integer getNumeroComentarios() { return numeroComentarios; }
    public String getCategoria() { return categoria; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setResumo(String resumo) { this.resumo = resumo; }
    public void setAutor(String autor) { this.autor = autor; }
    public void setNumeroComentarios(Integer numeroComentarios) { this.numeroComentarios = numeroComentarios; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    private static long parseDateString(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return 0L;
        }

        try {
            return Long.parseLong(dateString);
        } catch (NumberFormatException e) {
            // Não é um número, continua para os formatos de data
        }

        String[] patterns = {
                "EEE MMM dd HH:mm:ss zzz yyyy",
                "dd/MM/yyyy HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss",
                "dd/MM/yyyy",
                "MM/dd/yyyy"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
                Date date = format.parse(dateString);
                if (date != null) {
                    return date.getTime();
                }
            } catch (ParseException ex) {
                // Tenta o próximo formato
            }
        }

        // Se todas as tentativas falharem, registra o valor problemático.
        Log.d("PostDateConversion", "Could not parse date: '" + dateString + "'");
        return 0L;
    }

    public void setData(Object dataValue) {
        if (dataValue instanceof Long) {
            this.data = (Long) dataValue;
        } else if (dataValue instanceof String) {
            this.data = parseDateString((String) dataValue);
        } else {
            this.data = 0L;
        }
    }
}
