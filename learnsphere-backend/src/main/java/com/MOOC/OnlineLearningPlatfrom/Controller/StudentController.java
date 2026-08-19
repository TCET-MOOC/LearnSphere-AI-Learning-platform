package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LectureResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserProgressResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserProgressRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final UserProgressRepository userProgressRepository;
    private final com.MOOC.OnlineLearningPlatfrom.Service.LiveSessionService liveSessionService;

    public StudentController(CourseService courseService,
                              EnrollmentRepository enrollmentRepository,
                              UserProgressRepository userProgressRepository,
                              com.MOOC.OnlineLearningPlatfrom.Service.LiveSessionService liveSessionService) {
        this.courseService = courseService;
        this.enrollmentRepository = enrollmentRepository;
        this.userProgressRepository = userProgressRepository;
        this.liveSessionService = liveSessionService;
    }

    @GetMapping("/live-sessions")
    public ResponseEntity<List<com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionResponseDto>> getLiveSessions(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(liveSessionService.getStudentSessions(principal));
    }

    @GetMapping("/live-sessions/{id}")
    public ResponseEntity<com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionResponseDto> getLiveSession(@PathVariable Long id) {
        return ResponseEntity.ok(liveSessionService.getSessionById(id));
    }

    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponseDto>> getEnrolledCourses(@AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        List<CourseResponseDto> courses = enrollmentRepository.findByUserId(userId).stream()
                .map(Enrollment::getCourse)
                .filter(c -> c != null)
                .map(c -> {
                    long lectureCount = courseService.getLectures(c.getId()).size();
                    long completedCount = userProgressRepository.findByUserIdAndLecture_Course_Id(userId, c.getId()).stream()
                            .filter(p -> p.getProgressPercent() != null && p.getProgressPercent() >= 100)
                            .count();
                    return CourseResponseDto.from(c, lectureCount, completedCount);
                })
                .toList();
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<CourseResponseDto> getEnrolledCourseDetail(@PathVariable Long id,
                                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        assertEnrolled(userId, id);
        Course c = courseService.getCourseEntity(id);
        long lectureCount = courseService.getLectures(id).size();
        long completedCount = userProgressRepository.findByUserIdAndLecture_Course_Id(userId, id).stream()
                .filter(p -> p.getProgressPercent() != null && p.getProgressPercent() >= 100)
                .count();
        return ResponseEntity.ok(CourseResponseDto.from(c, lectureCount, completedCount));
    }

    @GetMapping("/courses/{courseId}/progress")
    public ResponseEntity<List<UserProgressResponseDto>> getCourseProgress(@PathVariable Long courseId,
                                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        assertEnrolled(userId, courseId);
        List<UserProgressResponseDto> progressList = userProgressRepository.findByUserIdAndLecture_Course_Id(userId, courseId).stream()
                .map(UserProgressResponseDto::from)
                .toList();
        return ResponseEntity.ok(progressList);
    }

    @GetMapping("/courses/{courseId}/lectures/{lectureId}")
    public ResponseEntity<LectureResponseDto> getLecture(@PathVariable Long courseId,
                                                           @PathVariable Long lectureId,
                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        assertEnrolled(principal.getUser().getUserId(), courseId);
        LectureResponseDto lecture = courseService.getLectureById(lectureId);
        if (lecture.getCourseId() == null || !lecture.getCourseId().equals(courseId)) {
            throw new ResourceNotFoundException("Lecture not found in course " + courseId);
        }
        return ResponseEntity.ok(lecture);
    }

    @PostMapping("/lectures/{id}/complete")
    public ResponseEntity<UserProgressResponseDto> markLectureComplete(@PathVariable Long id,
                                                                         @AuthenticationPrincipal CustomUserDetails principal) {
        Lecture lecture = courseService.getLectureEntity(id);
        Long userId = principal.getUser().getUserId();
        UserProgress progress = userProgressRepository.findByUserIdAndLecture_Id(userId, id)
                .orElseGet(() -> {
                    UserProgress p = new UserProgress();
                    p.setUserId(userId);
                    p.setLecture(lecture);
                    return p;
                });
        progress.setProgressPercent(100);
        progress.setCompletedAt(LocalDateTime.now());
        if (lecture.getDuration() != null) {
            progress.setSecondsWatched(lecture.getDuration());
        }
        UserProgress saved = userProgressRepository.save(progress);
        return ResponseEntity.ok(UserProgressResponseDto.from(saved));
    }

    @PostMapping("/lectures/{id}/uncomplete")
    public ResponseEntity<UserProgressResponseDto> unmarkLectureComplete(@PathVariable Long id,
                                                                             @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        UserProgress progress = userProgressRepository.findByUserIdAndLecture_Id(userId, id)
                .orElseGet(() -> {
                    Lecture lecture = courseService.getLectureEntity(id);
                    UserProgress p = new UserProgress();
                    p.setUserId(userId);
                    p.setLecture(lecture);
                    return p;
                });
        progress.setProgressPercent(0);
        progress.setCompletedAt(null);
        progress.setSecondsWatched(0);
        UserProgress saved = userProgressRepository.save(progress);
        return ResponseEntity.ok(UserProgressResponseDto.from(saved));
    }

    @GetMapping("/lectures/{id}/progress")
    public ResponseEntity<UserProgressResponseDto> getProgress(@PathVariable Long id,
                                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        return ResponseEntity.ok(userProgressRepository.findByUserIdAndLecture_Id(userId, id)
                .map(UserProgressResponseDto::from)
                .orElseGet(() -> UserProgressResponseDto.empty(id)));
    }

    @PostMapping("/courses/{id}/enroll")
    public ResponseEntity<CourseResponseDto> enroll(@PathVariable Long id,
                                                      @AuthenticationPrincipal CustomUserDetails principal) {
        Course course = courseService.getCourseEntity(id);
        if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("This course requires payment — use the checkout flow.");
        }
        Long userId = principal.getUser().getUserId();
        if (!enrollmentRepository.existsByUserIdAndCourse_Id(userId, id)) {
            Enrollment enrollment = new Enrollment();
            enrollment.setUserId(userId);
            enrollment.setCourse(course);
            enrollment.setRole("STUDENT");
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollmentRepository.save(enrollment);
        }
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    private void assertEnrolled(Long userId, Long courseId) {
        if (!enrollmentRepository.existsByUserIdAndCourse_Id(userId, courseId)) {
            throw new ResourceNotFoundException("Course not found or not enrolled: " + courseId);
        }
    }
}
