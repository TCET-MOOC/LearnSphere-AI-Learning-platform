package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.AssessmentDtos.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.StudentAnswer;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestAttemptRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Student-facing assessment (test/quiz) endpoints. Wraps the pre-existing TestService rather than
 * duplicating its logic, and shapes everything through DTOs so students never see correctAnswer
 * or other students' attempts.
 */
@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {

    private final TestService testService;
    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;

    public AssessmentController(TestService testService,
                                 TestRepository testRepository,
                                 TestAttemptRepository testAttemptRepository) {
        this.testService = testService;
        this.testRepository = testRepository;
        this.testAttemptRepository = testAttemptRepository;
    }

    @GetMapping
    public ResponseEntity<List<TestSummaryDto>> getAssessments(@RequestParam(required = false) Long courseId,
                                                               @RequestParam(required = false) Long lectureId) {
        List<Test> tests;
        if (lectureId != null) {
            tests = testRepository.findByLecture_Id(lectureId);
        } else if (courseId != null) {
            tests = testRepository.findByCourse_Id(courseId);
        } else {
            tests = testRepository.findAll();
        }
        return ResponseEntity.ok(tests.stream().map(TestSummaryDto::from).toList());
    }

    @GetMapping("/lecture/{lectureId}")
    public ResponseEntity<List<TestSummaryDto>> getAssessmentsByLecture(@PathVariable Long lectureId) {
        List<Test> tests = testRepository.findByLecture_Id(lectureId);
        return ResponseEntity.ok(tests.stream().map(TestSummaryDto::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TestDetailDto> getAssessment(@PathVariable Long id) {
        Test test = testService.getTestById(id);
        return ResponseEntity.ok(TestDetailDto.from(test));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<AttemptDto> start(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails principal) {
        TestAttempt attempt = testService.startTestAttempt(id, principal.getUser().getUserId());
        return ResponseEntity.ok(AttemptDto.from(attempt));
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
        return ResponseEntity.ok(Map.of(
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
        return ResponseEntity.ok(AttemptReviewDto.from(finalized));
    }

    @GetMapping("/attempts/{attemptId}")
    public ResponseEntity<AttemptReviewDto> getAttemptReview(@PathVariable Long attemptId,
                                                               @AuthenticationPrincipal CustomUserDetails principal) {
        TestAttempt attempt = assertOwnedAttempt(attemptId, principal.getUser().getUserId());
        return ResponseEntity.ok(AttemptReviewDto.from(attempt));
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
