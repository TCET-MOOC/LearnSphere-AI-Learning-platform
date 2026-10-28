package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UserProgressResponseDto {
    private Long id;
    private Long lectureId;
    private BigDecimal progressPercent;
    private Integer secondsWatched;
    private LocalDateTime completedAt;

    public static UserProgressResponseDto from(UserProgress progress) {
        UserProgressResponseDto dto = new UserProgressResponseDto();
        dto.id = progress.getProgressId();
        dto.lectureId = progress.getLecture() != null ? progress.getLecture().getId() : null;
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

    public Long getId() { return id; }
    public Long getLectureId() { return lectureId; }
    public BigDecimal getProgressPercent() { return progressPercent; }
    public Integer getProgressPercentAsInt() { return progressPercent != null ? progressPercent.intValue() : null; }
    public Integer getSecondsWatched() { return secondsWatched; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
