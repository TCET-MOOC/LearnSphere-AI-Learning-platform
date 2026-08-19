package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;

/**
 * Response returned by POST /api/payments/checkout — simulates the "order created"
 * response a real payment gateway (Razorpay/Stripe) would return before redirecting
 * the user into a hosted checkout UI.
 */
public class PaymentOrderDto {
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private Long courseId;

    public PaymentOrderDto(String orderId, BigDecimal amount, String currency, Long courseId) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.courseId = courseId;
    }

    public String getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Long getCourseId() { return courseId; }
}
