package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;

/** Per-course royalty summary row for the teacher royalties page ("Earnings by course"). */
public class RoyaltyBreakdownDto {
    private Long courseId;
    private String courseTitle;
    private long enrolledCount;
    private long externalPaidCount;
    private BigDecimal amount;
    private String status;
    private boolean isDraft;

    public RoyaltyBreakdownDto(Long courseId, String courseTitle, long enrolledCount, long externalPaidCount,
                                BigDecimal amount, String status, boolean isDraft) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.enrolledCount = enrolledCount;
        this.externalPaidCount = externalPaidCount;
        this.amount = amount;
        this.status = status;
        this.isDraft = isDraft;
    }

    public Long getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public long getEnrolledCount() { return enrolledCount; }
    public long getExternalPaidCount() { return externalPaidCount; }
    public BigDecimal getAmount() { return amount; }
    public String getStatus() { return status; }
    // getIsDraft() (not isDraft()) so Jackson serializes this as "isDraft" — matching the
    // frontend's earningsByCourse row shape (course.isDraft).
    public boolean getIsDraft() { return isDraft; }
}
