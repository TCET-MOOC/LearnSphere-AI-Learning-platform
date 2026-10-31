package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Data
@NoArgsConstructor
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private UserAccount author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    private String category;

    private Boolean pinned = false;

    @Enumerated(EnumType.STRING)
    private Audience audience = Audience.ALL;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.pinned == null) {
            this.pinned = false;
        }
        if (this.audience == null) {
            this.audience = Audience.ALL;
        }
    }

    public Long getCourseId() {
        return course != null ? course.getCourseId() : null;
    }

    public enum Audience {
        ALL, STUDENT, TEACHER, ADMIN
    }
}
