package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin course-moderation: approve/reject a course pending review.
 * Bypasses the teacher-ownership check that CourseService.updateCourse enforces,
 * since an admin acts on any teacher's course.
 */
@RestController
@RequestMapping("/api/admin/courses")
public class CourseAdminController {

    private final CourseRepository courseRepository;
    private final CourseService courseService;

    public CourseAdminController(CourseRepository courseRepository, CourseService courseService) {
        this.courseRepository = courseRepository;
        this.courseService = courseService;
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<CourseResponseDto> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new BadRequestException("status is required");
        }
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        try {
            course.setStatus(Course.Status.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown course status: " + status);
        }
        courseRepository.save(course);
        return ResponseEntity.ok(courseService.getCourseById(id));
    }
}
