package com.MOOC.OnlineLearningPlatfrom.Dto;

public class TrendingCourseDto {
    private Long id;
    private String title;
    private String thumbnail;
    private String teacherName;
    private Long teacherId;
    private long enrolledCount;
    private int rank;

    public TrendingCourseDto(Long id, String title, String thumbnail, String teacherName, Long teacherId,
                              long enrolledCount, int rank) {
        this.id = id;
        this.title = title;
        this.thumbnail = thumbnail;
        this.teacherName = teacherName;
        this.teacherId = teacherId;
        this.enrolledCount = enrolledCount;
        this.rank = rank;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getThumbnail() { return thumbnail; }
    public String getTeacherName() { return teacherName; }
    public Long getTeacherId() { return teacherId; }
    public long getEnrolledCount() { return enrolledCount; }
    public int getRank() { return rank; }
}
