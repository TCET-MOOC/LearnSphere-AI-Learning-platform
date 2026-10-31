package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Direct messaging conversation thread between two users (W5-08).
 *
 * <p><b>Retention Note:</b> Direct messages and conversation threads between participants
 * are retained for academic integrity, audit, and dispute resolution purposes. Rows are
 * preserved for the active duration of the term with automated archiving compliant with
 * institutional data retention policies (default 1-year retention window).</p>
 */
@Entity
@Table(name = "conversations")
@Data
@NoArgsConstructor
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "participant_a_id")
    private UserAccount participantA;

    @ManyToOne
    @JoinColumn(name = "participant_b_id")
    private UserAccount participantB;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(columnDefinition = "TEXT")
    private String lastMessage;

    private LocalDateTime lastMessageAt;

    @Column(name = "is_blocked")
    private Boolean isBlocked = false;

    @ManyToOne
    @JoinColumn(name = "blocked_by_user_id")
    private UserAccount blockedByUser;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isBlocked == null) {
            this.isBlocked = false;
        }
    }
}
