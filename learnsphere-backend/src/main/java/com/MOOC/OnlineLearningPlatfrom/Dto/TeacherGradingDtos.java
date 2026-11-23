package com.MOOC.OnlineLearningPlatfrom.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTOs for the teacher assessments screen (SCR-35, W7-16). Built from explicit fields, so no
 * correct answer, option key or entity graph can reach the client.
 */
public final class TeacherGradingDtos {

    private TeacherGradingDtos() {
    }

    /** One test on a course the teacher owns, with what the screen needs to triage it. */
    public record TestSummary(
            Long testId,
            String title,
            Long courseId,
            String courseTitle,
            Long lectureId,
            String lectureTitle,
            int questionCount,
            int textQuestionCount,
            long pendingCount) {
    }

    /** A TEXT answer on a finished attempt that has no marks yet. */
    public record PendingAnswer(
            Long answerId,
            Long attemptId,
            Long testId,
            String testTitle,
            String courseTitle,
            Long studentId,
            String studentName,
            Long questionId,
            String questionText,
            BigDecimal maxMarks,
            String answerText,
            LocalDateTime submittedAt) {
    }
}
