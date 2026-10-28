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

    @Column(name = "legacy_lecture_id")
    private Long legacyLectureId;

    @Column(name = "source_type")
    private String sourceType; // YOUTUBE, UPLOAD, EXTERNAL

    @Column(name = "provider_video_id")
    private String providerVideoId;

    @Column(name = "storage_key")
    private String storageKey;

    @Column(name = "external_url", columnDefinition = "TEXT")
    private String externalUrl;

    @Column(name = "media_status")
    private String mediaStatus = "READY"; // READY, PROCESSING, FAILED

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @PrePersist
    protected void onCreate() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }
}
