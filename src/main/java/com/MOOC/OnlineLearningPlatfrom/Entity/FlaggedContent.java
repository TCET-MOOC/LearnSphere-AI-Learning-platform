package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * A moderation flag raised against a piece of user-generated content (a discussion post
 * or a direct message). Rows are created either by the keyword-based auto-flagger
 * (see ModerationServiceImpl) or, in the future, by a user-facing "report" action.
 */
@Entity
@Table(name = "flagged_content")
@Data
@NoArgsConstructor
public class FlaggedContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ContentType contentType;

    private Long contentId;

    @Enumerated(EnumType.STRING)
    private Reason reason;

    private Long reporterId;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum ContentType {
        DISCUSSION_POST, MESSAGE
    }

    public enum Reason {
        BULLYING, SPAM, SUSPICIOUS, HIGH_RISK
    }

    public enum Status {
        PENDING, RESOLVED, DISMISSED
    }
}
