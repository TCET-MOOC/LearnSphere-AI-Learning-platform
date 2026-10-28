package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CourseProgressSummaryDto {
    private Long courseId;
    private String courseTitle;
    private long totalEnrolled;
    private long totalContents;
    private long completedCount;
    private long inProgressCount;
    private long notStartedCount;
    private BigDecimal averageProgress;
    private List<StudentProgressItemDto> students;

    public CourseProgressSummaryDto() {}

    public CourseProgressSummaryDto(Long courseId, String courseTitle, long totalEnrolled,
                                    long totalContents, long completedCount, long inProgressCount,
                                    long notStartedCount, BigDecimal averageProgress,
                                    List<StudentProgressItemDto> students) {
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.totalEnrolled = totalEnrolled;
        this.totalContents = totalContents;
        this.completedCount = completedCount;
        this.inProgressCount = inProgressCount;
        this.notStartedCount = notStartedCount;
        this.averageProgress = averageProgress;
        this.students = students;
    }

    public static class StudentProgressItemDto {
        private Long userId;
        private String fullName;
        private String email;
        private long completedContentsCount;
        private long totalContents;
        private BigDecimal progressPercent;
        private boolean completed;
        private LocalDateTime enrolledAt;
        private LocalDateTime lastActivity;

        public StudentProgressItemDto() {}

        public StudentProgressItemDto(Long userId, String fullName, String email,
                                      long completedContentsCount, long totalContents,
                                      BigDecimal progressPercent, boolean completed,
                                      LocalDateTime enrolledAt, LocalDateTime lastActivity) {
            this.userId = userId;
            this.fullName = fullName;
            this.email = email;
            this.completedContentsCount = completedContentsCount;
            this.totalContents = totalContents;
            this.progressPercent = progressPercent;
            this.completed = completed;
            this.enrolledAt = enrolledAt;
            this.lastActivity = lastActivity;
        }

        public Long getUserId() { return userId; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public long getCompletedContentsCount() { return completedContentsCount; }
        public long getTotalContents() { return totalContents; }
        public BigDecimal getProgressPercent() { return progressPercent; }
        public boolean isCompleted() { return completed; }
        public LocalDateTime getEnrolledAt() { return enrolledAt; }
        public LocalDateTime getLastActivity() { return lastActivity; }

        public void setUserId(Long userId) { this.userId = userId; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public void setEmail(String email) { this.email = email; }
        public void setCompletedContentsCount(long completedContentsCount) { this.completedContentsCount = completedContentsCount; }
        public void setTotalContents(long totalContents) { this.totalContents = totalContents; }
        public void setProgressPercent(BigDecimal progressPercent) { this.progressPercent = progressPercent; }
        public void setCompleted(boolean completed) { this.completed = completed; }
        public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
        public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }
    }

    public Long getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public long getTotalEnrolled() { return totalEnrolled; }
    public long getTotalContents() { return totalContents; }
    public long getCompletedCount() { return completedCount; }
    public long getInProgressCount() { return inProgressCount; }
    public long getNotStartedCount() { return notStartedCount; }
    public BigDecimal getAverageProgress() { return averageProgress; }
    public List<StudentProgressItemDto> getStudents() { return students; }

    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public void setTotalEnrolled(long totalEnrolled) { this.totalEnrolled = totalEnrolled; }
    public void setTotalContents(long totalContents) { this.totalContents = totalContents; }
    public void setCompletedCount(long completedCount) { this.completedCount = completedCount; }
    public void setInProgressCount(long inProgressCount) { this.inProgressCount = inProgressCount; }
    public void setNotStartedCount(long notStartedCount) { this.notStartedCount = notStartedCount; }
    public void setAverageProgress(BigDecimal averageProgress) { this.averageProgress = averageProgress; }
    public void setStudents(List<StudentProgressItemDto> students) { this.students = students; }
}
