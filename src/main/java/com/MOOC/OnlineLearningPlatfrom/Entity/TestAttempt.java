package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_attempts")
public class TestAttempt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long attemptId;

    private Long userId;
    private Integer score;
    private LocalDateTime attemptedAt;
    private String status;

    @ManyToOne
    @JoinColumn(name = "test_id")
    @JsonIgnore
    private Test test;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonIgnore
    private List<StudentAnswer> studentAnswers;

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public LocalDateTime getAttemptedAt() { return attemptedAt; }
    public void setAttemptedAt(LocalDateTime attemptedAt) { this.attemptedAt = attemptedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Test getTest() { return test; }
    public void setTest(Test test) { this.test = test; }
    public List<StudentAnswer> getStudentAnswers() { return studentAnswers; }
    public void setStudentAnswers(List<StudentAnswer> studentAnswers) { this.studentAnswers = studentAnswers; }
}
