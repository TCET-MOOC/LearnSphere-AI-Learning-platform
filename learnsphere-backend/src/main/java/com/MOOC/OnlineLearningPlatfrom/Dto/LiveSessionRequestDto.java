package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.time.LocalDateTime;

public class LiveSessionRequestDto {
    private Long courseId;
    private String title;
    private LocalDateTime scheduledAt;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
}
