package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "leaderboard", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "course_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Leaderboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leaderboard_id")
    private Long leaderboardId;

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

    @JsonProperty("userAvatar")
    public String getUserAvatar() {
        return user != null ? user.getAvatarUrl() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;

    @JsonProperty("courseId")
    public Long getCourseId() {
        return course != null ? course.getId() : null;
    }

    @Column(name = "avg_score", precision = 6, scale = 2)
    private BigDecimal avgScore = BigDecimal.ZERO;

    @Column(name = "global_rank")
    private Integer globalRank;

    @Column(name = "dept_rank")
    private Integer deptRank;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }
}
