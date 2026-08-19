package com.MOOC.OnlineLearningPlatfrom.Dto;

public class StartConversationRequestDto {
    private Long otherUserId;
    private Long courseId;

    public Long getOtherUserId() { return otherUserId; }
    public void setOtherUserId(Long otherUserId) { this.otherUserId = otherUserId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
}
