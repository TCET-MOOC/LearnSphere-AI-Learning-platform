package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;

import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.StudentAnswer;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.QuestionRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.StudentAnswerRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestAttemptRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    private final TestService testService;
    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final QuestionRepository questionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final CourseRepository courseRepository;

    public TestController(TestService testService,
                          TestRepository testRepository,
                          TestAttemptRepository testAttemptRepository,
                          QuestionRepository questionRepository,
                          StudentAnswerRepository studentAnswerRepository,
                          CourseRepository courseRepository) {
        this.testService = testService;
        this.testRepository = testRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.questionRepository = questionRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.courseRepository = courseRepository;
    }

    // --- Course-scoped tests ---

    @GetMapping("/courses/{courseId}/tests")
    public ResponseEntity<List<Test>> getTestsByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(testRepository.findByCourse_Id(courseId));
    }

    @PostMapping("/courses/{courseId}/tests")
    public ResponseEntity<Test> createTestUnderCourse(@PathVariable Long courseId, @RequestBody Test test) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        test.setCourse(course);
        return ResponseEntity.ok(testService.createTest(test));
    }

    // --- Test CRUD ---

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

    // --- Questions ---

    @GetMapping("/tests/{testId}/questions")
    public ResponseEntity<List<Question>> getTestQuestions(@PathVariable Long testId) {
        Test test = testService.getTestById(testId);
        return ResponseEntity.ok(test.getQuestions());
    }

    @PostMapping("/tests/{testId}/questions")
    public ResponseEntity<Question> addQuestion(@PathVariable Long testId, @RequestBody Question question) {
        return ResponseEntity.ok(testService.addQuestionToTest(testId, question));
    }

    @GetMapping("/questions/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
        return ResponseEntity.ok(question);
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id, @RequestBody Question updated) {
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
        if (updated.getBody() != null) existing.setBody(updated.getBody());
        if (updated.getQuestionType() != null) existing.setQuestionType(updated.getQuestionType());
        if (updated.getMarks() != null) existing.setMarks(updated.getMarks());
        if (updated.getCorrectAnswer() != null) existing.setCorrectAnswer(updated.getCorrectAnswer());
        if (updated.getExtra() != null) existing.setExtra(updated.getExtra());
        return ResponseEntity.ok(questionRepository.save(existing));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- Attempts ---

    @PostMapping("/tests/{testId}/attempts")
    public ResponseEntity<TestAttempt> startTest(
            @PathVariable Long testId,
            @RequestBody(required = false) Map<String, Long> payload,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId()
                : (payload != null && payload.get("userId") != null ? payload.get("userId") : 1L);
        return ResponseEntity.ok(testService.startTestAttempt(testId, userId));
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<TestAttempt> getAttemptById(@PathVariable Long attemptId) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));
        return ResponseEntity.ok(attempt);
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ResponseEntity<TestAttempt> submitTest(@PathVariable Long attemptId) {
        return ResponseEntity.ok(testService.calculateAndFinalizeScore(attemptId));
    }

    @PostMapping("/attempts/{attemptId}/finalize")
    public ResponseEntity<TestAttempt> finalizeTest(@PathVariable Long attemptId) {
        return ResponseEntity.ok(testService.calculateAndFinalizeScore(attemptId));
    }

    @GetMapping("/tests/{testId}/attempts/me")
    public ResponseEntity<List<TestAttempt>> getMyAttempts(
            @PathVariable Long testId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        return ResponseEntity.ok(testAttemptRepository.findByUserIdAndTest_TestId(userId, testId));
    }

    @GetMapping("/tests/{testId}/attempts")
    public ResponseEntity<List<TestAttempt>> getAllAttemptsForTest(@PathVariable Long testId) {
        return ResponseEntity.ok(testAttemptRepository.findByTest_TestId(testId));
    }

    // --- Answers & Grading ---

    @PostMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<StudentAnswer> submitAnswer(@PathVariable Long attemptId, @RequestBody StudentAnswer studentAnswer) {
        return ResponseEntity.ok(testService.submitAnswer(attemptId, studentAnswer.getQuestion().getQuestionId(), studentAnswer));
    }

    @GetMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<List<StudentAnswer>> getAttemptAnswers(@PathVariable Long attemptId) {
        return ResponseEntity.ok(studentAnswerRepository.findByAttempt_AttemptId(attemptId));
    }

    @PatchMapping("/answers/{id}/grade")
    public ResponseEntity<StudentAnswer> gradeAnswer(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        StudentAnswer answer = studentAnswerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + id));
        if (body.containsKey("marksAwarded")) {
            answer.setMarksAwarded(Integer.valueOf(body.get("marksAwarded").toString()));
        }
        return ResponseEntity.ok(studentAnswerRepository.save(answer));
    }
}

