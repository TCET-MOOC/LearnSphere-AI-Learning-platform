package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class TestServiceImpl implements TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final UserProgressRepository userProgressRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TestServiceImpl(TestRepository testRepository,
                           QuestionRepository questionRepository,
                           TestAttemptRepository testAttemptRepository,
                           StudentAnswerRepository studentAnswerRepository,
                           UserProgressRepository userProgressRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.userProgressRepository = userProgressRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Test createTest(Test test) {
        return testRepository.save(test);
    }

    @Override
    public List<Test> getAllTests() {
        return testRepository.findAll();
    }

    @Override
    public Test getTestById(Long testId) {
        return testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));
    }

    @Override
    public Test updateTest(Long testId, Test testDetails) {
        Test test = getTestById(testId);
        test.setTitle(testDetails.getTitle());
        test.setDurationMinutes(testDetails.getDurationMinutes());
        test.setSecurityPolicy(testDetails.getSecurityPolicy());
        test.setIsRemedial(testDetails.getIsRemedial());
        test.setScheduledAt(testDetails.getScheduledAt());
        if (testDetails.getPlacement() != null) test.setPlacement(testDetails.getPlacement());
        if (testDetails.getRequiredForCompletion() != null) test.setRequiredForCompletion(testDetails.getRequiredForCompletion());
        if (testDetails.getPassThreshold() != null) test.setPassThreshold(testDetails.getPassThreshold());
        if (testDetails.getMaxAttempts() != null) test.setMaxAttempts(testDetails.getMaxAttempts());
        if (testDetails.getContent() != null) test.setContent(testDetails.getContent());
        return testRepository.save(test);
    }

    @Override
    public void deleteTest(Long testId) {
        Test test = getTestById(testId);
        testRepository.delete(test);
    }

    @Override
    public Question addQuestionToTest(Long testId, Question question) {
        Test test = getTestById(testId);
        question.setTest(test);
        question.validateMcqCorrectAnswer();
        return questionRepository.save(question);
    }

    @Override
    public TestAttempt startTestAttempt(Long testId, Long userId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

        LocalDateTime now = LocalDateTime.now();

        // 1. Enforce scheduledAt
        if (test.getScheduledAt() != null && now.isBefore(test.getScheduledAt())) {
            throw new BadRequestException("Test is scheduled for a future time: " + test.getScheduledAt());
        }

        // 2. Check existing attempts for this user and test
        List<TestAttempt> existingAttempts = testAttemptRepository.findByUserIdAndTest_TestId(userId, testId);

        TestAttempt activeAttempt = null;
        if (existingAttempts != null) {
            for (TestAttempt att : existingAttempts) {
                if ("IN_PROGRESS".equalsIgnoreCase(att.getStatus())) {
                    LocalDateTime start = att.getStartedAt() != null ? att.getStartedAt() : att.getAttemptedAt();
                    if (start != null && test.getDurationMinutes() != null && test.getDurationMinutes() > 0) {
                        LocalDateTime deadline = start.plusMinutes(test.getDurationMinutes());
                        if (now.isAfter(deadline)) {
                            // Expired while still marked IN_PROGRESS -> auto-abandon
                            att.setStatus("ABANDONED");
                            att.setCompletedAt(deadline);
                            testAttemptRepository.save(att);
                        } else {
                            activeAttempt = att;
                            break;
                        }
                    } else {
                        activeAttempt = att;
                        break;
                    }
                }
            }
        }

        // Resume existing active in-progress attempt if within duration window
        if (activeAttempt != null) {
            return activeAttempt;
        }

        // 3. Enforce attempt limit
        if (test.getMaxAttempts() != null && test.getMaxAttempts() > 0 && existingAttempts != null) {
            long usedAttempts = existingAttempts.stream()
                    .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()) || "ABANDONED".equalsIgnoreCase(a.getStatus()))
                    .count();
            if (usedAttempts >= test.getMaxAttempts()) {
                throw new BadRequestException("Maximum attempt limit of " + test.getMaxAttempts() + " reached for this test");
            }
        }

        // 4. Start new attempt with server clock
        TestAttempt newAttempt = new TestAttempt();
        newAttempt.setTest(test);
        newAttempt.setUserId(userId);
        newAttempt.setStartedAt(now);
        newAttempt.setAttemptedAt(now);
        newAttempt.setCompletedAt(null);
        newAttempt.setStatus("IN_PROGRESS");
        return testAttemptRepository.save(newAttempt);
    }

    @Override
    public StudentAnswer submitAnswer(Long attemptId, Long questionId, StudentAnswer studentAnswer) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));

        if (!"IN_PROGRESS".equalsIgnoreCase(attempt.getStatus())) {
            throw new BadRequestException("Attempt is already finalized (status: " + attempt.getStatus() + ")");
        }

        LocalDateTime startTime = attempt.getStartedAt() != null ? attempt.getStartedAt() : attempt.getAttemptedAt();
        if (startTime != null && attempt.getTest() != null
                && attempt.getTest().getDurationMinutes() != null && attempt.getTest().getDurationMinutes() > 0) {
            LocalDateTime deadline = startTime.plusMinutes(attempt.getTest().getDurationMinutes());
            if (LocalDateTime.now().isAfter(deadline)) {
                attempt.setStatus("ABANDONED");
                attempt.setCompletedAt(deadline);
                testAttemptRepository.save(attempt);
                throw new BadRequestException("Attempt has expired / timed out");
            }
        }

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));

        if (attempt.getTest() != null && question.getTest() != null
                && !attempt.getTest().getTestId().equals(question.getTest().getTestId())) {
            throw new BadRequestException("Question does not belong to this test");
        }

        StudentAnswer targetAnswer = studentAnswerRepository
                .findByAttempt_AttemptIdAndQuestion_QuestionId(attemptId, questionId)
                .orElse(null);

        if (targetAnswer == null) {
            targetAnswer = new StudentAnswer();
            targetAnswer.setAttempt(attempt);
            targetAnswer.setQuestion(question);
        }

        targetAnswer.setAnswerText(studentAnswer.getAnswerText());

        // AC-17: Auto-grading is performed strictly server-side for MCQ/TF; TEXT remains null
        String type = question.getQuestionType() != null ? question.getQuestionType().trim().toUpperCase() : "";
        boolean isAutoGradable = type.equals("MCQ") || type.equals("MULTIPLE_CHOICE")
                || type.equals("TF") || type.equals("TRUE_FALSE") || type.equals("BOOLEAN");

        if (isAutoGradable) {
            String answerText = studentAnswer.getAnswerText();
            boolean hasAnswer = answerText != null && !answerText.trim().isEmpty();
            BigDecimal marksForQuestion = question.getMarks() != null ? question.getMarks() : BigDecimal.ZERO;

            if (hasAnswer && question.getCorrectAnswer() != null
                    && question.getCorrectAnswer().trim().equalsIgnoreCase(answerText.trim())) {
                targetAnswer.setMarksAwarded(marksForQuestion);
            } else {
                targetAnswer.setMarksAwarded(BigDecimal.ZERO);
            }
        } else {
            targetAnswer.setMarksAwarded((BigDecimal) null);
        }

        return studentAnswerRepository.save(targetAnswer);
    }

    @Override
    public TestAttempt calculateAndFinalizeScore(Long attemptId) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));

        // Replays are harmless: a finished attempt is returned as stored. It is not re-graded,
        // never flipped to ABANDONED after its deadline, and publishes no second completion event.
        if ("COMPLETED".equalsIgnoreCase(attempt.getStatus()) || "ABANDONED".equalsIgnoreCase(attempt.getStatus())) {
            return attempt;
        }

        List<StudentAnswer> answers = studentAnswerRepository.findByAttempt_AttemptId(attemptId);

        LocalDateTime startTime = attempt.getStartedAt() != null ? attempt.getStartedAt() : attempt.getAttemptedAt();
        if (startTime != null && attempt.getTest() != null
                && attempt.getTest().getDurationMinutes() != null && attempt.getTest().getDurationMinutes() > 0) {
            LocalDateTime deadline = startTime.plusMinutes(attempt.getTest().getDurationMinutes());
            if (LocalDateTime.now().isAfter(deadline)) {
                attempt.setStatus("ABANDONED");
                attempt.setCompletedAt(deadline);
                // Grade auto-gradable answers
                for (StudentAnswer answer : answers) {
                    Question question = answer.getQuestion();
                    BigDecimal marksForQuestion = question != null && question.getMarks() != null ? question.getMarks() : BigDecimal.ZERO;
                    String answerText = answer.getAnswerText();
                    boolean hasAnswer = answerText != null && !answerText.trim().isEmpty();
                    if (question != null) {
                        String type = question.getQuestionType() != null ? question.getQuestionType().trim().toUpperCase() : "";
                        boolean isAutoGradable = type.equals("MCQ") || type.equals("MULTIPLE_CHOICE")
                                || type.equals("TF") || type.equals("TRUE_FALSE") || type.equals("BOOLEAN");
                        if (isAutoGradable) {
                            BigDecimal awarded = BigDecimal.ZERO;
                            if (hasAnswer) {
                                String correct = question.getCorrectAnswer();
                                if (correct != null && correct.trim().equalsIgnoreCase(answerText.trim())) {
                                    awarded = marksForQuestion;
                                }
                            }
                            answer.setMarksAwarded(awarded);
                            studentAnswerRepository.save(answer);
                        }
                    }
                }
                BigDecimal totalScore = BigDecimal.ZERO;
                for (StudentAnswer answer : answers) {
                    if (answer.getMarksAwarded() != null) {
                        totalScore = totalScore.add(answer.getMarksAwarded());
                    }
                }
                attempt.setScore(totalScore);
                TestAttempt saved = testAttemptRepository.save(attempt);
                if (saved.getTest() != null && saved.getTest().getCourse() != null) {
                    eventPublisher.publishEvent(new com.MOOC.OnlineLearningPlatfrom.Event.TestAttemptCompletedEvent(
                            saved.getAttemptId(),
                            saved.getTest().getTestId(),
                            saved.getTest().getCourse().getId(),
                            saved.getUserId()
                    ));
                }
                throw new BadRequestException("Attempt has expired / timed out");
            }
        }

        for (StudentAnswer answer : answers) {
            Question question = answer.getQuestion();
            BigDecimal marksForQuestion = question != null && question.getMarks() != null ? question.getMarks() : BigDecimal.ZERO;
            String answerText = answer.getAnswerText();
            boolean hasAnswer = answerText != null && !answerText.trim().isEmpty();

            if (question != null) {
                String type = question.getQuestionType() != null ? question.getQuestionType().trim().toUpperCase() : "";
                boolean isAutoGradable = type.equals("MCQ") || type.equals("MULTIPLE_CHOICE")
                        || type.equals("TF") || type.equals("TRUE_FALSE") || type.equals("BOOLEAN");

                if (isAutoGradable) {
                    BigDecimal awarded = BigDecimal.ZERO;
                    if (hasAnswer) {
                        String correct = question.getCorrectAnswer();
                        if (correct != null && correct.trim().equalsIgnoreCase(answerText.trim())) {
                            awarded = marksForQuestion;
                        }
                    }
                    answer.setMarksAwarded(awarded);
                } else {
                    if (answer.getMarksAwarded() == null) {
                        answer.setMarksAwarded((BigDecimal) null);
                    }
                }
            }
            studentAnswerRepository.save(answer);
        }

        // AC-16: Score is computed strictly from stored answers server-side
        BigDecimal totalScore = BigDecimal.ZERO;
        for (StudentAnswer answer : answers) {
            if (answer.getMarksAwarded() != null) {
                totalScore = totalScore.add(answer.getMarksAwarded());
            }
        }
        attempt.setScore(totalScore);
        attempt.setStatus("COMPLETED");
        attempt.setCompletedAt(LocalDateTime.now());
        TestAttempt saved = testAttemptRepository.save(attempt);

        if (saved.getTest() != null && saved.getTest().getCourse() != null) {
            eventPublisher.publishEvent(new com.MOOC.OnlineLearningPlatfrom.Event.TestAttemptCompletedEvent(
                    saved.getAttemptId(),
                    saved.getTest().getTestId(),
                    saved.getTest().getCourse().getId(),
                    saved.getUserId()
            ));
        }

        // W4-11: Post-video quiz completion gate (BR-25, BR-10)
        Test test = saved.getTest();
        if (test != null && Boolean.TRUE.equals(test.getRequiredForCompletion())) {
            if (isPassingAttempt(test, saved)) {
                UserProgress prog = null;
                if (test.getContent() != null) {
                    prog = userProgressRepository.findByUserIdAndContent_ContentId(saved.getUserId(), test.getContent().getContentId()).orElse(null);
                }
                if (prog == null && test.getLecture() != null) {
                    prog = userProgressRepository.findByUserIdAndLecture_Id(saved.getUserId(), test.getLecture().getId()).orElse(null);
                }
                if (prog != null && prog.getProgressPercent() != null && prog.getProgressPercent().compareTo(BigDecimal.valueOf(90)) >= 0) {
                    if (prog.getCompletedAt() == null) {
                        prog.setCompletedAt(LocalDateTime.now());
                        userProgressRepository.save(prog);
                    }
                }
            }
        }

        return saved;
    }

    @Override
    public boolean isPassingAttempt(Test test, TestAttempt attempt) {
        if (test == null || attempt == null || !"COMPLETED".equalsIgnoreCase(attempt.getStatus())) {
            return false;
        }
        List<Question> questions = questionRepository.findByTest_TestId(test.getTestId());
        BigDecimal maxScore = questions != null
                ? questions.stream().map(Question::getMarks).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)
                : BigDecimal.ZERO;
        if (maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        BigDecimal score = attempt.getScore() != null ? attempt.getScore() : BigDecimal.ZERO;
        BigDecimal passThreshold = test.getPassThreshold() != null ? test.getPassThreshold() : new BigDecimal("60.00");
        BigDecimal percentage = score.multiply(new BigDecimal("100.00")).divide(maxScore, 2, RoundingMode.HALF_UP);
        return percentage.compareTo(passThreshold) >= 0;
    }

    @Override
    public boolean hasPassingAttempt(Long testId, Long userId) {
        Test test = testRepository.findById(testId).orElse(null);
        if (test == null || userId == null) {
            return false;
        }
        List<TestAttempt> attempts = testAttemptRepository.findByUserIdAndTest_TestId(userId, testId);
        if (attempts == null || attempts.isEmpty()) {
            return false;
        }
        for (TestAttempt att : attempts) {
            if (isPassingAttempt(test, att)) {
                return true;
            }
        }
        return false;
    }
}
