package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.AuditService;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin course-moderation: approve/reject a course pending review.
 * Bypasses the teacher-ownership check that CourseService.updateCourse enforces,
 * since an admin acts on any teacher's course. Audited to audit_log.
 */
@RestController
@RequestMapping("/api/admin/courses")
@PreAuthorize("hasRole('ADMIN')")
public class CourseAdminController {

    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final AuditService auditService;
    private final com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService publicCacheService;

    @org.springframework.beans.factory.annotation.Autowired
    public CourseAdminController(CourseRepository courseRepository,
                                 CourseService courseService,
                                 AuditService auditService,
                                 @org.springframework.beans.factory.annotation.Autowired(required = false)
                                 com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService publicCacheService) {
        this.courseRepository = courseRepository;
        this.courseService = courseService;
        this.auditService = auditService;
        this.publicCacheService = publicCacheService;
    }

    public CourseAdminController(CourseRepository courseRepository,
                                 CourseService courseService,
                                 AuditService auditService) {
        this(courseRepository, courseService, auditService, null);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CourseResponseDto> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body != null ? body.get("status") : null;
        if (status == null || status.isBlank()) {
            throw new BadRequestException("status is required");
        }
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        Course.Status oldStatus = course.getStatus();
        Course.Status newStatus;
        try {
            newStatus = Course.Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown course status: " + status);
        }

        course.setStatus(newStatus);
        courseRepository.save(course);

        if (auditService != null) {
            auditService.record(
                    AuditService.COURSE_STATUS_CHANGED,
                    AuditService.RESOURCE_COURSE,
                    course.getId(),
                    Map.of("status", oldStatus != null ? oldStatus.name() : "UNKNOWN"),
                    Map.of("status", newStatus.name())
            );
        }

        if (publicCacheService != null) {
            publicCacheService.invalidateCourse(id);
        }

        return ResponseEntity.ok(courseService.getCourseById(id));
    }
}
