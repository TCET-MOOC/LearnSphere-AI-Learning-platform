package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_attempts")
public class TestAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attemptId;

    private Long userId;
    private BigDecimal score;
    private LocalDateTime attemptedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String status;

    @ManyToOne
    @JoinColumn(name = "test_id")
    @JsonIgnore
    private Test test;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnore
    private List<StudentAnswer> studentAnswers;

    @PrePersist
    public void onPrePersist() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
        if (attemptedAt == null) {
            attemptedAt = startedAt;
        }
        if (status == null) {
            status = "IN_PROGRESS";
        }
    }

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public void setScore(int score) { this.score = BigDecimal.valueOf(score); }
    public LocalDateTime getAttemptedAt() { return attemptedAt != null ? attemptedAt : startedAt; }
    public void setAttemptedAt(LocalDateTime attemptedAt) {
        this.attemptedAt = attemptedAt;
        if (this.startedAt == null) {
            this.startedAt = attemptedAt;
        }
    }
    public LocalDateTime getStartedAt() { return startedAt != null ? startedAt : attemptedAt; }
    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
        if (this.attemptedAt == null) {
            this.attemptedAt = startedAt;
        }
    }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }
    public List<StudentAnswer> getStudentAnswers() { return studentAnswers; }
    public void setStudentAnswers(List<StudentAnswer> studentAnswers) { this.studentAnswers = studentAnswers; }
}
