package com.MOOC.OnlineLearningPlatfrom.learning;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W3-10 / AUD-07 / BR-23: Tests for student course resume endpoint.
 *
 * Requirements:
 * - Next incomplete content for Continue/resume;
 * - 404 when complete or not enrolled;
 * - no client-side first-lecture fallback;
 * - Cache-Control: private, no-store.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ResumeCourseTargetTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

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
    private DepartmentRepository departmentRepository;

    private UserAccount student;
    private UserAccount unenrolledStudent;
    private UserAccount teacher;
    private Course course;
    private Module module1;
    private Module module2;
    private CourseContent content1;
    private CourseContent content2;
    private CourseContent content3;

    @BeforeEach
    void setUp() {
        Department dept = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Software Systems " + System.currentTimeMillis());
            return departmentRepository.save(d);
        });

        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        unenrolledStudent = userAccountRepository.findByEmail("external@learnsphere.com");

        course = new Course();
        course.setTitle("Distributed Systems Architecture");
        course.setDescription("Consensus, state machine replication, and partitioning");
        course.setTeacher(teacher);
        course.setDepartment(dept);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        module1 = new Module();
        module1.setCourse(course);
        module1.setTitle("Module 1: Foundations");
        module1.setPosition(1);
        module1 = moduleRepository.save(module1);

        module2 = new Module();
        module2.setCourse(course);
        module2.setTitle("Module 2: Consensus Protocols");
        module2.setPosition(2);
        module2 = moduleRepository.save(module2);

        content1 = new CourseContent();
        content1.setModule(module1);
        content1.setTitle("1.1 RPCs and Failure Models");
        content1.setContentType("VIDEO");
        content1.setPosition(1);
        content1.setDurationSeconds(900);
        content1 = courseContentRepository.save(content1);

        content2 = new CourseContent();
        content2.setModule(module1);
        content2.setTitle("1.2 Logical Clocks and Lamport Timestamps");
        content2.setContentType("VIDEO");
        content2.setPosition(2);
        content2.setDurationSeconds(1200);
        content2 = courseContentRepository.save(content2);

        content3 = new CourseContent();
        content3.setModule(module2);
        content3.setTitle("2.1 Raft Leader Election");
        content3.setContentType("VIDEO");
        content3.setPosition(1);
        content3.setDurationSeconds(1500);
        content3 = courseContentRepository.save(content3);
    }

    @Test
    @DisplayName("Resume rejects unenrolled student with 404 Not Found")
    void shouldRejectResumeForUnenrolledStudentWithNotFound() throws Exception {
        mockMvc.perform(get("/api/student/courses/" + course.getId() + "/resume")
                        .with(user(userDetailsService.loadUserByUsername(unenrolledStudent.getEmail()))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Resume returns first content when student has no progress")
    void shouldReturnFirstIncompleteContentWhenNoProgress() throws Exception {
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        mockMvc.perform(get("/api/student/courses/" + course.getId() + "/resume")
                        .with(user(userDetailsService.loadUserByUsername(student.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.courseId").value(course.getId()))
                .andExpect(jsonPath("$.contentId").value(content1.getContentId()))
                .andExpect(jsonPath("$.title").value(content1.getTitle()))
                .andExpect(jsonPath("$.position").value(1))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @DisplayName("Resume returns next incomplete content across modules")
    void shouldReturnNextIncompleteContentAcrossModules() throws Exception {
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        // Complete content 1 (100%) and content 2 (95%)
        UserProgress p1 = new UserProgress();
        p1.setUserId(student.getUserId());
        p1.setContent(content1);
        p1.setProgressPercent(new BigDecimal("100.00"));
        p1.setCompletedAt(LocalDateTime.now().minusDays(1));
        userProgressRepository.save(p1);

        UserProgress p2 = new UserProgress();
        p2.setUserId(student.getUserId());
        p2.setContent(content2);
        p2.setProgressPercent(new BigDecimal("95.00"));
        p2.setCompletedAt(LocalDateTime.now().minusHours(2));
        userProgressRepository.save(p2);

        // Resume must return content 3 in module 2
        mockMvc.perform(get("/api/student/courses/" + course.getId() + "/resume")
                        .with(user(userDetailsService.loadUserByUsername(student.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.courseId").value(course.getId()))
                .andExpect(jsonPath("$.contentId").value(content3.getContentId()))
                .andExpect(jsonPath("$.moduleId").value(module2.getModuleId()))
                .andExpect(jsonPath("$.title").value(content3.getTitle()))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @DisplayName("Resume returns partially completed content with progress details")
    void shouldReturnPartiallyCompletedContentWithDetails() throws Exception {
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        // Complete content 1
        UserProgress p1 = new UserProgress();
        p1.setUserId(student.getUserId());
        p1.setContent(content1);
        p1.setProgressPercent(new BigDecimal("100.00"));
        p1.setCompletedAt(LocalDateTime.now().minusDays(1));
        userProgressRepository.save(p1);

        // Partially complete content 2 (60%, 720 seconds watched, incomplete)
        UserProgress p2 = new UserProgress();
        p2.setUserId(student.getUserId());
        p2.setContent(content2);
        p2.setProgressPercent(new BigDecimal("60.00"));
        p2.setSecondsWatched(720);
        userProgressRepository.save(p2);

        mockMvc.perform(get("/api/student/courses/" + course.getId() + "/resume")
                        .with(user(userDetailsService.loadUserByUsername(student.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.contentId").value(content2.getContentId()))
                .andExpect(jsonPath("$.progressPercent").value(60.0))
                .andExpect(jsonPath("$.secondsWatched").value(720))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    @DisplayName("Resume returns 404 when all contents in course are completed")
    void shouldReturn404WhenAllContentsCompleted() throws Exception {
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        // Complete all 3 contents
        UserProgress p1 = new UserProgress();
        p1.setUserId(student.getUserId());
        p1.setContent(content1);
        p1.setProgressPercent(new BigDecimal("90.00"));
        p1.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p1);

        UserProgress p2 = new UserProgress();
        p2.setUserId(student.getUserId());
        p2.setContent(content2);
        p2.setProgressPercent(new BigDecimal("100.00"));
        p2.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p2);

        UserProgress p3 = new UserProgress();
        p3.setUserId(student.getUserId());
        p3.setContent(content3);
        p3.setProgressPercent(new BigDecimal("95.00"));
        p3.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p3);

        mockMvc.perform(get("/api/student/courses/" + course.getId() + "/resume")
                        .with(user(userDetailsService.loadUserByUsername(student.getEmail()))))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }
}
