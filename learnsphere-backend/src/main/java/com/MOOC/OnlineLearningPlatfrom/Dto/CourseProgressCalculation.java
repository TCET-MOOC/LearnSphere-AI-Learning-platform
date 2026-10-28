package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.time.LocalDateTime;

public class CourseProgressCalculation {
    private final long totalContents;
    private final long completedContents;
    private final double progressPercent;
    private final boolean completed;
    private final LocalDateTime lastActivity;

    public CourseProgressCalculation(long totalContents, long completedContents, double progressPercent, boolean completed, LocalDateTime lastActivity) {
        this.totalContents = totalContents;
        this.completedContents = completedContents;
        this.progressPercent = progressPercent;
        this.completed = completed;
        this.lastActivity = lastActivity;
    }

    public long getTotalContents() { return totalContents; }
    public long getCompletedContents() { return completedContents; }
    public double getProgressPercent() { return progressPercent; }
    public boolean isCompleted() { return completed; }
    public LocalDateTime getLastActivity() { return lastActivity; }
}
