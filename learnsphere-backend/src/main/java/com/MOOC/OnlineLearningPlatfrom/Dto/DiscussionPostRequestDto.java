package com.MOOC.OnlineLearningPlatfrom.Dto;

public class DiscussionPostRequestDto {
    private Long courseId;
    private Long lectureId;
    private Long parentPostId;
    private String body;

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getLectureId() { return lectureId; }
    public void setLectureId(Long lectureId) { this.lectureId = lectureId; }
    public Long getParentPostId() { return parentPostId; }
    public void setParentPostId(Long parentPostId) { this.parentPostId = parentPostId; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
