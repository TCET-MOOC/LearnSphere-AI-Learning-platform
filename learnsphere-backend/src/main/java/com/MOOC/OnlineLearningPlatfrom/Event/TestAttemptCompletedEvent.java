package com.MOOC.OnlineLearningPlatfrom.Event;

public class TestAttemptCompletedEvent {
    private final Long attemptId;
    private final Long testId;
    private final Long courseId;
    private final Long userId;

    public TestAttemptCompletedEvent(Long attemptId, Long testId, Long courseId, Long userId) {
        this.attemptId = attemptId;
        this.testId = testId;
        this.courseId = courseId;
        this.userId = userId;
    }

    public Long getAttemptId() { return attemptId; }
    public Long getTestId() { return testId; }
    public Long getCourseId() { return courseId; }
    public Long getUserId() { return userId; }
}
