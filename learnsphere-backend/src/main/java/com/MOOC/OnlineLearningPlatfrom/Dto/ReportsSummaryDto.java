package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;
import java.util.Map;

/** Real aggregate platform snapshot for the admin "Reports" screen / CSV export. */
public class ReportsSummaryDto {
    private long totalUsers;
    private Map<String, Long> usersByRole;
    private long totalCourses;
    private Map<String, Long> coursesByStatus;
    private BigDecimal totalRevenue;
    private long totalCertificatesIssued;
    private long flaggedContentPending;

    public ReportsSummaryDto(long totalUsers, Map<String, Long> usersByRole, long totalCourses,
                              Map<String, Long> coursesByStatus, BigDecimal totalRevenue,
                              long totalCertificatesIssued, long flaggedContentPending) {
        this.totalUsers = totalUsers;
        this.usersByRole = usersByRole;
        this.totalCourses = totalCourses;
        this.coursesByStatus = coursesByStatus;
        this.totalRevenue = totalRevenue;
        this.totalCertificatesIssued = totalCertificatesIssued;
        this.flaggedContentPending = flaggedContentPending;
    }

    public long getTotalUsers() { return totalUsers; }
    public Map<String, Long> getUsersByRole() { return usersByRole; }
    public long getTotalCourses() { return totalCourses; }
    public Map<String, Long> getCoursesByStatus() { return coursesByStatus; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public long getTotalCertificatesIssued() { return totalCertificatesIssued; }
    public long getFlaggedContentPending() { return flaggedContentPending; }
}
