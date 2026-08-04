package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A scheduled/live/ended teacher-led live session for a course. There is no real video
 * infrastructure wired up in this project — joinUrl is a placeholder meeting link generated
 * when the session goes live.
 */
@Entity
@Table(name = "live_sessions")
@Data
@NoArgsConstructor
public class LiveSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private UserAccount teacher;

    private String title;

    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    private Status status = Status.SCHEDULED;

    private String joinUrl;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Status {
        SCHEDULED, LIVE, ENDED
    }
}
