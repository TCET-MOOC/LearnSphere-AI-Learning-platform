package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.StudentAnswer;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.QuestionRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.StudentAnswerRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestAttemptRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.AuditService;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final OwnershipService ownershipService;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditService auditService;

    public TestController(TestService testService,
                          TestRepository testRepository,
                          TestAttemptRepository testAttemptRepository,
                          QuestionRepository questionRepository,
                          StudentAnswerRepository studentAnswerRepository,
                          CourseRepository courseRepository,
                          OwnershipService ownershipService,
                          ApplicationEventPublisher eventPublisher,
                          AuditService auditService) {
        this.testService = testService;
        this.testRepository = testRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.questionRepository = questionRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.courseRepository = courseRepository;
        this.ownershipService = ownershipService;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    // --- Course-scoped tests ---

    @GetMapping("/courses/{courseId}/tests")
    public ResponseEntity<List<Test>> getTestsByCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(testRepository.findByCourse_Id(courseId));
    }

    @PostMapping("/courses/{courseId}/tests")
    public ResponseEntity<Test> createTestUnderCourse(@PathVariable Long courseId,
                                                      @RequestBody Test test,
                                                      @AuthenticationPrincipal CustomUserDetails principal) {
        Course course = ownershipService.requireCourseStaff(courseId, principal);
        test.setCourse(course);
        return ResponseEntity.ok(testService.createTest(test));
    }

    // --- Test CRUD ---

    @Deprecated
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/tests")
    public ResponseEntity<Test> createTest(@RequestBody Test test,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        if (test.getCourse() != null && test.getCourse().getId() != null) {
            ownershipService.requireCourseStaff(test.getCourse().getId(), principal);
        } else if (!ownershipService.isAdmin(principal)) {
            throw new ResourceNotFoundException("Course not found with id: null");
        }
        return ResponseEntity.ok(testService.createTest(test));
    }

    @Deprecated
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tests")
    public ResponseEntity<List<Test>> getAllTests() {
        return ResponseEntity.ok(testService.getAllTests());
    }

    @GetMapping("/tests/{testId}")
    public ResponseEntity<Test> getTestById(@PathVariable Long testId) {
        return ResponseEntity.ok(testService.getTestById(testId));
    }

    @PutMapping("/tests/{testId}")
    public ResponseEntity<Test> updateTest(@PathVariable Long testId,
                                           @RequestBody Test testDetails,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireTestStaff(testId, principal);
        return ResponseEntity.ok(testService.updateTest(testId, testDetails));
    }

    @DeleteMapping("/tests/{testId}")
    public ResponseEntity<Void> deleteTest(@PathVariable Long testId,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireTestStaff(testId, principal);
        testService.deleteTest(testId);
        return ResponseEntity.noContent().build();
    }

    // --- Questions ---
    // Note: W4-01 retired legacy leaky question read endpoints:
    // GET /api/tests/{testId}/questions and GET /api/questions/{id}.
    // Student questions are queried via /api/assessments/{id} with correctAnswer hidden.

    @PostMapping("/tests/{testId}/questions")
    public ResponseEntity<Question> addQuestion(@PathVariable Long testId,
                                                @RequestBody Question question,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireTestStaff(testId, principal);
        return ResponseEntity.ok(testService.addQuestionToTest(testId, question));
    }

    @PutMapping("/questions/{id}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long id,
                                                   @RequestBody Question updated,
                                                   @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireQuestionStaff(id, principal);
        Question existing = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + id));
        if (updated.getBody() != null) existing.setBody(updated.getBody());
        if (updated.getQuestionType() != null) existing.setQuestionType(updated.getQuestionType());
        if (updated.getMarks() != null) existing.setMarks(updated.getMarks());
        if (updated.getOptions() != null) existing.setOptions(updated.getOptions());
        if (updated.getExtra() != null) existing.setExtra(updated.getExtra());
        if (updated.getCorrectAnswer() != null) existing.setCorrectAnswer(updated.getCorrectAnswer());
        existing.validateMcqCorrectAnswer();
        return ResponseEntity.ok(questionRepository.save(existing));
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id,
                                               @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireQuestionStaff(id, principal);
        questionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- Attempts ---
    // The student attempt flow (start, answer, submit, finalize, my attempts) lives only on
    // /api/assessments/** (W4-02). What remains here is staff/owner reads and grading.

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<TestAttempt> getAttemptById(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        TestAttempt attempt = ownershipService.requireAttemptOwnerOrStaff(attemptId, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(attempt);
    }

    @GetMapping("/tests/{testId}/attempts")
    public ResponseEntity<List<TestAttempt>> getAllAttemptsForTest(
            @PathVariable Long testId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireTestStaff(testId, principal);
        return ResponseEntity.ok(testAttemptRepository.findByTest_TestId(testId));
    }

    // --- Answers & Grading ---

    @GetMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<List<StudentAnswer>> getAttemptAnswers(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        TestAttempt attempt = ownershipService.requireAttemptOwnerOrStaff(attemptId, principal);
        List<StudentAnswer> answers = studentAnswerRepository.findByAttempt_AttemptId(attemptId);

        boolean isStaffOrAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_TEACHER") || a.getAuthority().equals("ROLE_INSTRUCTOR"));
        boolean isFinalized = "COMPLETED".equalsIgnoreCase(attempt.getStatus()) || "ABANDONED".equalsIgnoreCase(attempt.getStatus());

        if (!isStaffOrAdmin && !isFinalized) {
            // For student own attempt while still in-progress, mask marksAwarded so grading info does not leak
            answers = answers.stream().map(a -> {
                StudentAnswer masked = new StudentAnswer();
                masked.setStudentAnswerId(a.getStudentAnswerId());
                masked.setAnswerText(a.getAnswerText());
                masked.setAttempt(a.getAttempt());
                masked.setQuestion(a.getQuestion());
                masked.setMarksAwarded((java.math.BigDecimal) null);
                return masked;
            }).toList();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(answers);
    }

    @PatchMapping("/answers/{id}/grade")
    public ResponseEntity<StudentAnswer> gradeAnswer(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails principal) {
        StudentAnswer answer = studentAnswerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found with id: " + id));
        if (answer.getAttempt() != null && answer.getAttempt().getTest() != null) {
            ownershipService.requireTestStaff(answer.getAttempt().getTest().getTestId(), principal);
        } else if (!ownershipService.isAdmin(principal)) {
            throw new ResourceNotFoundException("Answer not found with id: " + id);
        }

        if (!body.containsKey("marksAwarded") || body.get("marksAwarded") == null) {
            throw new BadRequestException("marksAwarded is required");
        }
        java.math.BigDecimal marksAwarded;
        try {
            marksAwarded = new java.math.BigDecimal(body.get("marksAwarded").toString());
        } catch (Exception e) {
            throw new BadRequestException("marksAwarded must be a valid number");
        }

        java.math.BigDecimal maxMarks = answer.getQuestion() != null && answer.getQuestion().getMarks() != null
                ? answer.getQuestion().getMarks()
                : java.math.BigDecimal.valueOf(100);
        if (marksAwarded.compareTo(java.math.BigDecimal.ZERO) < 0 || marksAwarded.compareTo(maxMarks) > 0) {
            throw new BadRequestException("marksAwarded must be between 0 and " + maxMarks);
        }

        java.math.BigDecimal oldMarks = answer.getMarksAwarded();
        TestAttempt attempt = answer.getAttempt();
        java.math.BigDecimal oldScore = attempt != null ? attempt.getScore() : null;

        answer.setMarksAwarded(marksAwarded);
        StudentAnswer savedAnswer = studentAnswerRepository.save(answer);

        // Recalculate parent attempt score from stored answers only (AC-16, [D-F3])
        java.math.BigDecimal newTotal = null;
        if (attempt != null) {
            List<StudentAnswer> allAnswers = studentAnswerRepository.findByAttempt_AttemptId(attempt.getAttemptId());
            newTotal = allAnswers.stream()
                    .map(StudentAnswer::getMarksAwarded)
                    .filter(java.util.Objects::nonNull)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            attempt.setScore(newTotal);
            testAttemptRepository.save(attempt);

            // Re-trigger leaderboard recalculation if attempt is completed
            if ("COMPLETED".equalsIgnoreCase(attempt.getStatus()) && attempt.getTest() != null && attempt.getTest().getCourse() != null) {
                eventPublisher.publishEvent(new com.MOOC.OnlineLearningPlatfrom.Event.TestAttemptCompletedEvent(
                        attempt.getAttemptId(),
                        attempt.getTest().getTestId(),
                        attempt.getTest().getCourse().getId(),
                        attempt.getUserId()
                ));
            }
        }

        // Record audit log entry (AC-15, AC-16, W4-09)
        Map<String, Object> beforeMap = new java.util.LinkedHashMap<>();
        if (oldMarks != null) beforeMap.put("marksAwarded", oldMarks);
        if (oldScore != null) beforeMap.put("attemptScore", oldScore);

        Map<String, Object> afterMap = new java.util.LinkedHashMap<>();
        afterMap.put("marksAwarded", marksAwarded);
        if (newTotal != null) afterMap.put("attemptScore", newTotal);
        if (body.containsKey("feedback") && body.get("feedback") != null) {
            afterMap.put("feedback", body.get("feedback").toString());
        }

        auditService.record(
                AuditService.MANUAL_GRADE_AWARDED,
                AuditService.RESOURCE_STUDENT_ANSWER,
                id,
                beforeMap,
                afterMap
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(savedAnswer);
    }
}
