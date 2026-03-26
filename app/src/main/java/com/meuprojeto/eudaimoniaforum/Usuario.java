package com.meuprojeto.eudaimoniaforum;

public class Usuario {
    private String uid;
    private String nick;
    private String dataEntrada;
    private String sobreMim;
    private String vicio;
    private long lastLoginTimestamp; // Novo campo para o status
    private String fcmToken; // Token para notificações FCM
    private Long lastPostTimestamp;
    private String apresentacao;
    private java.util.Map<String, Boolean> postsComentados;
    private java.util.Map<String, Boolean> checkins;
    private java.util.Map<String, Boolean> posts;
    private java.util.Map<String, Boolean> conquistas;
    private Integer streakAtual;
    private String avatar;
    private Boolean perfilConfigurado;

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

    // Novo getter e setter para Token de Notificação
    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public Long getLastPostTimestamp() {
        return lastPostTimestamp;
    }

    public void setLastPostTimestamp(Long lastPostTimestamp) {
        this.lastPostTimestamp = lastPostTimestamp;
    }

    public String getApresentacao() {
        return apresentacao;
    }

    public void setApresentacao(String apresentacao) {
        this.apresentacao = apresentacao;
    }

    public java.util.Map<String, Boolean> getPostsComentados() {
        return postsComentados;
    }

    public void setPostsComentados(java.util.Map<String, Boolean> postsComentados) {
        this.postsComentados = postsComentados;
    }

    public java.util.Map<String, Boolean> getCheckins() {
        return checkins;
    }

    public void setCheckins(java.util.Map<String, Boolean> checkins) {
        this.checkins = checkins;
    }

    public java.util.Map<String, Boolean> getPosts() {
        return posts;
    }

    public void setPosts(java.util.Map<String, Boolean> posts) {
        this.posts = posts;
    }

    public java.util.Map<String, Boolean> getConquistas() {
        return conquistas;
    }

    public void setConquistas(java.util.Map<String, Boolean> conquistas) {
        this.conquistas = conquistas;
    }

    public Integer getStreakAtual() {
        return streakAtual;
    }

    public void setStreakAtual(Integer streakAtual) {
        this.streakAtual = streakAtual;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Boolean getPerfilConfigurado() {
        return perfilConfigurado;
    }

    public void setPerfilConfigurado(Boolean perfilConfigurado) {
        this.perfilConfigurado = perfilConfigurado;
    }
}
