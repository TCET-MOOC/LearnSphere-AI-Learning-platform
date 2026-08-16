package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A single royalty credit earned by a teacher (e.g. their share of a paid enrollment).
 * Rows accumulate over time; UserAccount.royaltyBalance tracks the running unpaid total,
 * which is zeroed out once a Payout is processed for the teacher.
 */
@Entity
@Table(name = "royalties")
@Data
@NoArgsConstructor
public class Royalty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private UserAccount teacher;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    /** Billing period this royalty falls in, e.g. "2026-08". */
    private String period;

    @Enumerated(EnumType.STRING)
    private Source source;

    private BigDecimal amount;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Source {
        EXTERNAL_SALES, COLLEGE_SHARE, REMEDIAL_CERTS
    }
}
