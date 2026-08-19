package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.time.LocalDateTime;

/** Matches the frontend's StudentStanding interface (core/models/user.model.ts) exactly. */
public class StudentStandingDto {
    private Long studentId;
    private String name;
    private String avatarUrl;
    private int rank;
    private double scorePercent;
    private long lecturesWatched;
    private long totalLectures;
    private LocalDateTime lastActiveAt;
    private boolean isRemedial;
    private boolean isAtRisk;

    public StudentStandingDto(Long studentId, String name, String avatarUrl, int rank, double scorePercent,
                               long lecturesWatched, long totalLectures, LocalDateTime lastActiveAt,
                               boolean isRemedial, boolean isAtRisk) {
        this.studentId = studentId;
        this.name = name;
        this.avatarUrl = avatarUrl;
        this.rank = rank;
        this.scorePercent = scorePercent;
        this.lecturesWatched = lecturesWatched;
        this.totalLectures = totalLectures;
        this.lastActiveAt = lastActiveAt;
        this.isRemedial = isRemedial;
        this.isAtRisk = isAtRisk;
    }

    public Long getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getAvatarUrl() { return avatarUrl; }
    public int getRank() { return rank; }
    public double getScorePercent() { return scorePercent; }
    public long getLecturesWatched() { return lecturesWatched; }
    public long getTotalLectures() { return totalLectures; }
    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    // Named getIsXxx() (not isXxx()) so Jackson serializes these as "isRemedial"/"isAtRisk" —
    // matching the frontend's StudentStanding interface exactly. A plain isXxx() getter would
    // have its "is" stripped by Jackson's bean-naming convention, producing "remedial"/"atRisk".
    public boolean getIsRemedial() { return isRemedial; }
    public boolean getIsAtRisk() { return isAtRisk; }
}
