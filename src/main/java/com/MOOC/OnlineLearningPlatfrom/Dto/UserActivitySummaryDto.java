package com.MOOC.OnlineLearningPlatfrom.Dto;

/** Real counts of users by recency of UserAccount.lastActiveAt, for the admin dashboard/users page. */
public class UserActivitySummaryDto {
    private long totalUsers;
    private long dailyActive;
    private long weeklyActive;
    private long inactive30Days;

    public UserActivitySummaryDto(long totalUsers, long dailyActive, long weeklyActive, long inactive30Days) {
        this.totalUsers = totalUsers;
        this.dailyActive = dailyActive;
        this.weeklyActive = weeklyActive;
        this.inactive30Days = inactive30Days;
    }

    public long getTotalUsers() { return totalUsers; }
    public long getDailyActive() { return dailyActive; }
    public long getWeeklyActive() { return weeklyActive; }
    public long getInactive30Days() { return inactive30Days; }
}
