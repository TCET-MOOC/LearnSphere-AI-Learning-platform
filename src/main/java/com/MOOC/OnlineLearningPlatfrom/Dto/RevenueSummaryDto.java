package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;
import java.util.List;

/** Real revenue breakdown computed from SUCCESS Payment rows. */
public class RevenueSummaryDto {
    private BigDecimal totalRevenue;
    private BigDecimal platformEarnings;
    private BigDecimal teacherRoyalties;
    private int platformCutPercent;
    private int teacherRoyaltyPercent;
    private List<CourseRevenueDto> byCourse;
    private List<MonthlyRevenueDto> byMonth;

    public RevenueSummaryDto(BigDecimal totalRevenue, BigDecimal platformEarnings, BigDecimal teacherRoyalties,
                              int platformCutPercent, int teacherRoyaltyPercent,
                              List<CourseRevenueDto> byCourse, List<MonthlyRevenueDto> byMonth) {
        this.totalRevenue = totalRevenue;
        this.platformEarnings = platformEarnings;
        this.teacherRoyalties = teacherRoyalties;
        this.platformCutPercent = platformCutPercent;
        this.teacherRoyaltyPercent = teacherRoyaltyPercent;
        this.byCourse = byCourse;
        this.byMonth = byMonth;
    }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public BigDecimal getPlatformEarnings() { return platformEarnings; }
    public BigDecimal getTeacherRoyalties() { return teacherRoyalties; }
    public int getPlatformCutPercent() { return platformCutPercent; }
    public int getTeacherRoyaltyPercent() { return teacherRoyaltyPercent; }
    public List<CourseRevenueDto> getByCourse() { return byCourse; }
    public List<MonthlyRevenueDto> getByMonth() { return byMonth; }
}
