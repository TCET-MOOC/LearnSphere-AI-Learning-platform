package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private DiscussionThread thread;

    @JsonProperty("threadId")
    public Long getThreadId() {
        return thread != null ? thread.getThreadId() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UserAccount user;

    @JsonProperty("userId")
    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }

    @JsonProperty("authorName")
    public String getAuthorName() {
        try {
            return user != null ? user.getFullName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @JsonProperty("authorAvatar")
    public String getAuthorAvatar() {
        try {
            return user != null ? user.getAvatarUrl() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_post_id")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Post parentPost;

    @Transient
    private Long parentPostId;

    @JsonProperty("parentPostId")
    public Long getParentPostId() {
        if (parentPost != null) {
            return parentPost.getPostId();
        }
        return parentPostId;
    }

    @JsonProperty("parentPostId")
    public void setParentPostId(Long parentPostId) {
        this.parentPostId = parentPostId;
    }

    @OneToMany(mappedBy = "parentPost", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<Post> replies = new ArrayList<>();

    @Column(name = "flagged")
    private Boolean flagged = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.flagged == null) {
            this.flagged = false;
        }
    }
}
