package com.MOOC.OnlineLearningPlatfrom.Dto;

public class TeacherScoreDto {
    private String initials;
    private String name;
    private long comments;
    private String score;
    private String status;

    public TeacherScoreDto(String initials, String name, long comments, String score, String status) {
        this.initials = initials;
        this.name = name;
        this.comments = comments;
        this.score = score;
        this.status = status;
    }

    public String getInitials() { return initials; }
    public String getName() { return name; }
    public long getComments() { return comments; }
    public String getScore() { return score; }
    public String getStatus() { return status; }
}
