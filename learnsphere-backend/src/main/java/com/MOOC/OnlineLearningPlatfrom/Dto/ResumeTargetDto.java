package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;

public class ResumeTargetDto {
    private Long courseId;
    private Long contentId;
    private Long lectureId;
    private Long moduleId;
    private String moduleTitle;
    private String title;
    private String contentType;
    private Integer position;
    private BigDecimal progressPercent;
    private Integer secondsWatched;
    private boolean completed;

    public ResumeTargetDto() {}

    public ResumeTargetDto(Long courseId, Long contentId, Long lectureId, Long moduleId,
                           String moduleTitle, String title, String contentType,
                           Integer position, BigDecimal progressPercent,
                           Integer secondsWatched, boolean completed) {
        this.courseId = courseId;
        this.contentId = contentId;
        this.lectureId = lectureId;
        this.moduleId = moduleId;
        this.moduleTitle = moduleTitle;
        this.title = title;
        this.contentType = contentType;
        this.position = position;
        this.progressPercent = progressPercent;
        this.secondsWatched = secondsWatched;
        this.completed = completed;
    }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }

    public Long getLectureId() { return lectureId; }
    public void setLectureId(Long lectureId) { this.lectureId = lectureId; }

    public Long getModuleId() { return moduleId; }
    public void setModuleId(Long moduleId) { this.moduleId = moduleId; }

    public String getModuleTitle() { return moduleTitle; }
    public void setModuleTitle(String moduleTitle) { this.moduleTitle = moduleTitle; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public BigDecimal getProgressPercent() { return progressPercent; }
    public void setProgressPercent(BigDecimal progressPercent) { this.progressPercent = progressPercent; }

    public Integer getSecondsWatched() { return secondsWatched; }
    public void setSecondsWatched(Integer secondsWatched) { this.secondsWatched = secondsWatched; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
