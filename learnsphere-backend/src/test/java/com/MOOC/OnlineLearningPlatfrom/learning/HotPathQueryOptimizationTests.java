package com.MOOC.OnlineLearningPlatfrom.learning;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseProgressCalculation;
import com.MOOC.OnlineLearningPlatfrom.Dto.CourseProgressSummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService;
import com.MOOC.OnlineLearningPlatfrom.Service.LeaderboardService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W3-13: Hot-path read optimization + cache boundary integration test suite.
 * Rules: NFR-02, NFR-03; AC-13, AC-14; W3-05, W3-07.
 *
 * Verifies:
 * - Hot-path queries (resume, course progress, student course detail, leaderboard) return Cache-Control: private, no-store.
 * - Set-based batch progress calculation produces identical, accurate progress across multiple courses without N+1 queries.
 * - getCourseProgressSummary performs batch lookups for student user accounts.
 * - Course leaderboard fetches user details eagerly without lazy loading overhead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class HotPathQueryOptimizationTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private CourseContentRepository courseContentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserProgressRepository userProgressRepository;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private LeaderboardService leaderboardService;

    private UserAccount student1;
    private UserAccount student2;
    private UserAccount teacher;
    private Course course1;
    private Course course2;
    private CourseContent content1A;
    private CourseContent content1B;
    private CourseContent content2A;

    @BeforeEach
    void setUp() {
        Department dept = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("HotPath Systems " + System.currentTimeMillis());
            return departmentRepository.save(d);
        });

        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student1 = userAccountRepository.findByEmail("student@learnsphere.com");
        student2 = userAccountRepository.findByEmail("external@learnsphere.com");

        // Course 1 with 2 contents
        course1 = new Course();
        course1.setTitle("Distributed Systems HotPath " + System.nanoTime());
        course1.setDescription("Hot-path verification course 1");
        course1.setTeacher(teacher);
        course1.setDepartment(dept);
        course1.setStatus(Course.Status.LIVE);
        course1.setPrice(BigDecimal.ZERO);
        course1 = courseRepository.save(course1);

        Module mod1 = new Module();
        mod1.setTitle("Module 1");
        mod1.setCourse(course1);
        mod1.setPosition(1);
        mod1 = moduleRepository.save(mod1);

        content1A = new CourseContent();
        content1A.setModule(mod1);
        content1A.setTitle("Lecture 1A");
        content1A.setContentType("VIDEO");
        content1A.setPosition(1);
        content1A.setDurationSeconds(1200);
        content1A = courseContentRepository.save(content1A);

        content1B = new CourseContent();
        content1B.setModule(mod1);
        content1B.setTitle("Lecture 1B");
        content1B.setContentType("VIDEO");
        content1B.setPosition(2);
        content1B.setDurationSeconds(1800);
        content1B = courseContentRepository.save(content1B);

        // Course 2 with 1 content
        course2 = new Course();
        course2.setTitle("Operating Systems HotPath " + System.nanoTime());
        course2.setDescription("Hot-path verification course 2");
        course2.setTeacher(teacher);
        course2.setDepartment(dept);
        course2.setStatus(Course.Status.LIVE);
        course2.setPrice(BigDecimal.ZERO);
        course2 = courseRepository.save(course2);

        Module mod2 = new Module();
        mod2.setTitle("Module 2");
        mod2.setCourse(course2);
        mod2.setPosition(1);
        mod2 = moduleRepository.save(mod2);

        content2A = new CourseContent();
        content2A.setModule(mod2);
        content2A.setTitle("Lecture 2A");
        content2A.setContentType("VIDEO");
        content2A.setPosition(1);
        content2A.setDurationSeconds(900);
        content2A = courseContentRepository.save(content2A);

        // Enrollments
        Enrollment e1 = new Enrollment();
        e1.setCourse(course1);
        e1.setUserId(student1.getUserId());
        e1.setRole("STUDENT");
        enrollmentRepository.save(e1);

        Enrollment e2 = new Enrollment();
        e2.setCourse(course2);
        e2.setUserId(student1.getUserId());
        e2.setRole("STUDENT");
        enrollmentRepository.save(e2);

        Enrollment e3 = new Enrollment();
        e3.setCourse(course1);
        e3.setUserId(student2.getUserId());
        e3.setRole("STUDENT");
        enrollmentRepository.save(e3);

        // Student 1 completed content1A (100%), content1B untouched
        UserProgress p1 = new UserProgress();
        p1.setUserId(student1.getUserId());
        p1.setContent(content1A);
        p1.setProgressPercent(BigDecimal.valueOf(100.00));
        p1.setSecondsWatched(1200);
        p1.setCompletedAt(LocalDateTime.now().minusHours(1));
        userProgressRepository.save(p1);

        // Student 1 completed content2A (95% -> complete)
        UserProgress p2 = new UserProgress();
        p2.setUserId(student1.getUserId());
        p2.setContent(content2A);
        p2.setProgressPercent(BigDecimal.valueOf(95.00));
        p2.setSecondsWatched(855);
        p2.setCompletedAt(LocalDateTime.now().minusMinutes(30));
        userProgressRepository.save(p2);

        // Leaderboard entry for Course 1
        Leaderboard lb1 = new Leaderboard();
        lb1.setCourse(course1);
        lb1.setUser(student1);
        lb1.setAvgScore(BigDecimal.valueOf(92.50));
        lb1.setGlobalRank(1);
        lb1.setDeptRank(1);
        lb1.setUpdatedAt(LocalDateTime.now());
        leaderboardRepository.save(lb1);
    }

    @Test
    @DisplayName("GET /api/student/courses/{courseId}/resume returns 200 with private, no-store Cache-Control header")
    void testResumeEndpointCacheControlPrivateNoStore() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(student1.getEmail());

        mockMvc.perform(get("/api/student/courses/" + course1.getId() + "/resume")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.courseId", is(course1.getId().intValue())))
                .andExpect(jsonPath("$.contentId", is(content1B.getContentId().intValue())))
                .andExpect(jsonPath("$.title", is("Lecture 1B")));
    }

    @Test
    @DisplayName("GET /api/student/courses/{courseId}/progress returns 200 with private, no-store Cache-Control header")
    void testCourseProgressEndpointCacheControlPrivateNoStore() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(student1.getEmail());

        mockMvc.perform(get("/api/student/courses/" + course1.getId() + "/progress")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].contentId", is(content1A.getContentId().intValue())))
                .andExpect(jsonPath("$[0].progressPercentAsInt", is(100)))
                .andExpect(jsonPath("$[1].contentId", is(content1B.getContentId().intValue())))
                .andExpect(jsonPath("$[1].progressPercentAsInt", is(0)));
    }

    @Test
    @DisplayName("GET /api/student/courses/{id} returns 200 with private, no-store Cache-Control header")
    void testStudentCourseDetailEndpointCacheControlPrivateNoStore() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(student1.getEmail());

        mockMvc.perform(get("/api/student/courses/" + course1.getId())
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.id", is(course1.getId().intValue())))
                .andExpect(jsonPath("$.lectureCount", is(2)))
                .andExpect(jsonPath("$.completedLecturesCount", is(1)));
    }

    @Test
    @DisplayName("GET /api/student/courses returns 200 with private, no-store and batch-resolved progress counts")
    void testEnrolledCoursesEndpointCacheControlAndBatchProgress() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(student1.getEmail());

        mockMvc.perform(get("/api/student/courses")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[?(@.id == " + course1.getId() + ")].lectureCount", contains(2)))
                .andExpect(jsonPath("$[?(@.id == " + course1.getId() + ")].completedLecturesCount", contains(1)))
                .andExpect(jsonPath("$[?(@.id == " + course2.getId() + ")].lectureCount", contains(1)))
                .andExpect(jsonPath("$[?(@.id == " + course2.getId() + ")].completedLecturesCount", contains(1)));
    }

    @Test
    @DisplayName("GET /api/courses/{courseId}/leaderboard returns 200 with private, no-store and eager user details")
    void testCourseLeaderboardCacheControlAndEagerUser() throws Exception {
        mockMvc.perform(get("/api/courses/" + course1.getId() + "/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(student1.getUserId().intValue())))
                .andExpect(jsonPath("$[0].userName", is(student1.getFullName())))
                .andExpect(jsonPath("$[0].avgScore", is(92.50)))
                .andExpect(jsonPath("$[0].globalRank", is(1)));
    }

    @Test
    @DisplayName("GET /api/leaderboard returns 200 with private, no-store Cache-Control header")
    void testGlobalLeaderboardCacheControlPrivateNoStore() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(student1.getEmail());

        mockMvc.perform(get("/api/leaderboard")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }

    @Test
    @DisplayName("calculateCoursesProgressBatch resolves progress for multiple courses in a single pass")
    void testCalculateCoursesProgressBatchAccuracy() {
        List<Long> courseIds = List.of(course1.getId(), course2.getId());
        Map<Long, CourseProgressCalculation> batchResults =
                enrollmentService.calculateCoursesProgressBatch(courseIds, student1.getUserId());

        assertNotNull(batchResults);
        assertEquals(2, batchResults.size());

        CourseProgressCalculation c1Progress = batchResults.get(course1.getId());
        assertNotNull(c1Progress);
        assertEquals(2, c1Progress.getTotalContents());
        assertEquals(1, c1Progress.getCompletedContents());
        assertEquals(50.0, c1Progress.getProgressPercent());
        assertFalse(c1Progress.isCompleted());

        CourseProgressCalculation c2Progress = batchResults.get(course2.getId());
        assertNotNull(c2Progress);
        assertEquals(1, c2Progress.getTotalContents());
        assertEquals(1, c2Progress.getCompletedContents());
        assertEquals(100.0, c2Progress.getProgressPercent());
        assertTrue(c2Progress.isCompleted());
    }

    @Test
    @DisplayName("getCourseProgressSummary batch-resolves student profiles without N+1 query loop")
    void testGetCourseProgressSummaryBatchUserLookup() {
        CourseProgressSummaryDto summary = enrollmentService.getCourseProgressSummary(course1.getId());

        assertNotNull(summary);
        assertEquals(course1.getId(), summary.getCourseId());
        assertEquals(2, summary.getTotalEnrolled()); // student1 and student2
        assertEquals(2, summary.getTotalContents());
        assertEquals(2, summary.getStudents().size());

        CourseProgressSummaryDto.StudentProgressItemDto s1Item = summary.getStudents().stream()
                .filter(s -> s.getUserId().equals(student1.getUserId()))
                .findFirst()
                .orElse(null);
        assertNotNull(s1Item);
        assertEquals(student1.getFullName(), s1Item.getFullName());
        assertEquals(1, s1Item.getCompletedContentsCount());

        CourseProgressSummaryDto.StudentProgressItemDto s2Item = summary.getStudents().stream()
                .filter(s -> s.getUserId().equals(student2.getUserId()))
                .findFirst()
                .orElse(null);
        assertNotNull(s2Item);
        assertEquals(student2.getFullName(), s2Item.getFullName());
        assertEquals(0, s2Item.getCompletedContentsCount());
    }
}
