package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "course_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserAccount user;

    @JsonProperty("userId")
    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }

    @JsonProperty("userName")
    public String getUserName() {
        return user != null ? user.getFullName() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;

    @JsonProperty("courseId")
    public Long getCourseId() {
        return course != null ? course.getId() : null;
    }

    @Column(name = "rating", nullable = false)
    private Short rating; // 1 to 5

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
