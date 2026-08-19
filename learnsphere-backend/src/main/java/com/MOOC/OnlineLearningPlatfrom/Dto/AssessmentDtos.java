package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.StudentAnswer;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Holds all the small, purpose-built DTOs used by the Assessments (tests/quizzes) feature.
 * Kept in one file since each one is a handful of fields with no independent lifecycle.
 */
public class AssessmentDtos {

    // Lightweight summary for list views — no questions, no leaking other students' attempts.
    public static class TestSummaryDto {
        private Long id;
        private Long courseId;
        private String courseTitle;
        private Long lectureId;
        private String title;
        private Integer durationMinutes;
        private Boolean isRemedial;
        private int questionCount;
        private LocalDateTime scheduledAt;

        public static TestSummaryDto from(Test test) {
            TestSummaryDto dto = new TestSummaryDto();
            dto.id = test.getTestId();
            if (test.getCourse() != null) {
                dto.courseId = test.getCourse().getId();
                dto.courseTitle = test.getCourse().getTitle();
            }
            if (test.getLecture() != null) {
                dto.lectureId = test.getLecture().getId();
            }
            dto.title = test.getTitle();
            dto.durationMinutes = test.getDurationMinutes();
            dto.isRemedial = test.getIsRemedial();
            dto.questionCount = test.getQuestions() != null ? test.getQuestions().size() : 0;
            dto.scheduledAt = test.getScheduledAt();
            return dto;
        }

        public Long getId() { return id; }
        public Long getCourseId() { return courseId; }
        public String getCourseTitle() { return courseTitle; }
        public Long getLectureId() { return lectureId; }
        public String getTitle() { return title; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public Boolean getIsRemedial() { return isRemedial; }
        public int getQuestionCount() { return questionCount; }
        public LocalDateTime getScheduledAt() { return scheduledAt; }
    }

    // Student-facing question — never exposes correctAnswer.
    public static class QuestionDto {
        private Long questionId;
        private String body;
        private String questionType;
        private Integer marks;
        private List<String> options;

        public static QuestionDto from(Question q) {
            QuestionDto dto = new QuestionDto();
            dto.questionId = q.getQuestionId();
            dto.body = q.getBody();
            dto.questionType = q.getQuestionType();
            dto.marks = q.getMarks();
            dto.options = q.getOptions();
            return dto;
        }

        public Long getQuestionId() { return questionId; }
        public String getBody() { return body; }
        public String getQuestionType() { return questionType; }
        public Integer getMarks() { return marks; }
        public List<String> getOptions() { return options; }
    }

    // Full test detail including questions (student-safe).
    public static class TestDetailDto {
        private Long id;
        private Long courseId;
        private Long lectureId;
        private String title;
        private Integer durationMinutes;
        private String securityPolicy;
        private Boolean isRemedial;
        private LocalDateTime scheduledAt;
        private List<QuestionDto> questions;

        public static TestDetailDto from(Test test) {
            TestDetailDto dto = new TestDetailDto();
            dto.id = test.getTestId();
            dto.courseId = test.getCourse() != null ? test.getCourse().getId() : null;
            dto.lectureId = test.getLecture() != null ? test.getLecture().getId() : null;
            dto.title = test.getTitle();
            dto.durationMinutes = test.getDurationMinutes();
            dto.securityPolicy = test.getSecurityPolicy();
            dto.isRemedial = test.getIsRemedial();
            dto.scheduledAt = test.getScheduledAt();
            dto.questions = test.getQuestions() != null
                    ? test.getQuestions().stream().map(QuestionDto::from).toList()
                    : List.of();
            return dto;
        }

        public Long getId() { return id; }
        public Long getCourseId() { return courseId; }
        public Long getLectureId() { return lectureId; }
        public String getTitle() { return title; }
        public Integer getDurationMinutes() { return durationMinutes; }
        public String getSecurityPolicy() { return securityPolicy; }
        public Boolean getIsRemedial() { return isRemedial; }
        public LocalDateTime getScheduledAt() { return scheduledAt; }
        public List<QuestionDto> getQuestions() { return questions; }
    }

