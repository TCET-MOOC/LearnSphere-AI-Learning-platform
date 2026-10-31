package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Direct message within a conversation (W5-08).
 *
 * <p><b>Retention Note:</b> Individual messages are append-only and preserved for academic
 * integrity, audit, and dispute resolution purposes (default 1-year retention window).
 * Soft-deletion or moderation masking retains metadata for institutional compliance.</p>
 */
@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private UserAccount sender;

    @Column(columnDefinition = "TEXT")
    private String text;

    private LocalDateTime sentAt;

    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        this.sentAt = LocalDateTime.now();
    }
}
