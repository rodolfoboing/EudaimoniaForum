package com.meuprojeto.eudaimoniaforum;

public class Usuario {
    private String uid;
    private String nick;
    private String dataEntrada;
    private String sobreMim;
    private String vicio;
    private long lastLoginTimestamp; // Novo campo para o status

    public Usuario() {
        // Construtor vazio para o Firebase
    }

    public Usuario(String uid, String nick, String dataEntrada, String sobreMim, String vicio) {
        this.uid = uid;
        this.nick = nick;
        this.dataEntrada = dataEntrada;
        this.sobreMim = sobreMim;
        this.vicio = vicio;
        this.lastLoginTimestamp = System.currentTimeMillis(); // Define o timestamp no momento da criação
    }

    // Getters e Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getDataEntrada() {
        return dataEntrada;
    }

    public void setDataEntrada(String dataEntrada) {
        this.dataEntrada = dataEntrada;
    }

    public String getSobreMim() {
        return sobreMim;
    }

    public void setSobreMim(String sobreMim) {
        this.sobreMim = sobreMim;
    }

    public String getVicio() {
        return vicio;
    }

    public void setVicio(String vicio) {
        this.vicio = vicio;
    }

    public long getLastLoginTimestamp() {
        return lastLoginTimestamp;
    }

    public void setLastLoginTimestamp(long lastLoginTimestamp) {
        this.lastLoginTimestamp = lastLoginTimestamp;
    }
}
