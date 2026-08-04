package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LectureResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    // --- Public course browsing ---

    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponseDto>> getCourses(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Long teacherId) {
        return ResponseEntity.ok(courseService.getCourses(status, department, teacherId));
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<CourseResponseDto> getCourse(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @GetMapping("/courses/{id}/lectures")
    public ResponseEntity<List<LectureResponseDto>> getLectures(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getLectures(id));
    }

    // --- Teacher-only course management ---

    @GetMapping("/teacher/courses")
    public ResponseEntity<List<CourseResponseDto>> getMyCourses(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.getCourses(null, null, principal.getUser().getUserId()));
    }

    @PostMapping("/teacher/courses")
    public ResponseEntity<CourseResponseDto> createCourse(@RequestBody Course course,
                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.createCourse(course, principal));
    }

    @PutMapping("/teacher/courses/{id}")
    public ResponseEntity<CourseResponseDto> updateCourse(@PathVariable Long id,
                                                            @RequestBody Course course,
                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.updateCourse(id, course, principal));
    }

    @DeleteMapping("/teacher/courses/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id,
                                              @AuthenticationPrincipal CustomUserDetails principal) {
        courseService.deleteCourse(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/teacher/courses/{id}/lectures")
    public ResponseEntity<LectureResponseDto> addLecture(@PathVariable Long id,
                                                           @RequestBody Lecture lecture,
                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.addLecture(id, lecture, principal));
    }

    @PutMapping("/teacher/lectures/{id}")
    public ResponseEntity<LectureResponseDto> updateLecture(@PathVariable Long id,
                                                              @RequestBody Lecture lecture,
                                                              @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(courseService.updateLecture(id, lecture, principal));
    }

    @DeleteMapping("/teacher/lectures/{id}")
    public ResponseEntity<Void> deleteLecture(@PathVariable Long id,
                                               @AuthenticationPrincipal CustomUserDetails principal) {
        courseService.deleteLecture(id, principal);
        return ResponseEntity.noContent().build();
    }
}
