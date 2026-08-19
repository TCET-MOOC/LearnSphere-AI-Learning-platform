package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Bookmark;

import java.time.LocalDateTime;

public class BookmarkResponseDto {
    private Long id;
    private Long lectureId;
    private String lectureTitle;
    private Long courseId;
    private String courseName;
    private Integer timestampSeconds;
    private String label;
    private LocalDateTime createdAt;

    public static BookmarkResponseDto from(Bookmark bookmark) {
        BookmarkResponseDto dto = new BookmarkResponseDto();
        dto.id = bookmark.getId();
        if (bookmark.getLecture() != null) {
            dto.lectureId = bookmark.getLecture().getId();
            dto.lectureTitle = bookmark.getLecture().getTitle();
            if (bookmark.getLecture().getCourse() != null) {
                dto.courseId = bookmark.getLecture().getCourse().getId();
                dto.courseName = bookmark.getLecture().getCourse().getTitle();
            }
        }
        dto.timestampSeconds = bookmark.getTimestampSeconds();
        dto.label = bookmark.getLabel();
        dto.createdAt = bookmark.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getLectureId() { return lectureId; }
    public String getLectureTitle() { return lectureTitle; }
    public Long getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public Integer getTimestampSeconds() { return timestampSeconds; }
    public String getLabel() { return label; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
