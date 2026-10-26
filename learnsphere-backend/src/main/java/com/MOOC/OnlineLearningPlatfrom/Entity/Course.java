package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonProperty("courseId")
    public Long getCourseId() {
        return id;
    }

    public void setCourseId(Long courseId) {
        this.id = courseId;
    }

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private UserAccount teacher;

    public UserAccount getCreator() {
        return teacher;
    }

    public void setCreator(UserAccount creator) {
        this.teacher = creator;
    }

    private String department;

    private String thumbnail;

    public String getImageUrl() {
        return thumbnail;
    }

    public void setImageUrl(String imageUrl) {
        this.thumbnail = imageUrl;
    }

    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    public Boolean getIsPublished() {
        return status == Status.LIVE;
    }

    public void setIsPublished(Boolean isPublished) {
        if (Boolean.TRUE.equals(isPublished)) {
            this.status = Status.LIVE;
        } else if (this.status == Status.LIVE) {
            this.status = Status.DRAFT;
        }
    }

    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @JsonIgnore
    private List<Module> modules = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Feedback> feedback = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<DiscussionThread> threads = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Status {
        DRAFT, PENDING, LIVE, ARCHIVED
    }
}

