package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.AssessmentDtos.*;
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
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Student-facing assessment (test/quiz) endpoints. Wraps TestService and shapes
 * everything through student-safe DTOs so students never see correctAnswer pre-submission
 * or other students' attempts. Enforces private, no-store caching (NFR-03) and course enrolment checks.
 */
@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final TestService testService;
    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final QuestionRepository questionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final OwnershipService ownershipService;

    public AssessmentController(TestService testService,
                                 TestRepository testRepository,
                                 TestAttemptRepository testAttemptRepository,
                                 QuestionRepository questionRepository,
                                 StudentAnswerRepository studentAnswerRepository,
                                 OwnershipService ownershipService) {
        this.testService = testService;
        this.testRepository = testRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.questionRepository = questionRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.ownershipService = ownershipService;
    }

    @GetMapping
    public ResponseEntity<List<TestSummaryDto>> getAssessments(@RequestParam(required = false) Long courseId,
                                                               @RequestParam(required = false) Long lectureId,
                                                               @AuthenticationPrincipal CustomUserDetails principal) {
        List<Test> tests;
        if (lectureId != null) {
            tests = testRepository.findByLecture_Id(lectureId);
        } else if (courseId != null) {
            if (principal != null) {
                ownershipService.requireEnrolled(courseId, principal);
            }
            tests = testRepository.findByCourse_Id(courseId);
        } else {
            tests = testRepository.findAll();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(tests.stream().map(TestSummaryDto::from).toList());
    }

    @GetMapping("/lecture/{lectureId}")
    public ResponseEntity<List<TestSummaryDto>> getAssessmentsByLecture(@PathVariable Long lectureId) {
        List<Test> tests = testRepository.findByLecture_Id(lectureId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(tests.stream().map(TestSummaryDto::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestDetailDto> getAssessment(@PathVariable Long id,
                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        Test test = testService.getTestById(id);
        if (test.getCourse() != null) {
            ownershipService.requireEnrolled(test.getCourse().getId(), principal);
        }
        if (test.getQuestions() == null || test.getQuestions().isEmpty()) {
            List<Question> questions = questionRepository.findByTest_TestId(id);
            if (questions != null && !questions.isEmpty()) {
                test.setQuestions(questions);
            }
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(TestDetailDto.from(test));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<AttemptDto> start(@PathVariable Long id,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        Test test = testService.getTestById(id);
        if (test.getCourse() != null) {
            ownershipService.requireEnrolled(test.getCourse().getId(), principal);
        }
        TestAttempt attempt = testService.startTestAttempt(id, principal.getUser().getUserId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(AttemptDto.from(attempt));
    }

    @PostMapping("/attempts/{attemptId}/answers")
    public ResponseEntity<Map<String, Object>> submitAnswer(@PathVariable Long attemptId,
                                                              @RequestBody Map<String, Object> payload,
                                                              @AuthenticationPrincipal CustomUserDetails principal) {
        TestAttempt attempt = assertOwnedAttempt(attemptId, principal.getUser().getUserId());
        Object questionIdRaw = payload.get("questionId");
        if (questionIdRaw == null) {
            throw new BadRequestException("questionId is required");
        }
        Long questionId = Long.valueOf(questionIdRaw.toString());
        String answerText = payload.get("answerText") != null ? payload.get("answerText").toString() : "";

        StudentAnswer studentAnswer = new StudentAnswer();
        studentAnswer.setAnswerText(answerText);
        StudentAnswer saved = testService.submitAnswer(attemptId, questionId, studentAnswer);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(Map.of(
                        "studentAnswerId", saved.getStudentAnswerId(),
                        "questionId", questionId,
                        "answerText", saved.getAnswerText(),
                        "saved", true
                ));
    }

    @PostMapping("/attempts/{attemptId}/finalize")
    public ResponseEntity<AttemptReviewDto> finalizeAttempt(@PathVariable Long attemptId,
                                                              @AuthenticationPrincipal CustomUserDetails principal) {
        assertOwnedAttempt(attemptId, principal.getUser().getUserId());
        TestAttempt finalized = testService.calculateAndFinalizeScore(attemptId);
        List<StudentAnswer> answers = studentAnswerRepository.findByAttempt_AttemptId(attemptId);
        if (answers != null && !answers.isEmpty()) {
            finalized.setStudentAnswers(answers);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(AttemptReviewDto.from(finalized));
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<AttemptReviewDto> getAttemptReview(@PathVariable Long attemptId,
                                                               @AuthenticationPrincipal CustomUserDetails principal) {
        TestAttempt attempt = assertOwnedAttempt(attemptId, principal.getUser().getUserId());
        List<StudentAnswer> answers = studentAnswerRepository.findByAttempt_AttemptId(attemptId);
        if (answers != null && !answers.isEmpty()) {
            attempt.setStudentAnswers(answers);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(AttemptReviewDto.from(attempt));
    }

    private TestAttempt assertOwnedAttempt(Long attemptId, Long userId) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));
        if (attempt.getUserId() == null || !attempt.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Attempt not found with id: " + attemptId);
        }
        return attempt;
    }
}
