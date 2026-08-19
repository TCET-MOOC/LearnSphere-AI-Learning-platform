package com.MOOC.OnlineLearningPlatfrom.Dto;

public class LeaderboardEntryDto {
    private int rank;
    private Long studentId;
    private String name;
    private String avatarUrl;
    private int points;

    public LeaderboardEntryDto(int rank, Long studentId, String name, String avatarUrl, int points) {
        this.rank = rank;
        this.studentId = studentId;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.points = points;
    }

    public int getRank() { return rank; }
    public Long getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getAvatarUrl() { return avatarUrl; }
    public int getPoints() { return points; }
}
