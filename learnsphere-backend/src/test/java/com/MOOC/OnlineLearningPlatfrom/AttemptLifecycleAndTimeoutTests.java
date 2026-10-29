package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.Service.AttemptTimeoutJob;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W4-06 Integration tests: Attempt lifecycle (startedAt, completedAt, ABANDONED, timeout job, server clock).
 * References: AC-18, [D-F5], M-11.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AttemptLifecycleAndTimeoutTests extends PostgresIntegrationTest {

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
    private QuestionRepository questionRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @Autowired
    private AttemptTimeoutJob attemptTimeoutJob;

    @Autowired
    private EntityManager entityManager;

    private UserAccount studentUser;
    private Course course;
    private com.MOOC.OnlineLearningPlatfrom.Entity.Test test;
    private Question mcqQuestion;

    @BeforeEach
    void setUp() {
        studentUser = userAccountRepository.findByEmail("student@learnsphere.com");

        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");

        course = new Course();
        course.setTitle("Lifecycle Test Course " + System.currentTimeMillis());
        course.setDescription("Testing attempt lifecycle");
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
        test.setTitle("Timed Assessment");
        test.setDurationMinutes(15);
        test.setSecurityPolicy("STANDARD");
        test = testRepository.save(test);

        mcqQuestion = new Question();
        mcqQuestion.setTest(test);
        mcqQuestion.setQuestionType("MCQ");
        mcqQuestion.setBody("What is 2+2?");
        mcqQuestion.setOptions(List.of("3", "4", "5"));
        mcqQuestion.setCorrectAnswer("4");
        mcqQuestion.setMarks(10);
        mcqQuestion = questionRepository.save(mcqQuestion);

        entityManager.flush();
    }

    private RequestPostProcessor studentAuth() {
        return user(userDetailsService.loadUserByUsername(studentUser.getEmail()));
    }

    @Test
    @DisplayName("Should stamp startedAt, status IN_PROGRESS, and null completedAt upon attempt start")
    void shouldStampStartedAtAndInProgressOnAttemptStart() throws Exception {
        LocalDateTime beforeStart = LocalDateTime.now().minusSeconds(2);

        String response = mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.startedAt", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("\"completedAt\"");

        List<TestAttempt> attempts = testAttemptRepository.findByUserIdAndTest_TestId(studentUser.getUserId(), test.getTestId());
        assertThat(attempts).isNotEmpty();
        TestAttempt latest = attempts.get(attempts.size() - 1);
        assertThat(latest.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(latest.getStartedAt()).isNotNull();
        assertThat(latest.getStartedAt()).isAfterOrEqualTo(beforeStart);
        assertThat(latest.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("Should stamp completedAt and transition status to COMPLETED upon on-time finalization")
    void shouldStampCompletedAtOnFinalize() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
        attempt.setStatus("IN_PROGRESS");
        attempt = testAttemptRepository.save(attempt);

        StudentAnswer answer = new StudentAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(mcqQuestion);
        answer.setAnswerText("4");
        studentAnswerRepository.save(answer);

        entityManager.flush();

        LocalDateTime beforeFinalize = LocalDateTime.now().minusSeconds(2);

        mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/finalize")
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.completedAt", notNullValue()))
                .andExpect(jsonPath("$.score", is(10)));

        TestAttempt refreshed = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("COMPLETED");
        assertThat(refreshed.getCompletedAt()).isNotNull();
        assertThat(refreshed.getCompletedAt()).isAfterOrEqualTo(beforeFinalize);
    }

    @Test
    @DisplayName("Should reject answer submission and auto-abandon when past duration deadline")
    void shouldRejectAnswerSubmissionAfterDeadlineAndAutoAbandon() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        // Test duration is 15 minutes, set start to 20 minutes ago
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(20));
        attempt.setStatus("IN_PROGRESS");
        attempt = testAttemptRepository.save(attempt);
        entityManager.flush();

        mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/answers")
                        .with(studentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + mcqQuestion.getQuestionId() + ",\"answerText\":\"4\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Attempt has expired / timed out"));

        TestAttempt refreshed = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("ABANDONED");
        assertThat(refreshed.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject finalization and auto-abandon when past duration deadline")
    void shouldRejectFinalizeAndAutoAbandonWhenPastDurationDeadline() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(25));
        attempt.setStatus("IN_PROGRESS");
        attempt = testAttemptRepository.save(attempt);

        StudentAnswer answer = new StudentAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(mcqQuestion);
        answer.setAnswerText("4");
        studentAnswerRepository.save(answer);
        entityManager.flush();

        mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/finalize")
                        .with(studentAuth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Attempt has expired / timed out"));

        TestAttempt refreshed = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("ABANDONED");
        assertThat(refreshed.getCompletedAt()).isNotNull();
        assertThat(refreshed.getScore()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Should auto-abandon and evaluate existing answers via AttemptTimeoutJob")
    void shouldExpireTimedOutAttemptsViaTimeoutJob() {
        TestAttempt expiredAttempt = new TestAttempt();
        expiredAttempt.setTest(test);
        expiredAttempt.setUserId(studentUser.getUserId());
        expiredAttempt.setStartedAt(LocalDateTime.now().minusMinutes(30));
        expiredAttempt.setStatus("IN_PROGRESS");
        expiredAttempt = testAttemptRepository.save(expiredAttempt);

        StudentAnswer answer = new StudentAnswer();
        answer.setAttempt(expiredAttempt);
        answer.setQuestion(mcqQuestion);
        answer.setAnswerText("4");
        studentAnswerRepository.save(answer);

        entityManager.flush();

        int expiredCount = attemptTimeoutJob.checkAndExpireTimedOutAttempts();
        assertThat(expiredCount).isGreaterThanOrEqualTo(1);

        TestAttempt refreshed = testAttemptRepository.findById(expiredAttempt.getAttemptId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("ABANDONED");
        assertThat(refreshed.getCompletedAt()).isNotNull();
        assertThat(refreshed.getScore()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Should not expire in-progress attempts within duration window via AttemptTimeoutJob")
    void shouldNotExpireAttemptsWithinDurationWindow() {
        TestAttempt activeAttempt = new TestAttempt();
        activeAttempt.setTest(test);
        activeAttempt.setUserId(studentUser.getUserId());
        activeAttempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
        activeAttempt.setStatus("IN_PROGRESS");
        activeAttempt = testAttemptRepository.save(activeAttempt);
        entityManager.flush();

        attemptTimeoutJob.checkAndExpireTimedOutAttempts();

        TestAttempt refreshed = testAttemptRepository.findById(activeAttempt.getAttemptId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(refreshed.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("Database check constraint chk_test_attempts_status should reject invalid status values")
    void shouldEnforceStatusConstraintInDatabase() {
        assertThatThrownBy(() -> {
            entityManager.createNativeQuery(
                    "INSERT INTO test_attempts (user_id, test_id, started_at, status) VALUES (?, ?, NOW(), ?)")
                    .setParameter(1, studentUser.getUserId())
                    .setParameter(2, test.getTestId())
                    .setParameter(3, "INVALID_STATUS")
                    .executeUpdate();
            entityManager.flush();
        }).hasMessageFindingMatch("chk_test_attempts_status|check constraint");
    }

    @Test
    @DisplayName("Tests table should have created_at populated on persist")
    void shouldVerifyTestsCreatedAtTimestamp() {
        assertThat(test.getCreatedAt()).isNotNull();
        assertThat(test.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}

