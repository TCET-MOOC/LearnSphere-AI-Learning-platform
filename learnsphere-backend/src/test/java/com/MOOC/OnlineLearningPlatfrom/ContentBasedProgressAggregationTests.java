package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W3-05 integration tests:
 * - Content-based course completion aggregation
 * - Dashboard, course detail, student progress, and staff roster summary use the same query logic
 * - GET /api/student/courses/{id}/progress returns content-scoped progress with Cache-Control: private, no-store
 * - GET /api/progress/course/{id}/summary returns comprehensive student-level progress summary (C-F8)
 * - Access control: course staff/admin only on summary, foreign callers receive 404
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ContentBasedProgressAggregationTests extends PostgresIntegrationTest {

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

    private UserAccount student1;
    private UserAccount student2;
    private UserAccount teacher;
    private UserAccount otherTeacher;
    private UserAccount admin;
    private Course course;
    private CourseContent content1;
    private CourseContent content2;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student1 = userAccountRepository.findByEmail("student@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");

        // External student as student 2
        student2 = userAccountRepository.findByEmail("external@learnsphere.com");

        // Pending teacher as other teacher
        otherTeacher = userAccountRepository.findByEmail("pending@learnsphere.com");

        Department department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Computer Science W305");
            return departmentRepository.save(d);
        });

        course = new Course();
        course.setTitle("Distributed Storage & Consensus");
        course.setDescription("Comprehensive storage engine curriculum");
        course.setDepartment(department);
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course.setPrice(BigDecimal.ZERO);
        course = courseRepository.save(course);

        Module module = new Module();
        module.setCourse(course);
        module.setTitle("Module 1 - Replication");
        module.setPosition(1);
        module = moduleRepository.save(module);

        content1 = new CourseContent();
        content1.setModule(module);
        content1.setTitle("LSM Trees");
        content1.setContentType("VIDEO");
        content1.setFilePath("https://example.com/lsm.mp4");
        content1.setPosition(1);
        content1.setDurationSeconds(1200);
        content1 = courseContentRepository.save(content1);

        content2 = new CourseContent();
        content2.setModule(module);
        content2.setTitle("Paxos vs Raft");
        content2.setContentType("VIDEO");
        content2.setFilePath("https://example.com/raft.mp4");
        content2.setPosition(2);
        content2.setDurationSeconds(1800);
        content2 = courseContentRepository.save(content2);

        // Enroll student1
        Enrollment e1 = new Enrollment();
        e1.setCourse(course);
        e1.setUserId(student1.getUserId());
        e1.setRole("STUDENT");
        e1.setEnrolledAt(LocalDateTime.now().minusDays(3));
        enrollmentRepository.save(e1);

        // Enroll student2
        Enrollment e2 = new Enrollment();
        e2.setCourse(course);
        e2.setUserId(student2.getUserId());
        e2.setRole("STUDENT");
        e2.setEnrolledAt(LocalDateTime.now().minusDays(2));
        enrollmentRepository.save(e2);
    }

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("Progress list: GET /api/student/courses/{id}/progress returns content-based progress items")
    void shouldReturnContentBasedProgressForEnrolledStudent() throws Exception {
        // Student1 watches content1 to 95 %
        String p1 = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 95.00,
                    "secondsWatched": 1140
                }
                """, content1.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student1.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(p1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/courses/" + course.getId() + "/progress")
                        .with(as(student1.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].contentId").value(content1.getContentId()))
                .andExpect(jsonPath("$[0].progressPercent").value(95.00))
                .andExpect(jsonPath("$[0].completedAt").isNotEmpty())
                .andExpect(jsonPath("$[1].contentId").value(content2.getContentId()))
                .andExpect(jsonPath("$[1].progressPercent").value(0.00))
                .andExpect(jsonPath("$[1].completedAt").isEmpty());
    }

    @Test
    @DisplayName("Consistency: Dashboard and Course Detail report identical content-based completion")
    void shouldReportIdenticalProgressOnDashboardAndCourseDetail() throws Exception {
        // Student1 completes content1 (92 %)
        String p1 = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 92.00,
                    "secondsWatched": 1104
                }
                """, content1.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student1.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(p1))
                .andExpect(status().isOk());

        // Check Dashboard
        mockMvc.perform(get("/api/student/courses")
                        .with(as(student1.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$[?(@.id == " + course.getId() + ")].lectureCount").value(2))
                .andExpect(jsonPath("$[?(@.id == " + course.getId() + ")].completedLecturesCount").value(1))
                .andExpect(jsonPath("$[?(@.id == " + course.getId() + ")].progressPercent").value(50.0))
                .andExpect(jsonPath("$[?(@.id == " + course.getId() + ")].completed").value(false));

        // Check Course Detail
        mockMvc.perform(get("/api/student/courses/" + course.getId())
                        .with(as(student1.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.id").value(course.getId()))
                .andExpect(jsonPath("$.lectureCount").value(2))
                .andExpect(jsonPath("$.completedLecturesCount").value(1))
                .andExpect(jsonPath("$.progressPercent").value(50.0))
                .andExpect(jsonPath("$.completed").value(false));

        // Complete content2 (90 %)
        String p2 = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 90.00,
                    "secondsWatched": 1620
                }
                """, content2.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student1.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(p2))
                .andExpect(status().isOk());

        // Now both endpoints report 100% completed
        mockMvc.perform(get("/api/student/courses/" + course.getId())
                        .with(as(student1.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedLecturesCount").value(2))
                .andExpect(jsonPath("$.progressPercent").value(100.0))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    @DisplayName("Summary: GET /api/progress/course/{id}/summary returns full student progress breakdown (C-F8)")
    void shouldReturnComprehensiveCourseProgressSummaryForTeacher() throws Exception {
        // Student1 completes 1 of 2 (50 %)
        String s1p = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 95.00,
                    "secondsWatched": 1140
                }
                """, content1.getContentId());
        mockMvc.perform(post("/api/progress")
                        .with(as(student1.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(s1p))
                .andExpect(status().isOk());

        // Student2 completes 2 of 2 (100 %)
        String s2p1 = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 100.00,
                    "secondsWatched": 1200
                }
                """, content1.getContentId());
        String s2p2 = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 90.00,
                    "secondsWatched": 1620
                }
                """, content2.getContentId());
        mockMvc.perform(post("/api/progress")
                        .with(as(student2.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(s2p1))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/progress")
                        .with(as(student2.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(s2p2))
                .andExpect(status().isOk());

        // Teacher requests summary
        mockMvc.perform(get("/api/progress/course/" + course.getId() + "/summary")
                        .with(as(teacher.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.courseId").value(course.getId()))
                .andExpect(jsonPath("$.totalEnrolled").value(2))
                .andExpect(jsonPath("$.totalContents").value(2))
                .andExpect(jsonPath("$.completedCount").value(1))
                .andExpect(jsonPath("$.inProgressCount").value(1))
                .andExpect(jsonPath("$.notStartedCount").value(0))
                .andExpect(jsonPath("$.averageProgress").value(75.00))
                .andExpect(jsonPath("$.students.length()").value(2));
    }

    @Test
    @DisplayName("Summary: Admin can access course progress summary")
    void shouldAllowAdminToAccessSummary() throws Exception {
        mockMvc.perform(get("/api/progress/course/" + course.getId() + "/summary")
                        .with(as(admin.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.totalEnrolled").value(2))
                .andExpect(jsonPath("$.totalContents").value(2));
    }

    @Test
    @DisplayName("Summary: Foreign teacher or student gets 404 Not Found (IDOR defense)")
    void shouldMaskSummaryAsNotFoundForForeignTeacherOrStudent() throws Exception {
        // Enrolled student
        mockMvc.perform(get("/api/progress/course/" + course.getId() + "/summary")
                        .with(as(student1.getEmail())))
                .andExpect(status().isNotFound());

        // Other teacher who does not own the course
        mockMvc.perform(get("/api/progress/course/" + course.getId() + "/summary")
                        .with(as(otherTeacher.getEmail())))
                .andExpect(status().isNotFound());

        // Unauthenticated
        mockMvc.perform(get("/api/progress/course/" + course.getId() + "/summary"))
                .andExpect(status().isUnauthorized());
    }
}
