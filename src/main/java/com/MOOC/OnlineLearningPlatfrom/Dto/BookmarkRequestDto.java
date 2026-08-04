package com.MOOC.OnlineLearningPlatfrom.Dto;

public class BookmarkRequestDto {
    private Long lectureId;
    private Integer timestampSeconds;
    private String label;

    public Long getLectureId() { return lectureId; }
    public void setLectureId(Long lectureId) { this.lectureId = lectureId; }
    public Integer getTimestampSeconds() { return timestampSeconds; }
    public void setTimestampSeconds(Integer timestampSeconds) { this.timestampSeconds = timestampSeconds; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
