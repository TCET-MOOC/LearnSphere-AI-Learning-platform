package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Note;

import java.time.LocalDateTime;
import java.util.List;

public class NoteResponseDto {
    private Long id;
    private Long courseId;
    private String courseName;
    private Long lectureId;
    private String lectureLabel;
    private Long contentId;
    private String contentTitle;
    private Integer timestampSeconds;
    private String title;
    private String content;
    private List<String> tags;
    private LocalDateTime createdAt;

    public static NoteResponseDto from(Note note) {
        NoteResponseDto dto = new NoteResponseDto();
        dto.id = note.getId();
        if (note.getCourse() != null) {
            dto.courseId = note.getCourse().getId();
            dto.courseName = note.getCourse().getTitle();
        }
        if (note.getLecture() != null) {
            dto.lectureId = note.getLecture().getId();
            dto.lectureLabel = "Lec " + note.getLecture().getNumber() + ": " + note.getLecture().getTitle();
        }
        if (note.getCourseContent() != null) {
            dto.contentId = note.getCourseContent().getContentId();
            dto.contentTitle = note.getCourseContent().getTitle();
            if (dto.courseId == null && note.getCourseContent().getModule() != null && note.getCourseContent().getModule().getCourse() != null) {
                dto.courseId = note.getCourseContent().getModule().getCourse().getId();
                dto.courseName = note.getCourseContent().getModule().getCourse().getTitle();
            }
        }
        dto.timestampSeconds = note.getTimestampSeconds();
        dto.title = note.getTitle();
        dto.content = note.getContent();
        dto.tags = note.getTags();
        dto.createdAt = note.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public Long getLectureId() { return lectureId; }
    public String getLectureLabel() { return lectureLabel; }
    public Long getContentId() { return contentId; }
    public String getContentTitle() { return contentTitle; }
    public Integer getTimestampSeconds() { return timestampSeconds; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public List<String> getTags() { return tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
