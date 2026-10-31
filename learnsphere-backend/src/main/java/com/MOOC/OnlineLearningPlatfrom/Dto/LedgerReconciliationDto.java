package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerReconciliationDto {
    private long totalPayments;
    private long paidPaymentsCount;
    private BigDecimal paidPaymentsAmount;
    private long pendingPaymentsCount;
    private BigDecimal pendingPaymentsAmount;
    private long failedPaymentsCount;
    private BigDecimal failedPaymentsAmount;

    private long totalRoyaltiesCount;
    private BigDecimal totalRoyaltiesAmount;

    private long paidEnrollmentsCount;
    private long discrepanciesCount;

    @JsonProperty("isReconciled")
    private boolean isReconciled;

    private LocalDateTime generatedAt;
}

