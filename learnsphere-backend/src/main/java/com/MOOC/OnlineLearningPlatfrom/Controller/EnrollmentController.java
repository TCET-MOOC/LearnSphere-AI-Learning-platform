package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserProgressRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final EnrollmentRepository enrollmentRepository;
    private final UserProgressRepository userProgressRepository;
    private final CourseContentRepository courseContentRepository;
    private final OwnershipService ownershipService;

    public EnrollmentController(EnrollmentService enrollmentService,
                                EnrollmentRepository enrollmentRepository,
                                UserProgressRepository userProgressRepository,
                                CourseContentRepository courseContentRepository,
                                OwnershipService ownershipService) {
        this.enrollmentService = enrollmentService;
        this.enrollmentRepository = enrollmentRepository;
        this.userProgressRepository = userProgressRepository;
        this.courseContentRepository = courseContentRepository;
        this.ownershipService = ownershipService;
    }

    @PostMapping("/enrollments")
    public ResponseEntity<Enrollment> enrollInCourse(
            @RequestBody Enrollment enrollment,
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            throw new AccessDeniedException("Authentication required to enroll");
        }
        Long currentUserId = principal.getUser().getUserId();
        if (enrollment.getUserId() != null && !enrollment.getUserId().equals(currentUserId)) {
            if (!ownershipService.isAdmin(principal)) {
                throw new AccessDeniedException("Cannot enroll other users");
            }
        } else {
            enrollment.setUserId(currentUserId);
        }
        return ResponseEntity.ok(enrollmentService.enrollUser(enrollment));
    }

    @GetMapping("/enrollments/me")
    public ResponseEntity<List<Enrollment>> getMyEnrollments(@AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        return ResponseEntity.ok(enrollmentRepository.findByUserId(userId));
    }

    @DeleteMapping("/enrollments/{id}")
    public ResponseEntity<Void> unenrollFromCourse(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
        boolean isSelf = principal != null && principal.getUser() != null
                && principal.getUser().getUserId().equals(enrollment.getUserId());
        boolean isStaff = enrollment.getCourse() != null && ownershipService.isCourseStaff(enrollment.getCourse(), principal);
        if (!isSelf && !isStaff && !ownershipService.isAdmin(principal)) {
            throw new ResourceNotFoundException("Enrollment not found with id: " + id);
        }
        enrollmentRepository.delete(enrollment);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/courses/{courseId}/enrollments")
    public ResponseEntity<List<Enrollment>> getCourseEnrollments(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireCourseStaff(courseId, principal);
        return ResponseEntity.ok(enrollmentRepository.findByCourse_Id(courseId));
    }

    @PostMapping("/progress")
    public ResponseEntity<UserProgress> logUserProgress(
            @RequestBody UserProgress userProgress,
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (userProgress.getUserId() == null && principal != null && principal.getUser() != null) {
            userProgress.setUserId(principal.getUser().getUserId());
        } else if (userProgress.getUserId() != null) {
            ownershipService.requireSelf(userProgress.getUserId(), principal);
        }
        if (userProgress.getContent() != null && userProgress.getContent().getContentId() != null) {
            CourseContent content = courseContentRepository.findById(userProgress.getContent().getContentId()).orElse(null);
            if (content != null && content.getModule() != null && content.getModule().getCourse() != null) {
                ownershipService.requireEnrolled(content.getModule().getCourse().getId(), principal);
            }
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
    public ResponseEntity<Map<String, Object>> getCourseProgressSummary(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireCourseStaff(courseId, principal);
        long enrollmentCount = enrollmentRepository.countByCourse_Id(courseId);
        return ResponseEntity.ok(Map.of(
                "courseId", courseId,
                "totalEnrolled", enrollmentCount
        ));
    }
}
