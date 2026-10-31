package com.MOOC.OnlineLearningPlatfrom.commerce;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Story W6-08: Canonical 70/30 Royalty Policy
 * Rules: AC-21, audit.md B10, [F-F11], spec §6
 *
 * Defines the enterprise standard royalty split:
 * - 70% instructor royalty cut
 * - 30% platform infrastructure fee
 */
@Component
public class RoyaltyPolicy {

    public static final BigDecimal TEACHER_SHARE_RATE = new BigDecimal("0.70");
    public static final BigDecimal PLATFORM_SHARE_RATE = new BigDecimal("0.30");
    public static final int TEACHER_PERCENTAGE = 70;
    public static final int PLATFORM_PERCENTAGE = 30;

    /**
     * Calculates the instructor royalty portion from the gross paid amount (70%).
     */
    public BigDecimal calculateTeacherRoyalty(BigDecimal grossAmount) {
        if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return grossAmount.multiply(TEACHER_SHARE_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the platform fee portion from the gross paid amount (30%).
     */
    public BigDecimal calculatePlatformEarnings(BigDecimal grossAmount) {
        if (grossAmount == null || grossAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return grossAmount.subtract(calculateTeacherRoyalty(grossAmount)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTeacherShareRate() {
        return TEACHER_SHARE_RATE;
    }

    public BigDecimal getPlatformShareRate() {
        return PLATFORM_SHARE_RATE;
    }

    public int getTeacherPercentage() {
        return TEACHER_PERCENTAGE;
    }

    public int getPlatformPercentage() {
        return PLATFORM_PERCENTAGE;
    }
}

