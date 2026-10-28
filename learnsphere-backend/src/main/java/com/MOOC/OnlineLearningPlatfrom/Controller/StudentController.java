package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LectureResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserProgressResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserProgressRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final CourseService courseService;
    private final EnrollmentRepository enrollmentRepository;
    private final UserProgressRepository userProgressRepository;
    private final com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository courseContentRepository;
    private final com.MOOC.OnlineLearningPlatfrom.Service.LiveSessionService liveSessionService;
    private final com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService enrollmentService;

    public StudentController(CourseService courseService,
                              EnrollmentRepository enrollmentRepository,
                              UserProgressRepository userProgressRepository,
                              com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository courseContentRepository,
                              com.MOOC.OnlineLearningPlatfrom.Service.LiveSessionService liveSessionService,
                              com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService enrollmentService) {
        this.courseService = courseService;
        this.enrollmentRepository = enrollmentRepository;
        this.userProgressRepository = userProgressRepository;
        this.courseContentRepository = courseContentRepository;
        this.liveSessionService = liveSessionService;
        this.enrollmentService = enrollmentService;
    }

    @GetMapping("/live-sessions")
    public ResponseEntity<List<com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionResponseDto>> getLiveSessions(@AuthenticationPrincipal CustomUserDetails principal) {
        requireUserId(principal);
        return ResponseEntity.ok(liveSessionService.getStudentSessions(principal));
    }

    @GetMapping("/live-sessions/{id}")
    public ResponseEntity<com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionResponseDto> getLiveSession(@PathVariable Long id,
                                                                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        requireUserId(principal);
        return ResponseEntity.ok(liveSessionService.getSessionById(id));
    }

    @GetMapping("/courses")
    public ResponseEntity<List<CourseResponseDto>> getEnrolledCourses(@AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = requireUserId(principal);
        List<CourseResponseDto> courses = enrollmentRepository.findByUserId(userId).stream()
                .map(Enrollment::getCourse)
                .filter(c -> c != null)
                .map(c -> {
                    var calc = enrollmentService.calculateCourseProgress(c.getId(), userId);
                    return CourseResponseDto.from(c, calc.getTotalContents(), calc.getCompletedContents());
                })
                .toList();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(courses);
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<CourseResponseDto> getEnrolledCourseDetail(@PathVariable Long id,
                                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = requireUserId(principal);
        assertEnrolled(userId, id);
        Course c = courseService.getCourseEntity(id);
        var calc = enrollmentService.calculateCourseProgress(id, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(CourseResponseDto.from(c, calc.getTotalContents(), calc.getCompletedContents()));
    }

    @GetMapping("/courses/{courseId}/progress")
    public ResponseEntity<List<UserProgressResponseDto>> getCourseProgress(@PathVariable Long courseId,
                                                                            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = requireUserId(principal);
        assertEnrolled(userId, courseId);
        List<UserProgressResponseDto> progressList = enrollmentService.getStudentCourseProgress(courseId, userId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(progressList);
    }

    @GetMapping("/courses/{courseId}/resume")
    public ResponseEntity<com.MOOC.OnlineLearningPlatfrom.Dto.ResumeTargetDto> getResumeTarget(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = requireUserId(principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(enrollmentService.getResumeTarget(courseId, userId));
    }

    @GetMapping("/courses/{courseId}/lectures/{lectureId}")
    public ResponseEntity<LectureResponseDto> getLecture(@PathVariable Long courseId,
                                                           @PathVariable Long lectureId,
                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        assertEnrolled(requireUserId(principal), courseId);
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
        Long userId = requireUserId(principal);
        CourseContent content = courseContentRepository.findByLegacyLectureId(id).orElse(null);
        UserProgress progress = null;
        if (content != null) {
            progress = userProgressRepository.findByUserIdAndContent_ContentId(userId, content.getContentId()).orElse(null);
        }
        if (progress == null) {
            progress = userProgressRepository.findByUserIdAndLecture_Id(userId, id).orElseGet(() -> {
                UserProgress p = new UserProgress();
                p.setUserId(userId);
                p.setLecture(lecture);
                return p;
            });
        }
        progress.setProgressPercent(BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP));
        if (progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
        }
        if (lecture.getDuration() != null) {
            progress.setSecondsWatched(lecture.getDuration());
        }
        if (progress.getContent() == null && content != null) {
            progress.setContent(content);
        }
        if (progress.getLecture() == null) {
            progress.setLecture(lecture);
        }
        UserProgress saved = userProgressRepository.save(progress);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(UserProgressResponseDto.from(saved));
    }

    @PostMapping("/lectures/{id}/uncomplete")
    public ResponseEntity<UserProgressResponseDto> unmarkLectureComplete(@PathVariable Long id,
                                                                             @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = requireUserId(principal);
        Lecture lecture = courseService.getLectureEntity(id);
        CourseContent content = courseContentRepository.findByLegacyLectureId(id).orElse(null);
        UserProgress progress = null;
        if (content != null) {
            progress = userProgressRepository.findByUserIdAndContent_ContentId(userId, content.getContentId()).orElse(null);
        }
        if (progress == null) {
            progress = userProgressRepository.findByUserIdAndLecture_Id(userId, id).orElseGet(() -> {
                UserProgress p = new UserProgress();
                p.setUserId(userId);
                p.setLecture(lecture);
                return p;
            });
        }
        progress.setProgressPercent(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        progress.setCompletedAt(null);
        progress.setSecondsWatched(0);
        if (progress.getContent() == null && content != null) {
            progress.setContent(content);
        }
        if (progress.getLecture() == null) {
            progress.setLecture(lecture);
        }
        UserProgress saved = userProgressRepository.save(progress);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(UserProgressResponseDto.from(saved));
    }

    @GetMapping("/lectures/{id}/progress")
    public ResponseEntity<UserProgressResponseDto> getProgress(@PathVariable Long id,
                                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = requireUserId(principal);
        CourseContent content = courseContentRepository.findByLegacyLectureId(id).orElse(null);
        Optional<UserProgress> progressOpt = Optional.empty();
        if (content != null) {
            progressOpt = userProgressRepository.findByUserIdAndContent_ContentId(userId, content.getContentId());
        }
        if (progressOpt.isEmpty()) {
            progressOpt = userProgressRepository.findByUserIdAndLecture_Id(userId, id);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(progressOpt.map(UserProgressResponseDto::from).orElseGet(() -> UserProgressResponseDto.empty(id)));
    }

    @PostMapping("/courses/{id}/enroll")
    public ResponseEntity<CourseResponseDto> enroll(@PathVariable Long id,
                                                      @AuthenticationPrincipal CustomUserDetails principal) {
        Course course = courseService.getCourseEntity(id);
        if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            throw new BadRequestException("This course requires payment — use the checkout flow.");
        }
        Long userId = requireUserId(principal);
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

    private Long requireUserId(CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return principal.getUser().getUserId();
    }
}
