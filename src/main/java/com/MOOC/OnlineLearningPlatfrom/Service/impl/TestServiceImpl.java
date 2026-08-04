package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.StudentAnswer;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;
import com.MOOC.OnlineLearningPlatfrom.Repository.QuestionRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.StudentAnswerRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestAttemptRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TestServiceImpl implements TestService {

    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;

    public TestServiceImpl(TestRepository testRepository,
                           QuestionRepository questionRepository,
                           TestAttemptRepository testAttemptRepository,
                           StudentAnswerRepository studentAnswerRepository) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.studentAnswerRepository = studentAnswerRepository;
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
        return questionRepository.save(question);
    }

    @Override
    public TestAttempt startTestAttempt(Long testId, Long userId) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found with id: " + testId));
        TestAttempt newAttempt = new TestAttempt();
        newAttempt.setTest(test);
        newAttempt.setUserId(userId);
        newAttempt.setAttemptedAt(LocalDateTime.now());
        newAttempt.setStatus("IN_PROGRESS");
        return testAttemptRepository.save(newAttempt);
    }

    @Override
    public StudentAnswer submitAnswer(Long attemptId, Long questionId, StudentAnswer studentAnswer) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found with id: " + attemptId));
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with id: " + questionId));
        studentAnswer.setAttempt(attempt);
        studentAnswer.setQuestion(question);
        return studentAnswerRepository.save(studentAnswer);
    }

    @Override
    public TestAttempt calculateAndFinalizeScore(Long attemptId) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found with id: " + attemptId));
        List<StudentAnswer> answers = attempt.getStudentAnswers();
        int totalScore = 0;
        for (StudentAnswer answer : answers) {
            Question question = answer.getQuestion();
            int marksForQuestion = question != null && question.getMarks() != null ? question.getMarks() : 0;
            String answerText = answer.getAnswerText();
            boolean hasAnswer = answerText != null && !answerText.trim().isEmpty();
            int awarded = 0;
            if (hasAnswer && question != null) {
                String type = question.getQuestionType() != null ? question.getQuestionType().trim().toUpperCase() : "";
                if (type.equals("MCQ") || type.equals("MULTIPLE_CHOICE")) {
                    String correct = question.getCorrectAnswer();
                    if (correct != null && correct.trim().equalsIgnoreCase(answerText.trim())) {
                        awarded = marksForQuestion;
                    } else {
                        awarded = 0;
                    }
                } else {
                    // SHORT_ANSWER / ESSAY / other free-text types: no reliable auto-grading,
                    // so any non-empty answer earns full marks.
                    awarded = marksForQuestion;
                }
            }
            answer.setMarksAwarded(awarded);
            totalScore += awarded;
            studentAnswerRepository.save(answer);
        }
        attempt.setScore(totalScore);
        attempt.setStatus("COMPLETED");
        return testAttemptRepository.save(attempt);
    }
}
