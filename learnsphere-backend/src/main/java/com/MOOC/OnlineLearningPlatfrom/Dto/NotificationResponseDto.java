package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Notification;

import java.time.LocalDateTime;

public class NotificationResponseDto {
    private Long id;
    private String title;
    private String body;
    private String category;
    private Boolean read;
    private LocalDateTime createdAt;

    public static NotificationResponseDto from(Notification notification) {
        NotificationResponseDto dto = new NotificationResponseDto();
        dto.id = notification.getId();
        dto.title = notification.getTitle();
        dto.body = notification.getBody();
        dto.category = notification.getCategory();
        dto.read = notification.getRead();
        dto.createdAt = notification.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getCategory() { return category; }
    public Boolean getRead() { return read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
