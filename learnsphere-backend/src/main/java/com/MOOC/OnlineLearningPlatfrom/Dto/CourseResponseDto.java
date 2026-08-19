package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CourseResponseDto {
    private Long id;
    private String title;
    private String description;
    private Long teacherId;
    private String teacherName;
    private String department;
    private String thumbnail;
    private String status;
    private BigDecimal price;
    private LocalDateTime createdAt;
    private long lectureCount;
    private long completedLecturesCount;
    private double progressPercent;
    private boolean completed;

    public static CourseResponseDto from(Course course, long lectureCount) {
        CourseResponseDto dto = new CourseResponseDto();
        dto.id = course.getId();
        dto.title = course.getTitle();
        dto.description = course.getDescription();
        if (course.getTeacher() != null) {
            dto.teacherId = course.getTeacher().getUserId();
            dto.teacherName = course.getTeacher().getFullName();
        }
        dto.department = course.getDepartment();
        dto.thumbnail = course.getThumbnail();
        dto.status = course.getStatus() != null ? course.getStatus().name() : null;
        dto.price = course.getPrice();
        dto.createdAt = course.getCreatedAt();
        dto.lectureCount = lectureCount;
        return dto;
    }

    public static CourseResponseDto from(Course course, long lectureCount, long completedLecturesCount) {
        CourseResponseDto dto = from(course, lectureCount);
        dto.completedLecturesCount = completedLecturesCount;
        dto.progressPercent = lectureCount > 0 ? Math.min(100.0, Math.round(((double) completedLecturesCount / lectureCount) * 100.0)) : 0.0;
        dto.completed = lectureCount > 0 && completedLecturesCount >= lectureCount;
        return dto;
    }

    public static CourseResponseDto from(Course course) {
        return from(course, 0);
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public String getDepartment() { return department; }
    public String getThumbnail() { return thumbnail; }
    public String getStatus() { return status; }
    public BigDecimal getPrice() { return price; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getLectureCount() { return lectureCount; }
    public long getCompletedLecturesCount() { return completedLecturesCount; }
    public double getProgressPercent() { return progressPercent; }
    public boolean isCompleted() { return completed; }

    public void setCompletedLecturesCount(long completedLecturesCount) { this.completedLecturesCount = completedLecturesCount; }
    public void setProgressPercent(double progressPercent) { this.progressPercent = progressPercent; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
