package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.AuditService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for W4-09:
 * - Attempt review: answers + marks, correct answers only after finalize (COMPLETED or ABANDONED)
 * - GET /api/assessments/attempts/{id} and GET /api/attempts/{id}/answers
 * - Manual grade audit log recording (AUD-15, AC-15, AC-16, [D-F12])
 * - Stealth 404 on foreign attempt IDOR
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AttemptReviewAndGradeAuditTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private UserAccount teacherUser;
    private UserAccount studentUser;
    private UserAccount foreignStudentUser;
    private Course course;
    private com.MOOC.OnlineLearningPlatfrom.Entity.Test test;
    private Question mcqQuestion;
    private Question textQuestion;

    @BeforeEach
    void setUp() {
        teacherUser = userAccountRepository.findByEmail("teacher@learnsphere.com");
        studentUser = userAccountRepository.findByEmail("student@learnsphere.com");
        foreignStudentUser = userAccountRepository.findByEmail("external@learnsphere.com");

        course = new Course();
        course.setTitle("Distributed Systems Architecture");
        course.setDescription("Advanced cloud architecture and distributed computing.");
        course.setTeacher(teacherUser);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        // Enroll student
        Enrollment studentEnrollment = new Enrollment();
        studentEnrollment.setUserId(studentUser.getUserId());
        studentEnrollment.setCourse(course);
        studentEnrollment.setRole("STUDENT");
        studentEnrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(studentEnrollment);

        // Test
        test = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test.setTitle("Midterm Examination");
        test.setCourse(course);
        test.setDurationMinutes(60);
        test.setMaxAttempts(3);
        test = testRepository.save(test);

        // MCQ: 5 marks, options ["A", "B", "C", "D"], correctAnswer "B"
        mcqQuestion = new Question();
        mcqQuestion.setTest(test);
        mcqQuestion.setBody("What does CAP stand for?");
        mcqQuestion.setQuestionType("MCQ");
        mcqQuestion.setMarks(new BigDecimal("5.00"));
        mcqQuestion.setOptions(List.of("A", "B", "C", "D"));
        mcqQuestion.setCorrectAnswer("B");
        mcqQuestion = questionRepository.save(mcqQuestion);

        // TEXT: 10 marks
        textQuestion = new Question();
        textQuestion.setTest(test);
        textQuestion.setBody("Explain eventual consistency in DynamoDB.");
        textQuestion.setQuestionType("TEXT");
        textQuestion.setMarks(new BigDecimal("10.00"));
        textQuestion = questionRepository.save(textQuestion);

        entityManager.flush();
    }

    private RequestPostProcessor studentAuth() {
        return user(userDetailsService.loadUserByUsername(studentUser.getEmail()));
    }

    private RequestPostProcessor teacherAuth() {
        return user(userDetailsService.loadUserByUsername(teacherUser.getEmail()));
    }

    private RequestPostProcessor foreignStudentAuth() {
        return user(userDetailsService.loadUserByUsername(foreignStudentUser.getEmail()));
    }

    @Test
    @DisplayName("In-progress attempt review hides correctAnswer, score, and marksAwarded for students via /api/assessments/attempts/{id}")
    void shouldHideCorrectAnswerAndMarksWhileInProgress() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
        attempt = testAttemptRepository.save(attempt);

        StudentAnswer answer = new StudentAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(mcqQuestion);
        answer.setAnswerText("B");
        studentAnswerRepository.save(answer);

        entityManager.flush();

        mockMvc.perform(get("/api/assessments/attempts/{attemptId}", attempt.getAttemptId())
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.score").doesNotExist()) // hidden while in progress
                .andExpect(jsonPath("$.answers", hasSize(1)))
                .andExpect(jsonPath("$.answers[0].questionId").value(mcqQuestion.getQuestionId()))
                .andExpect(jsonPath("$.answers[0].studentAnswer").value("B"))
                .andExpect(jsonPath("$.answers[0].correctAnswer").doesNotExist()) // strictly hidden!
                .andExpect(jsonPath("$.answers[0].marksAwarded").doesNotExist()); // strictly hidden!
    }

    @Test
    @DisplayName("In-progress answers endpoint masks marksAwarded for students via /api/attempts/{id}/answers")
    void shouldMaskMarksAwardedForStudentOnInProgressAnswersEndpoint() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
        attempt = testAttemptRepository.save(attempt);

        StudentAnswer answer = new StudentAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(mcqQuestion);
        answer.setAnswerText("B");
        studentAnswerRepository.save(answer);

        entityManager.flush();

        mockMvc.perform(get("/api/attempts/{attemptId}/answers", attempt.getAttemptId())
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].studentAnswerId").value(answer.getStudentAnswerId()))
                .andExpect(jsonPath("$[0].marksAwarded").doesNotExist()); // must be null/omitted
    }

    @Test
    @DisplayName("Completed attempt review reveals score, correctAnswer, and marksAwarded")
    void shouldRevealScoreAndCorrectAnswerOnceCompleted() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("COMPLETED");
        attempt.setScore(new BigDecimal("5.00"));
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(30));
        attempt.setCompletedAt(LocalDateTime.now().minusMinutes(5));
        attempt = testAttemptRepository.save(attempt);

        StudentAnswer answer = new StudentAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(mcqQuestion);
        answer.setAnswerText("B");
        answer.setMarksAwarded(new BigDecimal("5.00"));
        studentAnswerRepository.save(answer);

        entityManager.flush();

        mockMvc.perform(get("/api/assessments/attempts/{attemptId}", attempt.getAttemptId())
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(5.00))
                .andExpect(jsonPath("$.answers", hasSize(1)))
                .andExpect(jsonPath("$.answers[0].correctAnswer").value("B"))
                .andExpect(jsonPath("$.answers[0].marksAwarded").value(5.00));
    }

    @Test
    @DisplayName("Abandoned attempt review reveals score and correctAnswer")
    void shouldRevealScoreAndCorrectAnswerOnceAbandoned() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("ABANDONED");
        attempt.setScore(new BigDecimal("5.00"));
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(70));
        attempt.setCompletedAt(LocalDateTime.now().minusMinutes(10));
        attempt = testAttemptRepository.save(attempt);

        StudentAnswer answer = new StudentAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(mcqQuestion);
        answer.setAnswerText("B");
        answer.setMarksAwarded(new BigDecimal("5.00"));
        studentAnswerRepository.save(answer);

        entityManager.flush();

        mockMvc.perform(get("/api/assessments/attempts/{attemptId}", attempt.getAttemptId())
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.status").value("ABANDONED"))
                .andExpect(jsonPath("$.score").value(5.00))
                .andExpect(jsonPath("$.answers", hasSize(1)))
                .andExpect(jsonPath("$.answers[0].correctAnswer").value("B"))
                .andExpect(jsonPath("$.answers[0].marksAwarded").value(5.00));
    }

    @Test
    @DisplayName("Course teacher can review student attempt via GET /api/assessments/attempts/{id}")
    void shouldAllowCourseTeacherToReviewStudentAttempt() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("COMPLETED");
        attempt.setScore(new BigDecimal("5.00"));
        attempt = testAttemptRepository.save(attempt);

        StudentAnswer answer = new StudentAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(mcqQuestion);
        answer.setAnswerText("B");
        answer.setMarksAwarded(new BigDecimal("5.00"));
        studentAnswerRepository.save(answer);

        entityManager.flush();

        mockMvc.perform(get("/api/assessments/attempts/{attemptId}", attempt.getAttemptId())
                        .with(teacherAuth()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.attemptId").value(attempt.getAttemptId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(5.00));
    }

    @Test
    @DisplayName("Foreign student receives stealth 404 on attempt review or answers endpoint")
    void shouldRejectForeignStudentWithStealth404() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("COMPLETED");
        attempt = testAttemptRepository.save(attempt);

        entityManager.flush();

        // Foreign student on /api/assessments/attempts/{id}
        mockMvc.perform(get("/api/assessments/attempts/{attemptId}", attempt.getAttemptId())
                        .with(foreignStudentAuth()))
                .andExpect(status().isNotFound());

        // Foreign student on /api/attempts/{id}/answers
        mockMvc.perform(get("/api/attempts/{attemptId}/answers", attempt.getAttemptId())
                        .with(foreignStudentAuth()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Manual grade awards record audit log entry in audit_log table with before and after state")
    void shouldRecordAuditLogOnManualGradeAward() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("COMPLETED");
        attempt.setScore(new BigDecimal("5.00")); // MCQ was 5
        attempt = testAttemptRepository.save(attempt);

        StudentAnswer mcqAnswer = new StudentAnswer();
        mcqAnswer.setAttempt(attempt);
        mcqAnswer.setQuestion(mcqQuestion);
        mcqAnswer.setAnswerText("B");
        mcqAnswer.setMarksAwarded(new BigDecimal("5.00"));
        studentAnswerRepository.save(mcqAnswer);

        StudentAnswer textAnswer = new StudentAnswer();
        textAnswer.setAttempt(attempt);
        textAnswer.setQuestion(textQuestion);
        textAnswer.setAnswerText("Detailed explanation of eventual consistency and conflict resolution.");
        textAnswer.setMarksAwarded(null);
        textAnswer = studentAnswerRepository.save(textAnswer);

        entityManager.flush();

        String gradePayload = """
                {
                    "marksAwarded": 8.50,
                    "feedback": "Outstanding architectural analysis"
                }
                """;

        mockMvc.perform(patch("/api/answers/{id}/grade", textAnswer.getStudentAnswerId())
                        .with(teacherAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradePayload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.marksAwarded").value(8.50));

        // Verify audit log entry
        List<AuditLog> auditLogs = auditLogRepository.findByResourceTypeAndResourceIdOrderByIdAsc(
                AuditService.RESOURCE_STUDENT_ANSWER, textAnswer.getStudentAnswerId());

        assertThat(auditLogs).isNotEmpty();
        AuditLog audit = auditLogs.get(auditLogs.size() - 1);
        assertThat(audit.getAction()).isEqualTo(AuditService.MANUAL_GRADE_AWARDED);
        assertThat(audit.getActorId()).isEqualTo(teacherUser.getUserId());
        assertThat(audit.getResourceType()).isEqualTo("STUDENT_ANSWER");
        assertThat(audit.getResourceId()).isEqualTo(textAnswer.getStudentAnswerId());
        assertThat(audit.getAfterJson()).isNotNull();
        assertThat(new BigDecimal(audit.getAfterJson().get("marksAwarded").toString()))
                .isEqualByComparingTo(new BigDecimal("8.50"));
        assertThat(new BigDecimal(audit.getAfterJson().get("attemptScore").toString()))
                .isEqualByComparingTo(new BigDecimal("13.50"));
        assertThat(audit.getAfterJson().get("feedback")).isEqualTo("Outstanding architectural analysis");
    }

    @Test
    @DisplayName("Student cannot manually grade and cannot tamper with audit")
    void shouldRejectStudentManualGradingWithStealth404() throws Exception {
        StudentAnswer textAnswer = new StudentAnswer();
        textAnswer.setQuestion(textQuestion);
        textAnswer.setAnswerText("Student answer.");
        textAnswer = studentAnswerRepository.save(textAnswer);

        entityManager.flush();

        int initialAuditCount = auditLogRepository.findAll().size();

        mockMvc.perform(patch("/api/answers/{id}/grade", textAnswer.getStudentAnswerId())
                        .with(studentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marksAwarded\": 10}"))
                .andExpect(status().isNotFound());

        int finalAuditCount = auditLogRepository.findAll().size();
        assertThat(finalAuditCount).isEqualTo(initialAuditCount);
    }
}
