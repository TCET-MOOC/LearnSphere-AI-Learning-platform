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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "department")
    private String departmentLabel;

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
        if (department != null && department.getName() != null) {
            this.departmentLabel = department.getName();
        }
    }

    public void setDepartment(String departmentName) {
        this.departmentLabel = departmentName;
    }

    public String getDepartmentLabel() {
        if (departmentLabel != null) {
            return departmentLabel;
        }
        return department != null ? department.getName() : null;
    }

    public void setDepartmentLabel(String departmentLabel) {
        this.departmentLabel = departmentLabel;
    }

    @JsonProperty("departmentId")
    public Long getDepartmentId() {
        return department != null ? department.getDepartmentId() : null;
    }

    @JsonProperty("departmentId")
    public void setDepartmentId(Long departmentId) {
        if (departmentId != null) {
            Department d = new Department();
            d.setDepartmentId(departmentId);
            this.department = d;
        } else {
            this.department = null;
        }
    }

    @com.fasterxml.jackson.annotation.JsonSetter("department")
    public void setDepartmentFromJson(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || node.isNull()) {
            this.department = null;
            this.departmentLabel = null;
        } else if (node.isTextual()) {
            this.departmentLabel = node.asText();
        } else if (node.isObject()) {
            if (node.has("departmentId") && !node.get("departmentId").isNull()) {
                setDepartmentId(node.get("departmentId").asLong());
            }
            if (node.has("name") && !node.get("name").isNull()) {
                this.departmentLabel = node.get("name").asText();
            }
        }
    }

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
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public enum Status {
        DRAFT, PENDING, LIVE, ARCHIVED
    }
}

