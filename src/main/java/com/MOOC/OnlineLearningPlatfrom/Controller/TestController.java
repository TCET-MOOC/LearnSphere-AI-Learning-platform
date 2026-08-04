package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.StudentAnswer;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @PostMapping("/tests")
    public ResponseEntity<Test> createTest(@RequestBody Test test) {
        return ResponseEntity.ok(testService.createTest(test));
    }

    @GetMapping("/tests")
    public ResponseEntity<List<Test>> getAllTests() {
        return ResponseEntity.ok(testService.getAllTests());
    }

    @GetMapping("/tests/{testId}")
    public ResponseEntity<Test> getTestById(@PathVariable Long testId) {
        return ResponseEntity.ok(testService.getTestById(testId));
    }

    @PutMapping("/tests/{testId}")
    public ResponseEntity<Test> updateTest(@PathVariable Long testId, @RequestBody Test testDetails) {
        return ResponseEntity.ok(testService.updateTest(testId, testDetails));
    }

    @DeleteMapping("/tests/{testId}")
    public ResponseEntity<Void> deleteTest(@PathVariable Long testId) {
        testService.deleteTest(testId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tests/{testId}/questions")
    public ResponseEntity<Question> addQuestion(@PathVariable Long testId, @RequestBody Question question) {
        return ResponseEntity.ok(testService.addQuestionToTest(testId, question));
    }

    @PostMapping("/tests/{testId}/attempts")
    public ResponseEntity<TestAttempt> startTest(@PathVariable Long testId, @RequestBody Map<String, Long> payload) {
        return ResponseEntity.ok(testService.startTestAttempt(testId, payload.get("userId")));
    }

    @PostMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<StudentAnswer> submitAnswer(@PathVariable Long attemptId, @RequestBody StudentAnswer studentAnswer) {
        return ResponseEntity.ok(testService.submitAnswer(attemptId, studentAnswer.getQuestion().getQuestionId(), studentAnswer));
    }

    @PostMapping("/attempts/{attemptId}/finalize")
    public ResponseEntity<TestAttempt> finalizeTest(@PathVariable Long attemptId) {
        return ResponseEntity.ok(testService.calculateAndFinalizeScore(attemptId));
    }
}
