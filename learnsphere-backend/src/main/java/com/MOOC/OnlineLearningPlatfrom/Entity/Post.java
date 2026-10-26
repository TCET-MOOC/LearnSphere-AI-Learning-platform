package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private DiscussionThread thread;

    @JsonProperty("threadId")
    public Long getThreadId() {
        return thread != null ? thread.getThreadId() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserAccount user;

    @JsonProperty("userId")
    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }

    @JsonProperty("authorName")
    public String getAuthorName() {
        return user != null ? user.getFullName() : null;
    }

    @JsonProperty("authorAvatar")
    public String getAuthorAvatar() {
        return user != null ? user.getAvatarUrl() : null;
    }

    @Column(name = "body", columnDefinition = "TEXT", nullable = false)
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_post_id")
    @JsonIgnore
    private Post parentPost;

    @JsonProperty("parentPostId")
    public Long getParentPostId() {
        return parentPost != null ? parentPost.getPostId() : null;
    }

    @OneToMany(mappedBy = "parentPost", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private List<Post> replies = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
