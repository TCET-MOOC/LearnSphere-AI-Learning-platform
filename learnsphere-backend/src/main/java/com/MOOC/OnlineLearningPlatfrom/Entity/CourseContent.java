package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "course_content")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_id")
    private Long contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    @JsonIgnore
    private Module module;

    @JsonProperty("moduleId")
    public Long getModuleId() {
        return module != null ? module.getModuleId() : null;
    }

    @Column(name = "content_type", nullable = false)
    private String contentType; // VIDEO, PDF, or QUIZ

    @Column(name = "title")
    private String title;

    @Column(name = "file_path", columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "duration_seconds")
    private Integer durationSeconds = 0;

    @Column(name = "position")
    private Integer position = 0;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }
}
