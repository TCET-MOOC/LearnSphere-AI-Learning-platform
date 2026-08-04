package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List; // Import List

@Entity
@Table(name = "tests")
public class Test {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long testId;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    private String title;
    private Integer durationMinutes;
    private String securityPolicy;
    private LocalDateTime scheduledAt;
    private Boolean isRemedial = false;

    // --- NEW RELATIONSHIPS ---

    // A test has a list of questions. If a test is deleted, all its questions are also deleted.
    @JsonIgnore
    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL)
    private List<Question> questions;

    // A test can have many attempts by different users.
    @JsonIgnore
    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL)
    private List<TestAttempt> attempts;


    // --- Getters and Setters for all fields, including the new lists ---

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Long getCourseId() {
        return course != null ? course.getId() : null;
    }

    public Boolean getIsRemedial() {
        return isRemedial;
    }

    public void setIsRemedial(Boolean isRemedial) {
        this.isRemedial = isRemedial;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getSecurityPolicy() {
        return securityPolicy;
    }

    public void setSecurityPolicy(String securityPolicy) {
        this.securityPolicy = securityPolicy;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public List<TestAttempt> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<TestAttempt> attempts) {
        this.attempts = attempts;
    }
}