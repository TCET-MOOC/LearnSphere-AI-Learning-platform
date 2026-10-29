package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.StudentAnswer;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.QuestionRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.StudentAnswerRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestAttemptRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TestServiceImpl implements TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TestServiceImpl(TestRepository testRepository,
                           QuestionRepository questionRepository,
                           TestAttemptRepository testAttemptRepository,
                           StudentAnswerRepository studentAnswerRepository,
                           ApplicationEventPublisher eventPublisher) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.studentAnswerRepository = studentAnswerRepository;
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
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));
    }

    @Override
    public Test updateTest(Long testId, Test testDetails) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));
        test.setTitle(testDetails.getTitle());
        test.setDurationMinutes(testDetails.getDurationMinutes());
        test.setSecurityPolicy(testDetails.getSecurityPolicy());
        test.setScheduledAt(testDetails.getScheduledAt());
        if (testDetails.getCourse() != null) {
            test.setCourse(testDetails.getCourse());
        }
        if (testDetails.getIsRemedial() != null) {
            test.setIsRemedial(testDetails.getIsRemedial());
        }
        return testRepository.save(test);
    }

    @Override
    public void deleteTest(Long testId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));
        testRepository.delete(test);
    }

    @Override
    public Question addQuestionToTest(Long testId, Question question) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));
        question.setTest(test);
        question.validateMcqCorrectAnswer();
        return questionRepository.save(question);
    }

    @Override
    public TestAttempt startTestAttempt(Long testId, Long userId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));
        TestAttempt newAttempt = new TestAttempt();
        newAttempt.setTest(test);
        newAttempt.setUserId(userId);
        LocalDateTime now = LocalDateTime.now();
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

        Optional<StudentAnswer> existingOpt = studentAnswerRepository
                .findByAttempt_AttemptIdAndQuestion_QuestionId(attemptId, questionId);

        StudentAnswer toSave;
        if (existingOpt.isPresent()) {
            toSave = existingOpt.get();
            toSave.setAnswerText(studentAnswer.getAnswerText());
            toSave.setMarksAwarded(null);
        } else {
            toSave = studentAnswer;
            toSave.setAttempt(attempt);
            toSave.setQuestion(question);
        }

        return studentAnswerRepository.save(toSave);
    }

    @Override
    public TestAttempt calculateAndFinalizeScore(Long attemptId) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));

        if (!"IN_PROGRESS".equalsIgnoreCase(attempt.getStatus())) {
            throw new BadRequestException("Attempt is already finalized (status: " + attempt.getStatus() + ")");
        }

        List<StudentAnswer> answers = studentAnswerRepository.findByAttempt_AttemptId(attemptId);
        if (answers == null || answers.isEmpty()) {
            answers = attempt.getStudentAnswers() != null ? attempt.getStudentAnswers() : java.util.List.of();
        }

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
                    int marksForQuestion = question != null && question.getMarks() != null ? question.getMarks() : 0;
                    String answerText = answer.getAnswerText();
                    boolean hasAnswer = answerText != null && !answerText.trim().isEmpty();
                    if (question != null) {
                        String type = question.getQuestionType() != null ? question.getQuestionType().trim().toUpperCase() : "";
                        boolean isAutoGradable = type.equals("MCQ") || type.equals("MULTIPLE_CHOICE")
                                || type.equals("TF") || type.equals("TRUE_FALSE") || type.equals("BOOLEAN");
                        if (isAutoGradable) {
                            int awarded = 0;
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
                int totalScore = 0;
                for (StudentAnswer answer : answers) {
                    if (answer.getMarksAwarded() != null) {
                        totalScore += answer.getMarksAwarded();
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
            int marksForQuestion = question != null && question.getMarks() != null ? question.getMarks() : 0;
            String answerText = answer.getAnswerText();
            boolean hasAnswer = answerText != null && !answerText.trim().isEmpty();

            if (question != null) {
                String type = question.getQuestionType() != null ? question.getQuestionType().trim().toUpperCase() : "";
                boolean isAutoGradable = type.equals("MCQ") || type.equals("MULTIPLE_CHOICE")
                        || type.equals("TF") || type.equals("TRUE_FALSE") || type.equals("BOOLEAN");

                if (isAutoGradable) {
                    int awarded = 0;
                    if (hasAnswer) {
                        String correct = question.getCorrectAnswer();
                        if (correct != null && correct.trim().equalsIgnoreCase(answerText.trim())) {
                            awarded = marksForQuestion;
                        }
                    }
                    answer.setMarksAwarded(awarded);
                } else {
                    // AC-17, [D-F3]: TEXT and subjective answers remain ungraded (null) until an instructor grades them.
                    // Preserve manual grade if one was already assigned, otherwise leave as null.
                    if (answer.getMarksAwarded() == null) {
                        answer.setMarksAwarded(null);
                    }
                }
            }
            studentAnswerRepository.save(answer);
        }

        // AC-16: Score is computed strictly from stored answers server-side
        int totalScore = 0;
        for (StudentAnswer answer : answers) {
            if (answer.getMarksAwarded() != null) {
                totalScore += answer.getMarksAwarded();
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

        return saved;
    }
}
