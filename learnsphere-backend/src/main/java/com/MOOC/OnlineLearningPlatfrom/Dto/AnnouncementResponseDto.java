package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Announcement;

import java.time.LocalDateTime;

public class AnnouncementResponseDto {
    private Long id;
    private String title;
    private String body;
    private Long authorId;
    private String authorName;
    private Long courseId;
    private String courseTitle;
    private String category;
    private Boolean pinned;
    private String audience;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public static AnnouncementResponseDto from(Announcement announcement) {
        AnnouncementResponseDto dto = new AnnouncementResponseDto();
        dto.id = announcement.getId();
        dto.title = announcement.getTitle();
        dto.body = announcement.getBody();
        if (announcement.getAuthor() != null) {
            dto.authorId = announcement.getAuthor().getUserId();
            try {
                dto.authorName = announcement.getAuthor().getFullName();
            } catch (Exception ignored) {}
        }
        if (announcement.getCourse() != null) {
            dto.courseId = announcement.getCourse().getCourseId();
            try {
                dto.courseTitle = announcement.getCourse().getTitle();
            } catch (Exception ignored) {}
        }
        dto.category = announcement.getCategory();
        dto.pinned = announcement.getPinned();
        dto.audience = announcement.getAudience() != null ? announcement.getAudience().name() : null;
        dto.expiresAt = announcement.getExpiresAt();
        dto.createdAt = announcement.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public Long getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public String getCategory() { return category; }
    public Boolean getPinned() { return pinned; }
    public String getAudience() { return audience; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
