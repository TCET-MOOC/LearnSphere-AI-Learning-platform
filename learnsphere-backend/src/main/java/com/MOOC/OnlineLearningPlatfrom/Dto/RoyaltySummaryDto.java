package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;

public class RoyaltySummaryDto {
    private BigDecimal thisMonthTotal;
    private BigDecimal totalEarned;
    private BigDecimal pendingPayout;
    private long externalEnrollments;

    public RoyaltySummaryDto(BigDecimal thisMonthTotal, BigDecimal totalEarned, BigDecimal pendingPayout, long externalEnrollments) {
        this.thisMonthTotal = thisMonthTotal;
        this.totalEarned = totalEarned;
        this.pendingPayout = pendingPayout;
        this.externalEnrollments = externalEnrollments;
    }

    public BigDecimal getThisMonthTotal() { return thisMonthTotal; }
    public BigDecimal getTotalEarned() { return totalEarned; }
    public BigDecimal getPendingPayout() { return pendingPayout; }
    public long getExternalEnrollments() { return externalEnrollments; }
}
