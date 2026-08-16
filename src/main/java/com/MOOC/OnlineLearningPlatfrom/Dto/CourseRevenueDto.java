package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;

public class CourseRevenueDto {
    private Long courseId;
    private String courseTitle;
    private String teacherName;
    private BigDecimal amount;
    private long purchases;

    public CourseRevenueDto(Long courseId, String courseTitle, String teacherName, BigDecimal amount, long purchases) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.teacherName = teacherName;
        this.amount = amount;
        this.purchases = purchases;
    }

    public Long getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public String getTeacherName() { return teacherName; }
    public BigDecimal getAmount() { return amount; }
    public long getPurchases() { return purchases; }
}
