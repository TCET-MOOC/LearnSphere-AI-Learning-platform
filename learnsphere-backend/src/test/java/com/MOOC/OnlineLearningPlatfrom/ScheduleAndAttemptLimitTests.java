package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W4-07 Integration tests: Schedule, duration, attempt limit enforcement on start; check enrolment.
 * References: AC-18, [D-F6, D-F7, D-F10].
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ScheduleAndAttemptLimitTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private EntityManager entityManager;

    private UserAccount studentUser;
    private UserAccount foreignUser;
    private Course course;
    private com.MOOC.OnlineLearningPlatfrom.Entity.Test test;

    @BeforeEach
    void setUp() {
        studentUser = userAccountRepository.findByEmail("student@learnsphere.com");
        foreignUser = userAccountRepository.findByEmail("external@learnsphere.com");
        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");

        course = new Course();
        course.setTitle("Schedule & Limit Test Course " + System.currentTimeMillis());
        course.setDescription("Testing schedule & limit enforcement");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(studentUser.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        test = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test.setCourse(course);
        test.setTitle("Strict Assessment");
        test.setDurationMinutes(15);
        test.setSecurityPolicy("STANDARD");
        test = testRepository.save(test);

        entityManager.flush();
    }

    private RequestPostProcessor studentAuth() {
        return user(userDetailsService.loadUserByUsername(studentUser.getEmail()));
    }

    private RequestPostProcessor foreignAuth() {
        return user(userDetailsService.loadUserByUsername(foreignUser.getEmail()));
    }

    @Test
    @DisplayName("Should reject attempt start if test is scheduled in the future")
    void shouldRejectAttemptStartIfTestIsScheduledInFuture() throws Exception {
        test.setScheduledAt(LocalDateTime.now().plusHours(3));
        test = testRepository.save(test);
        entityManager.flush();

        // Student canonical start
        mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(studentAuth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("scheduled for a future time")));

        // The legacy start route is retired (W4-02 audit fix): no second way round the schedule.
        assertThat(mockMvc.perform(post("/api/tests/" + test.getTestId() + "/attempts")
                        .with(studentAuth())).andReturn().getResponse().getStatus()).isIn(404, 405);
    }

    @Test
    @DisplayName("Should allow attempt start if scheduledAt is in the past or null")
    void shouldAllowAttemptStartIfScheduledAtIsInPastOrNull() throws Exception {
        test.setScheduledAt(LocalDateTime.now().minusHours(1));
        test = testRepository.save(test);
        entityManager.flush();

        mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("Should reject attempt start if max attempts limit is reached")
    void shouldRejectAttemptStartIfMaxAttemptsReached() throws Exception {
        test.setMaxAttempts(2);
        test = testRepository.save(test);

        // Record 2 completed attempts
        for (int i = 0; i < 2; i++) {
            TestAttempt attempt = new TestAttempt();
            attempt.setTest(test);
            attempt.setUserId(studentUser.getUserId());
            attempt.setStatus("COMPLETED");
            attempt.setStartedAt(LocalDateTime.now().minusDays(1));
            attempt.setCompletedAt(LocalDateTime.now().minusDays(1).plusMinutes(10));
            testAttemptRepository.save(attempt);
        }
        entityManager.flush();

        // Attempting a 3rd start should be rejected with 400 Bad Request
        mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(studentAuth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("Maximum attempt limit")));

        // The legacy start route is retired (W4-02 audit fix): no second way round the limit.
        assertThat(mockMvc.perform(post("/api/tests/" + test.getTestId() + "/attempts")
                        .with(studentAuth())).andReturn().getResponse().getStatus()).isIn(404, 405);
    }

    @Test
    @DisplayName("Should resume existing active in-progress attempt if within duration without creating duplicate")
    void shouldResumeExistingInProgressAttemptIfWithinDuration() throws Exception {
        TestAttempt existing = new TestAttempt();
        existing.setTest(test);
        existing.setUserId(studentUser.getUserId());
        existing.setStatus("IN_PROGRESS");
        existing.setStartedAt(LocalDateTime.now().minusMinutes(3)); // Duration is 15 min
        existing = testAttemptRepository.save(existing);
        entityManager.flush();

        mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptId", is(existing.getAttemptId().intValue())))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")));

        List<TestAttempt> allAttempts = testAttemptRepository.findByUserIdAndTest_TestId(studentUser.getUserId(), test.getTestId());
        assertThat(allAttempts).hasSize(1);
    }

    @Test
    @DisplayName("Should auto-abandon expired active attempt and allow new attempt if under limit")
    void shouldAutoAbandonExpiredAttemptAndAllowNewAttemptIfUnderLimit() throws Exception {
        test.setMaxAttempts(2);
        test = testRepository.save(test);

        // Previous attempt was started 30 mins ago on 15 min test
        TestAttempt expired = new TestAttempt();
        expired.setTest(test);
        expired.setUserId(studentUser.getUserId());
        expired.setStatus("IN_PROGRESS");
        expired.setStartedAt(LocalDateTime.now().minusMinutes(30));
        expired = testAttemptRepository.save(expired);
        entityManager.flush();

        // Start should auto-abandon the expired attempt and start attempt #2 of 2
        mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.attemptId").value(org.hamcrest.Matchers.not(expired.getAttemptId().intValue())));

        TestAttempt refreshedOld = testAttemptRepository.findById(expired.getAttemptId()).orElseThrow();
        assertThat(refreshedOld.getStatus()).isEqualTo("ABANDONED");
        assertThat(refreshedOld.getCompletedAt()).isNotNull();

        List<TestAttempt> allAttempts = testAttemptRepository.findByUserIdAndTest_TestId(studentUser.getUserId(), test.getTestId());
        assertThat(allAttempts).hasSize(2);
    }

    @Test
    @DisplayName("Should reject foreign unenrolled student with stealth 404")
    void shouldRejectForeignUnenrolledStudentWithStealth404() throws Exception {
        // foreignUser is not enrolled in course
        mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(foreignAuth()))
                .andExpect(status().isNotFound());

        // The legacy start route is retired (W4-02 audit fix).
        assertThat(mockMvc.perform(post("/api/tests/" + test.getTestId() + "/attempts")
                        .with(foreignAuth())).andReturn().getResponse().getStatus()).isIn(404, 405);
    }

    @Test
    @DisplayName("Should return Cache-Control private, no-store on start attempt")
    void shouldReturnPrivateNoStoreCacheHeaderOnStart() throws Exception {
        mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"));
    }
}

