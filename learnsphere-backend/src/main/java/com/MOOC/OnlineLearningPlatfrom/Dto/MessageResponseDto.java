package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Message;

import java.time.LocalDateTime;

public class MessageResponseDto {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String senderAvatarUrl;
    private String text;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;

    public static MessageResponseDto from(Message message) {
        MessageResponseDto dto = new MessageResponseDto();
        dto.id = message.getId();
        dto.conversationId = message.getConversation() != null ? message.getConversation().getId() : null;
        if (message.getSender() != null) {
            dto.senderId = message.getSender().getUserId();
            dto.senderName = message.getSender().getFullName();
            dto.senderAvatarUrl = message.getSender().getAvatarUrl();
        }
        dto.text = message.getText();
        dto.sentAt = message.getSentAt();
        dto.readAt = message.getReadAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public Long getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getSenderAvatarUrl() { return senderAvatarUrl; }
    public String getText() { return text; }
    public LocalDateTime getSentAt() { return sentAt; }
    public LocalDateTime getReadAt() { return readAt; }
}
