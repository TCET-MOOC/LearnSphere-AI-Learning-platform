package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.StudentAnswer;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;

import java.util.List;

public interface TestService {
    Test createTest(Test test);
    List<Test> getAllTests();
    Test getTestById(Long testId);
    Test updateTest(Long testId, Test testDetails);
    void deleteTest(Long testId);
    Question addQuestionToTest(Long testId, Question question);
    TestAttempt startTestAttempt(Long testId, Long userId);
    StudentAnswer submitAnswer(Long attemptId, Long questionId, StudentAnswer studentAnswer);
    TestAttempt calculateAndFinalizeScore(Long attemptId);
}
