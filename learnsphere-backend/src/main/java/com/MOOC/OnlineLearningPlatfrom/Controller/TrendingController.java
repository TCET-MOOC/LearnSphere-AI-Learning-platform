package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.TrendingCourseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

/**
 * Public trending-courses endpoint — mounted under /api/courses/** which is already
 * permitAll (GET) in SecurityConfig. Ranked purely by live enrollment count, computed
 * on demand (no separate "trending" table/cache).
 */
@RestController
public class TrendingController {

    private static final int DEFAULT_LIMIT = 10;

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public TrendingController(CourseRepository courseRepository, EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @GetMapping("/api/courses/trending")
    public ResponseEntity<List<TrendingCourseDto>> getTrending(@RequestParam(required = false) Integer limit) {
        int n = (limit != null && limit > 0) ? limit : DEFAULT_LIMIT;

        List<Course> liveCourses = courseRepository.findByStatus(Course.Status.LIVE);

        List<Object[]> ranked = liveCourses.stream()
                .map(course -> new Object[]{course, enrollmentRepository.countByCourse_Id(course.getId())})
                .sorted(Comparator.comparingLong((Object[] o) -> (Long) o[1]).reversed())
                .limit(n)
                .toList();

        List<TrendingCourseDto> result = new java.util.ArrayList<>();
        int rank = 1;
        for (Object[] entry : ranked) {
            Course course = (Course) entry[0];
            long count = (Long) entry[1];
            result.add(new TrendingCourseDto(
                    course.getId(),
                    course.getTitle(),
                    course.getThumbnail(),
                    course.getTeacher() != null ? course.getTeacher().getFullName() : null,
                    course.getTeacher() != null ? course.getTeacher().getUserId() : null,
                    count,
                    rank++
            ));
        }
        return ResponseEntity.ok(result);
    }
}