    // Attempt summary — returned by start/answer calls.
    public static class AttemptDto {
        private Long attemptId;
        private Long testId;
        private String status;
        private Integer score;
        private LocalDateTime attemptedAt;

        public static AttemptDto from(TestAttempt attempt) {
            AttemptDto dto = new AttemptDto();
            dto.attemptId = attempt.getAttemptId();
            dto.testId = attempt.getTest() != null ? attempt.getTest().getTestId() : null;
            dto.status = attempt.getStatus();
            dto.score = attempt.getScore();
            dto.attemptedAt = attempt.getAttemptedAt();
            return dto;
        }

        public Long getAttemptId() { return attemptId; }
        public Long getTestId() { return testId; }
        public String getStatus() { return status; }
        public Integer getScore() { return score; }
        public LocalDateTime getAttemptedAt() { return attemptedAt; }
    }

    // Per-question review row: what was asked, the correct answer, what the student answered, marks awarded.
    public static class AnswerReviewDto {
        private Long questionId;
        private String questionBody;
        private String questionType;
        private Integer marks;
        private List<String> options;
        private String correctAnswer;
        private String studentAnswer;
        private Integer marksAwarded;

        public static AnswerReviewDto from(StudentAnswer answer) {
            AnswerReviewDto dto = new AnswerReviewDto();
            Question q = answer.getQuestion();
            if (q != null) {
                dto.questionId = q.getQuestionId();
                dto.questionBody = q.getBody();
                dto.questionType = q.getQuestionType();
                dto.marks = q.getMarks();
                dto.options = q.getOptions();
                dto.correctAnswer = q.getCorrectAnswer();
            }
            dto.studentAnswer = answer.getAnswerText();
            dto.marksAwarded = answer.getMarksAwarded();
            return dto;
        }

        public Long getQuestionId() { return questionId; }
        public String getQuestionBody() { return questionBody; }
        public String getQuestionType() { return questionType; }
        public Integer getMarks() { return marks; }
        public List<String> getOptions() { return options; }
        public String getCorrectAnswer() { return correctAnswer; }
        public String getStudentAnswer() { return studentAnswer; }
        public Integer getMarksAwarded() { return marksAwarded; }
    }

    // Full review for a completed (or in-progress) attempt.
    public static class AttemptReviewDto {
        private Long attemptId;
        private Long testId;
        private String testTitle;
        private String status;
        private Integer score;
        private int maxScore;
        private Boolean isRemedial;
        private LocalDateTime attemptedAt;
        private List<AnswerReviewDto> answers;

        public static AttemptReviewDto from(TestAttempt attempt) {
            AttemptReviewDto dto = new AttemptReviewDto();
            dto.attemptId = attempt.getAttemptId();
            Test test = attempt.getTest();
            if (test != null) {
                dto.testId = test.getTestId();
                dto.testTitle = test.getTitle();
                dto.isRemedial = test.getIsRemedial();
                dto.maxScore = test.getQuestions() != null
                        ? test.getQuestions().stream().mapToInt(q -> q.getMarks() != null ? q.getMarks() : 0).sum()
                        : 0;
            }
            dto.status = attempt.getStatus();
            dto.score = attempt.getScore();
            dto.attemptedAt = attempt.getAttemptedAt();
            dto.answers = attempt.getStudentAnswers() != null
                    ? attempt.getStudentAnswers().stream().map(AnswerReviewDto::from).toList()
                    : List.of();
            return dto;
        }

        public Long getAttemptId() { return attemptId; }
        public Long getTestId() { return testId; }
        public String getTestTitle() { return testTitle; }
        public String getStatus() { return status; }
        public Integer getScore() { return score; }
        public int getMaxScore() { return maxScore; }
        public Boolean getIsRemedial() { return isRemedial; }
        public LocalDateTime getAttemptedAt() { return attemptedAt; }
        public List<AnswerReviewDto> getAnswers() { return answers; }
    }
}
