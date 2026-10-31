package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Story W6-09: Teacher Payout Statement DTO
 * Rules: A-22, BR-20, spec §4 SCR-55
 */
public class TeacherPayoutStatementDto {
    private String period;
    private Long teacherId;
    private String teacherName;
    private BigDecimal currentBalance;
    private BigDecimal minimumThreshold;
    private boolean eligibleForPayout;
    private BigDecimal thisMonthEarnings;
    private BigDecimal lifetimeEarnings;
    private List<PayoutResponseDto> payoutHistory;
    private LocalDateTime generatedAt;

    public TeacherPayoutStatementDto() {}

    public TeacherPayoutStatementDto(String period, Long teacherId, String teacherName,
                                     BigDecimal currentBalance, BigDecimal minimumThreshold,
                                     boolean eligibleForPayout, BigDecimal thisMonthEarnings,
                                     BigDecimal lifetimeEarnings, List<PayoutResponseDto> payoutHistory,
                                     LocalDateTime generatedAt) {
        this.period = period;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.currentBalance = currentBalance;
        this.minimumThreshold = minimumThreshold;
        this.eligibleForPayout = eligibleForPayout;
        this.thisMonthEarnings = thisMonthEarnings;
        this.lifetimeEarnings = lifetimeEarnings;
        this.payoutHistory = payoutHistory;
        this.generatedAt = generatedAt;
    }

    public String getPeriod() { return period; }
    public Long getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public BigDecimal getMinimumThreshold() { return minimumThreshold; }
    public boolean isEligibleForPayout() { return eligibleForPayout; }
    public BigDecimal getThisMonthEarnings() { return thisMonthEarnings; }
    public BigDecimal getLifetimeEarnings() { return lifetimeEarnings; }
    public List<PayoutResponseDto> getPayoutHistory() { return payoutHistory; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
}

