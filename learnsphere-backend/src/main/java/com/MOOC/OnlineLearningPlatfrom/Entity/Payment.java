package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a (simulated) payment-gateway transaction for a paid course checkout.
 * No real gateway (Razorpay/Stripe) is configured for this project, so checkout/verify
 * simulate the order-creation + payment-confirmation lifecycle locally.
 */
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    private BigDecimal amount;

    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private String gatewayOrderId;

    private String gatewayPaymentId;

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Status {
        PENDING, SUCCESS, FAILED
    }
}
