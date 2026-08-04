package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A processed (or pending) transfer of a teacher's accrued royalty balance to their bank account. */
@Entity
@Table(name = "payouts")
@Data
@NoArgsConstructor
public class Payout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private UserAccount teacher;

    /** Billing period this payout covers, e.g. "2026-08". */
    private String period;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private LocalDateTime transferredAt;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING, PAID
    }
}
