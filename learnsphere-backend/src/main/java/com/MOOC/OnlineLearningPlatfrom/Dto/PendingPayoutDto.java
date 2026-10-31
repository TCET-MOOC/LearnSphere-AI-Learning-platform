package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;

/**
 * Admin-facing view of a teacher who currently has an un-paid royalty balance.
 * "Pending" here is computed live from UserAccount.royaltyBalance rather than a stored
 * Payout row — a Payout row is only ever created once it's actually PAID (see PayoutAdminController).
 */
public class PendingPayoutDto {
    private Long teacherId;
    private String teacherName;
    private String avatarUrl;
    private BigDecimal pendingAmount;
    private long courseCount;
    private boolean eligibleForPayout;
    private BigDecimal minimumThreshold;

    public PendingPayoutDto(Long teacherId, String teacherName, String avatarUrl, BigDecimal pendingAmount, long courseCount, boolean eligibleForPayout, BigDecimal minimumThreshold) {
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.avatarUrl = avatarUrl;
        this.pendingAmount = pendingAmount;
        this.courseCount = courseCount;
        this.eligibleForPayout = eligibleForPayout;
        this.minimumThreshold = minimumThreshold;
    }

    public PendingPayoutDto(Long teacherId, String teacherName, String avatarUrl, BigDecimal pendingAmount, long courseCount) {
        this(teacherId, teacherName, avatarUrl, pendingAmount, courseCount,
                pendingAmount != null && pendingAmount.compareTo(new BigDecimal("500.00")) >= 0,
                new BigDecimal("500.00"));
    }

    public Long getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public String getAvatarUrl() { return avatarUrl; }
    public BigDecimal getPendingAmount() { return pendingAmount; }
    public long getCourseCount() { return courseCount; }
    public boolean isEligibleForPayout() { return eligibleForPayout; }
    public BigDecimal getMinimumThreshold() { return minimumThreshold; }
}
