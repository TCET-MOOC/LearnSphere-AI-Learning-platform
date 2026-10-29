package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.StudentAnswer;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;
import com.MOOC.OnlineLearningPlatfrom.Event.TestAttemptCompletedEvent;
import com.MOOC.OnlineLearningPlatfrom.Repository.StudentAnswerRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * W4-06 Background worker: Attempt lifecycle timeout job.
 * Runs periodically to transition expired IN_PROGRESS attempts to ABANDONED,
 * auto-evaluating score from existing answers and publishing completion events (AC-18, [D-F5]).
 */
@Component
public class AttemptTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(AttemptTimeoutJob.class);

    private final TestAttemptRepository testAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AttemptTimeoutJob(TestAttemptRepository testAttemptRepository,
                             StudentAnswerRepository studentAnswerRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.testAttemptRepository = testAttemptRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Runs every minute to sweep expired in-progress attempts.
     */
    @Scheduled(fixedDelay = 60000)
    public void runTimeoutSweep() {
        checkAndExpireTimedOutAttempts();
    }

    /**
     * Checks all in-progress attempts against server clock and test duration.
     * Transitions expired attempts to ABANDONED and calculates score from stored answers.
     *
     * @return count of expired attempts
     */
    @Transactional
    public int checkAndExpireTimedOutAttempts() {
        List<TestAttempt> inProgressAttempts = testAttemptRepository.findByStatus("IN_PROGRESS");
        if (inProgressAttempts == null || inProgressAttempts.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        int expiredCount = 0;

        for (TestAttempt attempt : inProgressAttempts) {
            Test test = attempt.getTest();
            if (test == null || test.getDurationMinutes() == null || test.getDurationMinutes() <= 0) {
                continue;
            }

            LocalDateTime start = attempt.getStartedAt() != null ? attempt.getStartedAt() : attempt.getAttemptedAt();
            if (start == null) {
                continue;
            }

            LocalDateTime deadline = start.plusMinutes(test.getDurationMinutes());
            if (now.isAfter(deadline)) {
                log.info("Expiring timed-out test attempt id={} (startedAt={}, deadline={})",
                        attempt.getAttemptId(), start, deadline);

                attempt.setStatus("ABANDONED");
                attempt.setCompletedAt(deadline);

                // Auto-grade existing stored answers
                List<StudentAnswer> answers = studentAnswerRepository.findByAttempt_AttemptId(attempt.getAttemptId());
                java.math.BigDecimal totalScore = java.math.BigDecimal.ZERO;
                if (answers != null) {
                    for (StudentAnswer answer : answers) {
                        Question question = answer.getQuestion();
                        if (question != null) {
                            String type = question.getQuestionType() != null ? question.getQuestionType().trim().toUpperCase() : "";
                            boolean isAutoGradable = type.equals("MCQ") || type.equals("MULTIPLE_CHOICE")
                                    || type.equals("TF") || type.equals("TRUE_FALSE") || type.equals("BOOLEAN");

                            if (isAutoGradable && answer.getMarksAwarded() == null) {
                                String correct = question.getCorrectAnswer();
                                String studentAns = answer.getAnswerText();
                                if (correct != null && studentAns != null && correct.trim().equalsIgnoreCase(studentAns.trim())) {
                                    java.math.BigDecimal marks = question.getMarks() != null ? question.getMarks() : java.math.BigDecimal.ZERO;
                                    answer.setMarksAwarded(marks);
                                } else {
                                    answer.setMarksAwarded(java.math.BigDecimal.ZERO);
                                }
                                studentAnswerRepository.save(answer);
                            }
                        }
                        if (answer.getMarksAwarded() != null) {
                            totalScore = totalScore.add(answer.getMarksAwarded());
                        }
                    }
                }

                attempt.setScore(totalScore);
                TestAttempt saved = testAttemptRepository.save(attempt);
                expiredCount++;

                if (saved.getTest() != null && saved.getTest().getCourse() != null) {
                    eventPublisher.publishEvent(new TestAttemptCompletedEvent(
                            saved.getAttemptId(),
                            saved.getTest().getTestId(),
                            saved.getTest().getCourse().getId(),
                            saved.getUserId()
                    ));
                }
            }
        }

        return expiredCount;
    }
}

