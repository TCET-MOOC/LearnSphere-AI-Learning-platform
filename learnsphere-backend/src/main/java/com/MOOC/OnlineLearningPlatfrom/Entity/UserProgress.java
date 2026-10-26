package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_progress")
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

    @Column(name = "progress_percent")
    private Integer progressPercent;

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

    public Integer getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Integer progressPercent) {
        this.progressPercent = progressPercent;
    }

    public Integer getSecondsWatched() {
        return secondsWatched;
    }

    public void setSecondsWatched(Integer secondsWatched) {
        this.secondsWatched = secondsWatched;
    }
}
