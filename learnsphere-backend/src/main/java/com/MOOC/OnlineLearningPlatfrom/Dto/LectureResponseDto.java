package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;

public class LectureResponseDto {
    private Long id;
    private Long courseId;
    private Long contentId;
    private String title;
    private Integer number;
    private String videoUrl;
    private Integer duration;
    private Boolean isDownloadable;
    private String status;

    public static LectureResponseDto from(Lecture lecture) {
        return from(lecture, null);
    }

    public static LectureResponseDto from(Lecture lecture, Long contentId) {
        LectureResponseDto dto = new LectureResponseDto();
        dto.id = lecture.getId();
        dto.courseId = lecture.getCourse() != null ? lecture.getCourse().getId() : null;
        dto.contentId = contentId;
        dto.title = lecture.getTitle();
        dto.number = lecture.getNumber();
        
        String rawUrl = lecture.getVideoUrl();
        if (rawUrl != null && (rawUrl.startsWith("uploads/") || rawUrl.contains("disk_storage") || rawUrl.startsWith("/"))) {
            // Do not leak raw server storage paths in metadata; delegate to signed playback descriptor
            dto.videoUrl = null;
        } else {
            dto.videoUrl = rawUrl;
        }

        dto.duration = lecture.getDuration();
        dto.isDownloadable = lecture.getIsDownloadable();
        dto.status = lecture.getStatus() != null ? lecture.getStatus().name() : null;
        return dto;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public Long getContentId() { return contentId; }
    public String getTitle() { return title; }
    public Integer getNumber() { return number; }
    public String getVideoUrl() { return videoUrl; }
    public Integer getDuration() { return duration; }
    public Boolean getIsDownloadable() { return isDownloadable; }
    public String getStatus() { return status; }
}
