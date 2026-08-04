package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Teacher-only endpoints for building assessments (tests/quizzes) on their own courses.
 * Ownership of the target course is checked manually since /api/teacher/** here just requires
 * ROLE_TEACHER, not "owns this specific course".
 */
@RestController
@RequestMapping("/api/teacher/tests")
public class TeacherAssessmentController {

    private final TestService testService;
    private final TestRepository testRepository;
    private final CourseRepository courseRepository;

    public TeacherAssessmentController(TestService testService,
                                        TestRepository testRepository,
                                        CourseRepository courseRepository) {
        this.testService = testService;
        this.testRepository = testRepository;
        this.courseRepository = courseRepository;
    }

    @PostMapping
    public ResponseEntity<Test> createTest(@RequestBody Test test, @AuthenticationPrincipal CustomUserDetails principal) {
        if (test.getCourse() == null || test.getCourse().getId() == null) {
            throw new BadRequestException("A course is required to create a test.");
        }
        Course course = assertOwnedCourse(test.getCourse().getId(), principal.getUser().getUserId());
        test.setCourse(course);
        return ResponseEntity.ok(testService.createTest(test));
    }

    @PostMapping("/{testId}/questions")
    public ResponseEntity<Question> addQuestion(@PathVariable Long testId,
                                                 @RequestBody Question question,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));
        if (test.getCourse() == null) {
            throw new BadRequestException("This test is not linked to a course.");
        }
        assertOwnedCourse(test.getCourse().getId(), principal.getUser().getUserId());
        return ResponseEntity.ok(testService.addQuestionToTest(testId, question));
    }

    private Course assertOwnedCourse(Long courseId, Long teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        if (course.getTeacher() == null || !course.getTeacher().getUserId().equals(teacherId)) {
            throw new BadRequestException("You do not own this course.");
        }
        return course;
    }
}
