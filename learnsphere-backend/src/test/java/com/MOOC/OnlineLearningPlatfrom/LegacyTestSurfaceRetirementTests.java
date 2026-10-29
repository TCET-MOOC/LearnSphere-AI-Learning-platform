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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W4-02 Integration tests: Retire legacy /api/tests write surface & canonical path enforcement ([D-F2, D-F12]).
 *
 * Contracts verified:
 * 1. Generic unscoped /api/tests collection (GET /api/tests) is retired from regular roles:
 *    students and teachers receive 403 Forbidden, admin has inspect access.
 * 2. Generic unscoped test creation (POST /api/tests) is retired from regular roles:
 *    non-admins receive 403 Forbidden, admin has deprecated access.
 * 3. Canonical teacher authoring is under /api/teacher/tests/**:
 *    - Teacher updates test via PUT /api/teacher/tests/{testId}
 *    - Teacher updates question via PUT /api/teacher/tests/{testId}/questions/{questionId}
 *    - Teacher deletes question via DELETE /api/teacher/tests/{testId}/questions/{questionId}
 *    - Foreign teacher attempts to mutate receive 404 Not Found (stealth IDOR)
 * 4. Canonical student assessment path is strictly under /api/assessments/**:
 *    - Detail (GET /api/assessments/{id})
 *    - Start (POST /api/assessments/{id}/start)
 *    - Answer (POST /api/assessments/attempts/{id}/answers)
 *    - Finalize (POST /api/assessments/attempts/{id}/finalize)
 *    - Review (GET /api/assessments/attempts/{id})
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LegacyTestSurfaceRetirementTests extends PostgresIntegrationTest {

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
    private EntityManager entityManager;

    private Long teacherId;
    private Long foreignTeacherId;
    private Long studentId;
    private Long courseId;
    private Long testId;
    private Long questionId;

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @BeforeEach
    void setUp() {
        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        teacherId = teacher.getUserId();

        UserAccount foreignTeacher = userAccountRepository.findByEmail("pending@learnsphere.com");
        foreignTeacherId = foreignTeacher.getUserId();

        UserAccount student = userAccountRepository.findByEmail("student@learnsphere.com");
        studentId = student.getUserId();

        // Create LIVE course owned by teacher
        Course course = new Course();
        course.setTitle("Distributed Systems Architecture");
        course.setDescription("Advanced assessment course");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);
        courseId = course.getId();

        // Enroll student
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUserId(studentId);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        // Create test
        com.MOOC.OnlineLearningPlatfrom.Entity.Test test = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test.setCourse(course);
        test.setTitle("Consensus Algorithms Quiz");
        test.setDurationMinutes(45);
        test.setIsRemedial(false);
        test = testRepository.save(test);
        testId = test.getTestId();

        // Create question
        Question question = new Question();
        question.setTest(test);
        question.setBody("Which consensus algorithm uses leader leases?");
        question.setQuestionType("MCQ");
        question.setMarks(10);
        question.setOptions(List.of("Raft", "Paxos", "ZAB", "Two-Phase Commit"));
        question.setCorrectAnswer("Raft");
        question = questionRepository.save(question);
        questionId = question.getQuestionId();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("W4-02: Generic GET /api/tests is retired for non-admins (403 Forbidden)")
    void shouldBlockGenericTestListingForNonAdmins() throws Exception {
        // Student receives 403
        mockMvc.perform(get("/api/tests")
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isForbidden());

        // Teacher receives 403
        mockMvc.perform(get("/api/tests")
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isForbidden());

        // Admin retains access
        mockMvc.perform(get("/api/tests")
                        .with(as("admin@learnsphere.com")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("W4-02: Generic POST /api/tests is retired for non-admins (403 Forbidden)")
    void shouldBlockGenericTestCreationForNonAdmins() throws Exception {
        String payload = "{\"title\":\"Rogue Test\",\"durationMinutes\":30}";

        // Student receives 403
        mockMvc.perform(post("/api/tests")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        // Teacher receives 403 (must use /api/teacher/tests)
        mockMvc.perform(post("/api/tests")
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("W4-02: Canonical teacher test and question update/delete under /api/teacher/tests/**")
    void shouldSupportCanonicalTeacherTestAndQuestionMutations() throws Exception {
        // Teacher updates test
        mockMvc.perform(put("/api/teacher/tests/{testId}", testId)
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Raft Quiz\",\"durationMinutes\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Raft Quiz"))
                .andExpect(jsonPath("$.durationMinutes").value(50));

        // Foreign teacher cannot update test (404 Not Found)
        mockMvc.perform(put("/api/teacher/tests/{testId}", testId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked Title\"}"))
                .andExpect(status().isNotFound());

        // Teacher updates question
        mockMvc.perform(put("/api/teacher/tests/{testId}/questions/{questionId}", testId, questionId)
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Updated question body?\",\"marks\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Updated question body?"))
                .andExpect(jsonPath("$.marks").value(15));

        // Foreign teacher cannot update question (404 Not Found)
        mockMvc.perform(put("/api/teacher/tests/{testId}/questions/{questionId}", testId, questionId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Hacked body\"}"))
                .andExpect(status().isNotFound());

        // Teacher deletes question
        mockMvc.perform(delete("/api/teacher/tests/{testId}/questions/{questionId}", testId, questionId)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isNoContent());

        assertThat(questionRepository.findById(questionId)).isEmpty();
    }

    @Test
    @DisplayName("W4-02: Canonical student assessment flow is fully accessible via /api/assessments/**")
    void shouldSupportCanonicalStudentAssessmentFlow() throws Exception {
        // 1. Get assessment detail
        mockMvc.perform(get("/api/assessments/{id}", testId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testId))
                .andExpect(jsonPath("$.questions", hasSize(1)));

        // 2. Start attempt
        String startJson = mockMvc.perform(post("/api/assessments/{id}/start", testId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn().getResponse().getContentAsString();

        Long attemptId = com.jayway.jsonpath.JsonPath.parse(startJson).read("$.attemptId", Long.class);

        // 3. Submit answer
        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/answers", attemptId)
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + questionId + ",\"answerText\":\"Raft\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));

        // 4. Finalize attempt
        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/finalize", attemptId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(10));

        // 5. Review attempt
        mockMvc.perform(get("/api/assessments/attempts/{attemptId}", attemptId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(10))
                .andExpect(jsonPath("$.answers[0].correctAnswer").value("Raft"));
    }
}
