package com.meuprojeto.eudaimoniaforum.chat;

public class Conversation {

    // Informações do Outro Usuário
    private String otherUserId;
    private String otherUserNick;
    private boolean otherUserStatus; // true para ativo, false para inativo

    // Informações da Conversation
    private String chatId;
    private String lastMessage;
    private long lastMessageTimestamp;
    private int unreadCount;

    public Conversation() {
        // Construtor vazio para o Firebase
    }

    // Getters e Setters

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserNick() {
        return otherUserNick;
    }

    public void setOtherUserNick(String otherUserNick) {
        this.otherUserNick = otherUserNick;
    }

    public boolean isOtherUserStatus() {
        return otherUserStatus;
    }

    public void setOtherUserStatus(boolean otherUserStatus) {
        this.otherUserStatus = otherUserStatus;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
