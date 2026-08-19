package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.NotificationResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.StudentStandingDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LectureRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestAttemptRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserProgressRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Real backend for the teacher "student standings" page + at-risk nudge action.
 * {courseId} accepts either a real numeric course id, or "all" (the frontend's students
 * page currently only ever calls with 'all', aggregating standings across every course the
 * teacher owns) — any other non-numeric value (e.g. legacy hardcoded tab slugs used only by
 * the nudge call site) is treated the same as "all" so the request still resolves usefully
 * instead of 404ing.
 */
@RestController
@RequestMapping("/api/teacher/courses/{courseId}")
public class TeacherStudentsController {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureRepository lectureRepository;
    private final UserProgressRepository userProgressRepository;
    private final TestRepository testRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final UserAccountRepository userAccountRepository;
    private final NotificationService notificationService;

    public TeacherStudentsController(CourseRepository courseRepository,
                                      EnrollmentRepository enrollmentRepository,
                                      LectureRepository lectureRepository,
                                      UserProgressRepository userProgressRepository,
                                      TestRepository testRepository,
                                      TestAttemptRepository testAttemptRepository,
                                      UserAccountRepository userAccountRepository,
                                      NotificationService notificationService) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lectureRepository = lectureRepository;
        this.userProgressRepository = userProgressRepository;
        this.testRepository = testRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.userAccountRepository = userAccountRepository;
        this.notificationService = notificationService;
    }

    @GetMapping("/standings")
    public ResponseEntity<List<StudentStandingDto>> getStandings(@PathVariable String courseId,
                                                                   @AuthenticationPrincipal CustomUserDetails principal) {
        Long teacherId = principal.getUser().getUserId();
        List<Course> courses = resolveCourses(courseId, teacherId);

        List<Lecture> allLectures = courses.stream()
                .flatMap(c -> lectureRepository.findByCourse_IdOrderByNumberAsc(c.getId()).stream())
                .toList();
        long totalLectures = allLectures.size();
        Set<Long> lectureIds = new LinkedHashSet<>();
        allLectures.forEach(l -> lectureIds.add(l.getId()));

        List<Test> allTests = courses.stream()
                .flatMap(c -> testRepository.findByCourse_Id(c.getId()).stream())
                .toList();

        Set<Long> studentIds = new LinkedHashSet<>();
        courses.forEach(c -> enrollmentRepository.findByCourse_Id(c.getId())
                .forEach(e -> studentIds.add(e.getUserId())));

        List<StudentStandingDto> unranked = new ArrayList<>();
        for (Long studentId : studentIds) {
            UserAccount student = userAccountRepository.findById(studentId).orElse(null);
            if (student == null) {
                continue;
            }

            long lecturesWatched = userProgressRepository.findByUserId(studentId).stream()
                    .filter(p -> p.getLecture() != null && lectureIds.contains(p.getLecture().getId()))
                    .filter(p -> p.getProgressPercent() != null && p.getProgressPercent() >= 100)
                    .count();

            double scoreSum = 0;
            int scoredTests = 0;
            boolean isRemedial = false;
            for (Test test : allTests) {
                int maxMarks = 0;
                if (test.getQuestions() != null) {
                    for (Question q : test.getQuestions()) {
                        maxMarks += q.getMarks() != null ? q.getMarks() : 0;
                    }
                }
                List<TestAttempt> attempts = testAttemptRepository.findByUserIdAndTest_TestId(studentId, test.getTestId());
                TestAttempt best = attempts.stream()
                        .filter(a -> "COMPLETED".equalsIgnoreCase(a.getStatus()) && a.getScore() != null)
                        .max(Comparator.comparingInt(TestAttempt::getScore))
                        .orElse(null);
                if (best != null && maxMarks > 0) {
                    double percent = (best.getScore() * 100.0) / maxMarks;
                    scoreSum += percent;
                    scoredTests++;
                    if (Boolean.TRUE.equals(test.getIsRemedial()) && percent >= 40.0) {
                        isRemedial = true;
                    }
                }
            }
            double scorePercent = scoredTests > 0 ? scoreSum / scoredTests : 0.0;

            double lecturesRatio = totalLectures > 0 ? (double) lecturesWatched / totalLectures : 0.0;
            boolean isAtRisk = scorePercent < 40.0 || (totalLectures > 0 && lecturesRatio < 0.4);

            unranked.add(new StudentStandingDto(
                    studentId,
                    student.getFullName(),
                    student.getAvatarUrl(),
                    0,
                    Math.round(scorePercent * 10.0) / 10.0,
                    lecturesWatched,
                    totalLectures,
                    student.getLastActiveAt(),
                    isRemedial,
                    isAtRisk
            ));
        }

        unranked.sort(Comparator.comparingDouble(StudentStandingDto::getScorePercent).reversed());
        List<StudentStandingDto> ranked = new ArrayList<>();
        int rank = 1;
        for (StudentStandingDto dto : unranked) {
            ranked.add(new StudentStandingDto(dto.getStudentId(), dto.getName(), dto.getAvatarUrl(), rank++,
                    dto.getScorePercent(), dto.getLecturesWatched(), dto.getTotalLectures(), dto.getLastActiveAt(),
                    dto.getIsRemedial(), dto.getIsAtRisk()));
        }
        return ResponseEntity.ok(ranked);
    }

    @PostMapping("/nudge/{studentId}")
    public ResponseEntity<Map<String, Object>> nudge(@PathVariable String courseId,
                                                       @PathVariable Long studentId,
                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        Long teacherId = principal.getUser().getUserId();
        Course course = resolveNudgeCourse(courseId, studentId, teacherId);

        UserAccount student = userAccountRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        String courseTitle = course != null ? course.getTitle() : "your course";
        NotificationResponseDto notification = notificationService.create(
                student,
                "Your teacher sent you a nudge",
                "Your teacher noticed you're falling behind in \"" + courseTitle + "\" — check in on your progress and catch up when you can.",
                "NUDGE"
        );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Nudge sent",
                "notificationId", notification.getId()
        ));
    }

    /** Resolves the set of courses the standings should be computed over. */
    private List<Course> resolveCourses(String courseId, Long teacherId) {
        Long numericId = tryParseLong(courseId);
        if (numericId != null) {
            Course course = courseRepository.findById(numericId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + numericId));
            if (course.getTeacher() == null || !course.getTeacher().getUserId().equals(teacherId)) {
                throw new BadRequestException("You do not own this course.");
            }
            return List.of(course);
        }
        // "all", or any other non-numeric value (legacy tab slugs) — aggregate across every
        // course this teacher owns.
        return courseRepository.findByTeacher_UserId(teacherId);
    }

    /** Resolves which of the teacher's courses a nudge should reference. */
    private Course resolveNudgeCourse(String courseId, Long studentId, Long teacherId) {
        Long numericId = tryParseLong(courseId);
        if (numericId != null) {
            Course course = courseRepository.findById(numericId)
                    .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + numericId));
            if (course.getTeacher() == null || !course.getTeacher().getUserId().equals(teacherId)) {
                throw new BadRequestException("You do not own this course.");
            }
            return course;
        }
        // Non-numeric courseId (e.g. a legacy hardcoded tab slug from the frontend) — find any
        // course this teacher owns that the student is actually enrolled in.
        List<Course> ownedCourses = courseRepository.findByTeacher_UserId(teacherId);
        for (Course course : ownedCourses) {
            if (enrollmentRepository.existsByUserIdAndCourse_Id(studentId, course.getId())) {
                return course;
            }
        }
        if (ownedCourses.isEmpty()) {
            throw new ResourceNotFoundException("You do not have any courses yet.");
        }
        // Student isn't enrolled in any of the teacher's courses under this id — still allow the
        // nudge to go through (referencing the teacher's first course) rather than hard-failing,
        // since the frontend doesn't currently send a real course id here.
        return ownedCourses.get(0);
    }

    private Long tryParseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
