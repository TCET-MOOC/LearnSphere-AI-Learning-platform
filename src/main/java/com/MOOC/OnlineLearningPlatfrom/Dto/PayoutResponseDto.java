package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Payout;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayoutResponseDto {
    private Long id;
    private String period;
    private BigDecimal amount;
    private String status;
    private LocalDateTime transferredAt;
    private LocalDateTime createdAt;

    public static PayoutResponseDto from(Payout payout) {
        PayoutResponseDto dto = new PayoutResponseDto();
        dto.id = payout.getId();
        dto.period = payout.getPeriod();
        dto.amount = payout.getAmount();
        dto.status = payout.getStatus() != null ? payout.getStatus().name() : null;
        dto.transferredAt = payout.getTransferredAt();
        dto.createdAt = payout.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getPeriod() { return period; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    public LocalDateTime getTransferredAt() { return transferredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
