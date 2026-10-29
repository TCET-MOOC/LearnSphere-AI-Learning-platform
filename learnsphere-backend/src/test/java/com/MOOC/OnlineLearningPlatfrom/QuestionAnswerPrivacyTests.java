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
 * W4-01 Integration tests: Assessment integrity & Question answer privacy (AC-15, [D-F1], NFR-03).
 *
 * Contracts verified:
 * 1. Student question DTO never exposes correctAnswer (GET /api/assessments/{id}).
 * 2. Un-enrolled students querying assessment details receive 404 Not Found (stealth IDOR defense).
 * 3. Leaky legacy question read endpoints are retired and return 404/405:
 *    - GET /api/tests/{testId}/questions
 *    - GET /api/questions/{id}
 * 4. Attempt review hides correctAnswer and marksAwarded while status is IN_PROGRESS.
 * 5. Attempt review only reveals correctAnswer after completion (POST /api/assessments/attempts/{id}/finalize).
 * 6. Cache-Control: private, no-store is returned on all assessment routes (NFR-03).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuestionAnswerPrivacyTests extends PostgresIntegrationTest {

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
    private Long enrolledStudentId;
    private Long unenrolledStudentId;
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

        UserAccount enrolledStudent = userAccountRepository.findByEmail("student@learnsphere.com");
        enrolledStudentId = enrolledStudent.getUserId();

        UserAccount unenrolledStudent = userAccountRepository.findByEmail("external@learnsphere.com");
        unenrolledStudentId = unenrolledStudent.getUserId();

        // Create LIVE course owned by teacher
        Course course = new Course();
        course.setTitle("Assessment Integrity 101");
        course.setDescription("Course on assessment integrity");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);
        courseId = course.getId();

        // Enroll student@learnsphere.com
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUserId(enrolledStudentId);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        // Create Test under course
        com.MOOC.OnlineLearningPlatfrom.Entity.Test test = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test.setCourse(course);
        test.setTitle("Midterm Examination");
        test.setDurationMinutes(60);
        test.setIsRemedial(false);
        test = testRepository.save(test);
        testId = test.getTestId();

        // Create Question with explicit correctAnswer
        Question question = new Question();
        question.setTest(test);
        question.setBody("What is the capital of France?");
        question.setQuestionType("MCQ");
        question.setMarks(5);
        question.setOptions(List.of("Paris", "London", "Berlin", "Madrid"));
        question.setCorrectAnswer("Paris");
        question = questionRepository.save(question);
        questionId = question.getQuestionId();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("W4-01: Student assessment DTO hides correctAnswer and returns private, no-store")
    void shouldNotExposeCorrectAnswerInStudentAssessmentDto() throws Exception {
        mockMvc.perform(get("/api/assessments/{id}", testId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.id").value(testId))
                .andExpect(jsonPath("$.title").value("Midterm Examination"))
                .andExpect(jsonPath("$.questions", hasSize(1)))
                .andExpect(jsonPath("$.questions[0].questionId").value(questionId))
                .andExpect(jsonPath("$.questions[0].body").value("What is the capital of France?"))
                .andExpect(jsonPath("$.questions[0].marks").value(5))
                .andExpect(jsonPath("$.questions[0].options", containsInAnyOrder("Paris", "London", "Berlin", "Madrid")))
                .andExpect(jsonPath("$.questions[0].correctAnswer").doesNotExist())
                .andExpect(jsonPath("$.questions[*].correctAnswer").doesNotExist());
    }

    @Test
    @DisplayName("W4-01: Assessment detail returns 404 for unenrolled students (IDOR stealth)")
    void shouldMaskAssessmentForUnenrolledStudent() throws Exception {
        mockMvc.perform(get("/api/assessments/{id}", testId)
                        .with(as("external@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("W4-01: Retired leaky GET /api/tests/{testId}/questions returns 404 or 405")
    void shouldRetireLeakyTestsQuestionsEndpoint() throws Exception {
        mockMvc.perform(get("/api/tests/{testId}/questions", testId)
                        .with(as("student@learnsphere.com")))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));

        mockMvc.perform(get("/api/tests/{testId}/questions", testId)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));
    }

    @Test
    @DisplayName("W4-01: Retired leaky GET /api/questions/{id} returns 404 or 405")
    void shouldRetireLeakyQuestionByIdEndpoint() throws Exception {
        mockMvc.perform(get("/api/questions/{id}", questionId)
                        .with(as("student@learnsphere.com")))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));

        mockMvc.perform(get("/api/questions/{id}", questionId)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));
    }

    @Test
    @DisplayName("W4-01: Attempt review hides correctAnswer and marksAwarded while IN_PROGRESS")
    void shouldHideCorrectAnswerDuringInProgressAttempt() throws Exception {
        // Start attempt
        String startResponse = mockMvc.perform(post("/api/assessments/{id}/start", testId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.attemptId").isNumber())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andReturn().getResponse().getContentAsString();

        Long attemptId = com.jayway.jsonpath.JsonPath.parse(startResponse).read("$.attemptId", Long.class);

        // Submit answer
        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/answers", attemptId)
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + questionId + ",\"answerText\":\"Paris\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));

        // Read attempt review while still IN_PROGRESS
        mockMvc.perform(get("/api/assessments/attempts/{attemptId}", attemptId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.score").doesNotExist())
                .andExpect(jsonPath("$.answers", hasSize(1)))
                .andExpect(jsonPath("$.answers[0].studentAnswer").value("Paris"))
                .andExpect(jsonPath("$.answers[0].correctAnswer").doesNotExist())
                .andExpect(jsonPath("$.answers[0].marksAwarded").doesNotExist());
    }

    @Test
    @DisplayName("W4-01: Finalizing attempt reveals score and correctAnswer")
    void shouldRevealCorrectAnswerOnlyAfterFinalizeAttempt() throws Exception {
        // Start attempt
        String startResponse = mockMvc.perform(post("/api/assessments/{id}/start", testId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long attemptId = com.jayway.jsonpath.JsonPath.parse(startResponse).read("$.attemptId", Long.class);

        // Submit answer
        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/answers", attemptId)
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionId\":" + questionId + ",\"answerText\":\"Paris\"}"))
                .andExpect(status().isOk());

        // Finalize attempt
        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/finalize", attemptId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.answers", hasSize(1)))
                .andExpect(jsonPath("$.answers[0].studentAnswer").value("Paris"))
                .andExpect(jsonPath("$.answers[0].correctAnswer").value("Paris"))
                .andExpect(jsonPath("$.answers[0].marksAwarded").value(5));

        // Read attempt review after completion
        mockMvc.perform(get("/api/assessments/attempts/{attemptId}", attemptId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(5))
                .andExpect(jsonPath("$.answers[0].correctAnswer").value("Paris"))
                .andExpect(jsonPath("$.answers[0].marksAwarded").value(5));
    }
}
