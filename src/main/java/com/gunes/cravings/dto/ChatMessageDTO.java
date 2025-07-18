package com.gunes.cravings.dto;

import jakarta.validation.constraints.NotBlank; // Bu satırı ekle

public class ChatMessageDTO {
    private String sender;

    @NotBlank(message = "Mesaj içeriği boş olamaz.") // Bu anotasyonu ekle
    private String content;

    public ChatMessageDTO() {
    }

    public ChatMessageDTO(String sender, String content) {
        this.sender = sender;
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}