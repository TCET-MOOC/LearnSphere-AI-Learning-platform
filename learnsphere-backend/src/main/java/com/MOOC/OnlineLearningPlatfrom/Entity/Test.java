package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tests")
public class Test {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long testId;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "lecture_id")
    private Lecture lecture;

    @ManyToOne
    @JoinColumn(name = "content_id")
    private CourseContent content;

    private String title;
    private Integer durationMinutes;
    private String securityPolicy;
    private LocalDateTime scheduledAt;
    private Boolean isRemedial = false;
    private Integer maxAttempts = 3;
    private String placement = "POST_VIDEO";
    private Boolean requiredForCompletion = false;

    @Column(precision = 5, scale = 2)
    private BigDecimal passThreshold = new BigDecimal("60.00");

    private LocalDateTime createdAt;

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (maxAttempts == null) {
            maxAttempts = 3;
        }
        if (placement == null) {
            placement = "POST_VIDEO";
        }
        if (requiredForCompletion == null) {
            requiredForCompletion = false;
        }
        if (passThreshold == null) {
            passThreshold = new BigDecimal("60.00");
        }
    }

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

    public Lecture getLecture() {
        return lecture;
    }

    public void setLecture(Lecture lecture) {
        this.lecture = lecture;
    }

    public Long getLectureId() {
        return lecture != null ? lecture.getId() : null;
    }

    public CourseContent getContent() {
        return content;
    }

    public void setContent(CourseContent content) {
        this.content = content;
    }

    public Long getContentId() {
        return content != null ? content.getContentId() : null;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public String getPlacement() {
        return placement;
    }

    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public Boolean getRequiredForCompletion() {
        return requiredForCompletion;
    }

    public void setRequiredForCompletion(Boolean requiredForCompletion) {
        this.requiredForCompletion = requiredForCompletion;
    }

    public BigDecimal getPassThreshold() {
        return passThreshold;
    }

    public void setPassThreshold(BigDecimal passThreshold) {
        this.passThreshold = passThreshold;
    }

    public List<TestAttempt> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<TestAttempt> attempts) {
        this.attempts = attempts;
    }
}
