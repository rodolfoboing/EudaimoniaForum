package com.meuprojeto.eudaimoniaforum.chat;

public class ChatMessage {

    private String id;
    private String messageText;
    private String senderId;
    private String receiverId;
    private long timestamp;
    private String status; // Novo campo: "enviado", "lido"

    public ChatMessage() {
        // Construtor vazio para Firebase
    }

    public ChatMessage(String id, String messageText, String senderId, String receiverId, long timestamp) {
        this.id = id;
        this.messageText = messageText;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.status = "enviado"; // Por padrão, toda mensagem começa como "enviada"
    }

    public ChatMessage(String messageText, String senderId, String receiverId, long timestamp) {
        this(null, messageText, senderId, receiverId, timestamp);
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
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
}
