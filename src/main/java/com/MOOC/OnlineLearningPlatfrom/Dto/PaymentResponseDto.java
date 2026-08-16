package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponseDto {
    private Long id;
    private Long courseId;
    private String courseTitle;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String gatewayOrderId;
    private String gatewayPaymentId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    public static PaymentResponseDto from(Payment payment) {
        PaymentResponseDto dto = new PaymentResponseDto();
        dto.id = payment.getId();
        if (payment.getCourse() != null) {
            dto.courseId = payment.getCourse().getId();
            dto.courseTitle = payment.getCourse().getTitle();
        }
        dto.amount = payment.getAmount();
        dto.currency = payment.getCurrency();
        dto.status = payment.getStatus() != null ? payment.getStatus().name() : null;
        dto.gatewayOrderId = payment.getGatewayOrderId();
        dto.gatewayPaymentId = payment.getGatewayPaymentId();
        dto.createdAt = payment.getCreatedAt();
        dto.paidAt = payment.getPaidAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public String getGatewayOrderId() { return gatewayOrderId; }
    public String getGatewayPaymentId() { return gatewayPaymentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
