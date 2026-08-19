package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.LiveSession;

import java.time.LocalDateTime;

public class LiveSessionResponseDto {
    private Long id;
    private Long courseId;
    private String courseTitle;
    private String title;
    private LocalDateTime scheduledAt;
    private String status;
    private String joinUrl;

    public static LiveSessionResponseDto from(LiveSession session) {
        LiveSessionResponseDto dto = new LiveSessionResponseDto();
        dto.id = session.getId();
        if (session.getCourse() != null) {
            dto.courseId = session.getCourse().getId();
            dto.courseTitle = session.getCourse().getTitle();
        }
        dto.title = session.getTitle();
        dto.scheduledAt = session.getScheduledAt();
        dto.status = session.getStatus() != null ? session.getStatus().name() : null;
        dto.joinUrl = session.getJoinUrl();
        return dto;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public String getTitle() { return title; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public String getStatus() { return status; }
    public String getJoinUrl() { return joinUrl; }
}
