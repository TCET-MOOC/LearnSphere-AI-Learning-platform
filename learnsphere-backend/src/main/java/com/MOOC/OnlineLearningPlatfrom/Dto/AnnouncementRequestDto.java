package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.time.LocalDateTime;

public class AnnouncementRequestDto {
    private String title;
    private String body;
    private String category;
    private String audience;
    private Boolean pinned;
    private Long courseId;
    private LocalDateTime expiresAt;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public Boolean getPinned() { return pinned; }
    public void setPinned(Boolean pinned) { this.pinned = pinned; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
