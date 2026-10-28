package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserProgressResponseDto {
    private Long id;
    private Long contentId;
    private Long lectureId;
    private BigDecimal progressPercent;
    private Integer secondsWatched;
    private LocalDateTime completedAt;

    public static UserProgressResponseDto from(UserProgress progress) {
        UserProgressResponseDto dto = new UserProgressResponseDto();
        dto.id = progress.getProgressId();
        if (progress.getContent() != null) {
            dto.contentId = progress.getContent().getContentId();
            if (progress.getContent().getLegacyLectureId() != null) {
                dto.lectureId = progress.getContent().getLegacyLectureId();
            }
        }
        if (progress.getLecture() != null) {
            dto.lectureId = progress.getLecture().getId();
        }
        dto.progressPercent = progress.getProgressPercent();
        dto.secondsWatched = progress.getSecondsWatched();
        dto.completedAt = progress.getCompletedAt();
        return dto;
    }

    public static UserProgressResponseDto empty(Long lectureId) {
        UserProgressResponseDto dto = new UserProgressResponseDto();
        dto.lectureId = lectureId;
        dto.progressPercent = BigDecimal.ZERO;
        dto.secondsWatched = 0;
        dto.completedAt = null;
        return dto;
    }

    public static UserProgressResponseDto emptyForContent(Long contentId, Long lectureId) {
        UserProgressResponseDto dto = new UserProgressResponseDto();
        dto.contentId = contentId;
        dto.lectureId = lectureId;
        dto.progressPercent = BigDecimal.ZERO;
        dto.secondsWatched = 0;
        dto.completedAt = null;
        return dto;
    }

    public Long getId() { return id; }
    public Long getContentId() { return contentId; }
    public Long getLectureId() { return lectureId; }
    public BigDecimal getProgressPercent() { return progressPercent; }
    public Integer getProgressPercentAsInt() { return progressPercent != null ? progressPercent.intValue() : null; }
    public Integer getSecondsWatched() { return secondsWatched; }
    public LocalDateTime getCompletedAt() { return completedAt; }

    public void setId(Long id) { this.id = id; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public void setLectureId(Long lectureId) { this.lectureId = lectureId; }
    public void setProgressPercent(BigDecimal progressPercent) { this.progressPercent = progressPercent; }
    public void setSecondsWatched(Integer secondsWatched) { this.secondsWatched = secondsWatched; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
