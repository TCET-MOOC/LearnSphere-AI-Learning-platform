package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;

public class LectureResponseDto {
    private Long id;
    private Long courseId;
    private String title;
    private Integer number;
    private String videoUrl;
    private Integer duration;
    private Boolean isDownloadable;
    private String status;

    public static LectureResponseDto from(Lecture lecture) {
        LectureResponseDto dto = new LectureResponseDto();
        dto.id = lecture.getId();
        dto.courseId = lecture.getCourse() != null ? lecture.getCourse().getId() : null;
        dto.title = lecture.getTitle();
        dto.number = lecture.getNumber();
        dto.videoUrl = lecture.getVideoUrl();
        dto.duration = lecture.getDuration();
        dto.isDownloadable = lecture.getIsDownloadable();
        dto.status = lecture.getStatus() != null ? lecture.getStatus().name() : null;
        return dto;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public Integer getNumber() { return number; }
    public String getVideoUrl() { return videoUrl; }
    public Integer getDuration() { return duration; }
    public Boolean getIsDownloadable() { return isDownloadable; }
    public String getStatus() { return status; }
}
