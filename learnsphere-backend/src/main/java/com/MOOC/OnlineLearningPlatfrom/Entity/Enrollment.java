package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_enrollments_user_course_role", columnNames = {"user_id", "course_id", "role"})
})
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long enrollmentId;

    private Long userId;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    private LocalDateTime enrolledAt;

    @Column(name = "role", nullable = false)
    private String role = "STUDENT";

    @PrePersist
    public void prePersist() {
        if (enrolledAt == null) {
            enrolledAt = LocalDateTime.now();
        }
        if (role == null || role.isBlank()) {
            role = "STUDENT";
        }
    }

    // --- Getters and Setters ---

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
