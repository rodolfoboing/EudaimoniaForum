package com.meuprojeto.eudaimoniaforum;

public class ChatMessage {

    private String messageText;
    private String senderId;
    private String receiverId;
    private long timestamp;
    private String status; // Novo campo: "enviado", "lido"

    public ChatMessage() {
        // Construtor vazio para Firebase
    }

    public ChatMessage(String messageText, String senderId, String receiverId, long timestamp) {
        this.messageText = messageText;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.timestamp = timestamp;
        this.status = "enviado"; // Por padrão, toda mensagem começa como "enviada"
    }

    // Getters e Setters
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
