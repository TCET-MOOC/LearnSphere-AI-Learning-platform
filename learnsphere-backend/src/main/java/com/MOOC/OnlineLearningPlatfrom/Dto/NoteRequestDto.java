package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.util.List;

public class NoteRequestDto {
    private Long courseId;
    private Long lectureId;
    private Long contentId;
    private Integer timestampSeconds;
    private String title;
    private String content;
    private List<String> tags;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getLectureId() { return lectureId; }
    public void setLectureId(Long lectureId) { this.lectureId = lectureId; }
    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }
    public Integer getTimestampSeconds() { return timestampSeconds; }
    public void setTimestampSeconds(Integer timestampSeconds) { this.timestampSeconds = timestampSeconds; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
