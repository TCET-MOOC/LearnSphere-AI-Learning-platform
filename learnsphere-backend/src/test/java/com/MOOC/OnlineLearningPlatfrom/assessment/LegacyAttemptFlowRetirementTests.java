package com.MOOC.OnlineLearningPlatfrom.assessment;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Post-W6 audit fix (W4-02 regression): the legacy student attempt flow on /api/tests and
 * /api/attempts duplicated /api/assessments/** and stayed live after W4-02. It is retired:
 * an enrolled student calling it gets 404/405 and nothing is started, answered or graded.
 * Staff grading reads (GET /api/attempts/{id}/answers, PATCH /api/answers/{id}/grade) remain.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LegacyAttemptFlowRetirementTests extends PostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private CourseRepository courseRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private TestAttemptRepository testAttemptRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private TestService testService;

    private UserAccount student;
    private com.MOOC.OnlineLearningPlatfrom.Entity.Test quiz;
    private Question question;
    private TestAttempt attempt;

    @BeforeEach
    void setUp() {
        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");

        Course course = new Course();
        course.setTitle("Legacy Flow Course");
        course.setDescription("Legacy attempt endpoints are retired");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        Enrollment e = new Enrollment();
        e.setUserId(student.getUserId());
        e.setCourse(course);
        e.setRole("STUDENT");
        e.setEnrolledAt(LocalDateTime.now().minusDays(1));
        enrollmentRepository.save(e);

        quiz = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        quiz.setCourse(course);
        quiz.setTitle("Legacy flow quiz");
        quiz.setDurationMinutes(30);
        quiz = testRepository.save(quiz);

        question = new Question();
        question.setTest(quiz);
        question.setBody("2 + 2 = ?");
        question.setQuestionType("MCQ");
        question.setOptions(List.of("3", "4"));
        question.setCorrectAnswer("4");
        question.setMarks(new BigDecimal("1.00"));
        question = questionRepository.save(question);

        attempt = testService.startTestAttempt(quiz.getTestId(), student.getUserId());
        entityManager.flush();
    }

    private RequestPostProcessor me() {
        return user(userDetailsService.loadUserByUsername(student.getEmail()));
    }

    private void assertRetired(String label, MockHttpServletRequestBuilder request) throws Exception {
        int status = mockMvc.perform(request.with(me())).andReturn().getResponse().getStatus();
        assertThat(status).as(label).isIn(404, 405);
    }

    @Test
    @DisplayName("Starting an attempt through /api/tests is retired and starts nothing")
    void legacyStartRetired() throws Exception {
        long before = testAttemptRepository.count();
        assertRetired("POST /api/tests/{id}/attempts", post("/api/tests/{id}/attempts", quiz.getTestId()));
        assertThat(testAttemptRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("Answering, submitting and finalizing through /api/attempts are retired")
    void legacyAnswerSubmitFinalizeRetired() throws Exception {
        assertRetired("POST /api/attempts/{id}/answers", post("/api/attempts/{id}/answers", attempt.getAttemptId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"question\":{\"questionId\":" + question.getQuestionId() + "},\"answerText\":\"4\"}"));
        assertRetired("POST /api/attempts/{id}/submit", post("/api/attempts/{id}/submit", attempt.getAttemptId()));
        assertRetired("POST /api/attempts/{id}/finalize", post("/api/attempts/{id}/finalize", attempt.getAttemptId()));
        entityManager.clear();
        assertThat(testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow().getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("Listing my attempts through /api/tests is retired")
    void legacyMyAttemptsRetired() throws Exception {
        assertRetired("GET /api/tests/{id}/attempts/me", get("/api/tests/{id}/attempts/me", quiz.getTestId()));
    }
}
