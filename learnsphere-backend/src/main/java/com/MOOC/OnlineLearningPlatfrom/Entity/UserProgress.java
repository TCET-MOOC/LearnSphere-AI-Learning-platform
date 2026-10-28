package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uq_user_progress_user_content", columnNames = {"user_id", "content_id"})
})
public class UserProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long progressId;

    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id")
    private CourseContent content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_id")
    private Lecture lecture;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "progress_percent", precision = 5, scale = 2)
    private BigDecimal progressPercent;

    @Column(name = "seconds_watched")
    private Integer secondsWatched;


    // --- Getters and Setters ---

    public Long getProgressId() {
        return progressId;
    }

    public void setProgressId(Long progressId) {
        this.progressId = progressId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public CourseContent getContent() {
        return content;
    }

    public void setContent(CourseContent content) {
        this.content = content;
    }

    public Lecture getLecture() {
        return lecture;
    }


    public void setLecture(Lecture lecture) {
        this.lecture = lecture;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public BigDecimal getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(BigDecimal progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Integer getProgressPercentAsInt() {
        return progressPercent != null ? progressPercent.intValue() : null;
    }

    public Integer getSecondsWatched() {
        return secondsWatched;
    }

    public void setSecondsWatched(Integer secondsWatched) {
        this.secondsWatched = secondsWatched;
    }
}
