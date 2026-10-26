package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserProgressRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final EnrollmentRepository enrollmentRepository;
    private final UserProgressRepository userProgressRepository;

    public EnrollmentController(EnrollmentService enrollmentService,
                                EnrollmentRepository enrollmentRepository,
                                UserProgressRepository userProgressRepository) {
        this.enrollmentService = enrollmentService;
        this.enrollmentRepository = enrollmentRepository;
        this.userProgressRepository = userProgressRepository;
    }

    @PostMapping("/enrollments")
    public ResponseEntity<Enrollment> enrollInCourse(
            @RequestBody Enrollment enrollment,
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (enrollment.getUserId() == null && principal != null && principal.getUser() != null) {
            enrollment.setUserId(principal.getUser().getUserId());
        }
        return ResponseEntity.ok(enrollmentService.enrollUser(enrollment));
    }

    @GetMapping("/enrollments/me")
    public ResponseEntity<List<Enrollment>> getMyEnrollments(@AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        return ResponseEntity.ok(enrollmentRepository.findByUserId(userId));
    }

    @DeleteMapping("/enrollments/{id}")
    public ResponseEntity<Void> unenrollFromCourse(@PathVariable Long id) {
        enrollmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/courses/{courseId}/enrollments")
    public ResponseEntity<List<Enrollment>> getCourseEnrollments(@PathVariable Long courseId) {
        return ResponseEntity.ok(enrollmentRepository.findByCourse_Id(courseId));
    }

    @PostMapping("/progress")
    public ResponseEntity<UserProgress> logUserProgress(
            @RequestBody UserProgress userProgress,
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (userProgress.getUserId() == null && principal != null && principal.getUser() != null) {
            userProgress.setUserId(principal.getUser().getUserId());
        }
        return ResponseEntity.ok(enrollmentService.updateUserProgress(userProgress));
    }

    @GetMapping("/progress/me")
    public ResponseEntity<List<UserProgress>> getMyProgress(@AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        return ResponseEntity.ok(userProgressRepository.findByUserId(userId));
    }

    @GetMapping("/progress/content/{contentId}")
    public ResponseEntity<UserProgress> getContentProgress(
            @PathVariable Long contentId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        UserProgress progress = userProgressRepository.findByUserIdAndContent_ContentId(userId, contentId)
                .orElse(null);
        return ResponseEntity.ok(progress);
    }

    @GetMapping("/progress/course/{courseId}/summary")
    public ResponseEntity<Map<String, Object>> getCourseProgressSummary(@PathVariable Long courseId) {
        long enrollmentCount = enrollmentRepository.countByCourse_Id(courseId);
        return ResponseEntity.ok(Map.of(
                "courseId", courseId,
                "totalEnrolled", enrollmentCount
        ));
    }
}

