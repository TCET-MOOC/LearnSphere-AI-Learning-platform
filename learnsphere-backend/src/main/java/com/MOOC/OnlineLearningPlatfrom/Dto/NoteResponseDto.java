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
    public Integer getTimestampSeconds() { return timestampSeconds; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public List<String> getTags() { return tags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
