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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W3-06 integration tests:
 * - Certificate eligibility + real grade/score
 * - Idempotent certificate issuance per (user, course, type)
 * - 90 % per-content completion requirement for standard certificates
 * - Real grade & score calculation from tests or content completion (C-F9)
 * - Public verification (GET /api/certificates/verify/{code}) without authentication
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CertificateEligibilityAndVerificationTests extends PostgresIntegrationTest {

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
    private CertificateRepository certificateRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private QuestionRepository questionRepository;

    private UserAccount student;
    private UserAccount foreignStudent;
    private UserAccount teacher;
    private Course course;
    private CourseContent content1;
    private CourseContent content2;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        foreignStudent = userAccountRepository.findByEmail("external@learnsphere.com");

        Department department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Engineering W306");
            return departmentRepository.save(d);
        });

        course = new Course();
        course.setTitle("Advanced Distributed Systems");
        course.setDescription("Curriculum on consensus algorithms and distributed state");
        course.setDepartment(department);
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course.setPrice(BigDecimal.ZERO);
        course = courseRepository.save(course);

        Module module = new Module();
        module.setCourse(course);
        module.setTitle("Consensus Protocols");
        module.setPosition(1);
        module = moduleRepository.save(module);

        content1 = new CourseContent();
        content1.setModule(module);
        content1.setTitle("Raft Leadership");
        content1.setContentType("VIDEO");
        content1.setFilePath("https://example.com/raft1.mp4");
        content1.setPosition(1);
        content1.setDurationSeconds(1200);
        content1 = courseContentRepository.save(content1);

        content2 = new CourseContent();
        content2.setModule(module);
        content2.setTitle("Log Compaction");
        content2.setContentType("VIDEO");
        content2.setFilePath("https://example.com/raft2.mp4");
        content2.setPosition(2);
        content2.setDurationSeconds(1500);
        content2 = courseContentRepository.save(content2);

        // Enroll student
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUserId(student.getUserId());
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now().minusDays(5));
        enrollmentRepository.save(enrollment);
    }

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("Eligibility: Issue succeeds when all course contents are completed (≥ 90%)")
    void shouldIssueStandardCertificateWhenCourseIsFullyCompleted() throws Exception {
        // Complete both contents at 95% and 92%
        UserProgress p1 = new UserProgress();
        p1.setUserId(student.getUserId());
        p1.setContent(content1);
        p1.setProgressPercent(BigDecimal.valueOf(95.00));
        p1.setSecondsWatched(1140);
        p1.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p1);

        UserProgress p2 = new UserProgress();
        p2.setUserId(student.getUserId());
        p2.setContent(content2);
        p2.setProgressPercent(BigDecimal.valueOf(92.00));
        p2.setSecondsWatched(1380);
        p2.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p2);

        String issuePayload = String.format("""
                {
                    "courseId": %d,
                    "type": "STANDARD"
                }
                """, course.getId());

        mockMvc.perform(post("/api/certificates/issue")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issuePayload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.courseId").value(course.getId()))
                .andExpect(jsonPath("$.studentName").value(student.getFullName()))
                .andExpect(jsonPath("$.verificationCode").value(containsString("LS-")))
                .andExpect(jsonPath("$.score").isNumber())
                .andExpect(jsonPath("$.grade").isNotEmpty());
    }

    @Test
    @DisplayName("Eligibility: Issue rejected with 400 when contents are incomplete")
    void shouldRejectIssuanceWhenCourseContentNotFullyCompleted() throws Exception {
        // Complete only 1 of 2 contents
        UserProgress p1 = new UserProgress();
        p1.setUserId(student.getUserId());
        p1.setContent(content1);
        p1.setProgressPercent(BigDecimal.valueOf(95.00));
        p1.setSecondsWatched(1140);
        p1.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p1);

        // Content 2 is only at 50%
        UserProgress p2 = new UserProgress();
        p2.setUserId(student.getUserId());
        p2.setContent(content2);
        p2.setProgressPercent(BigDecimal.valueOf(50.00));
        p2.setSecondsWatched(750);
        userProgressRepository.save(p2);

        String issuePayload = String.format("""
                {
                    "courseId": %d,
                    "type": "STANDARD"
                }
                """, course.getId());

        mockMvc.perform(post("/api/certificates/issue")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issuePayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Eligibility: Issue rejected with 400 when student is not enrolled in course")
    void shouldRejectIssuanceWhenUserNotEnrolledInCourse() throws Exception {
        String issuePayload = String.format("""
                {
                    "courseId": %d,
                    "type": "STANDARD"
                }
                """, course.getId());

        mockMvc.perform(post("/api/certificates/issue")
                        .with(as(foreignStudent.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issuePayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Idempotency: Repeated issue calls return identical certificate")
    void shouldBeIdempotentWhenIssueCalledMultipleTimes() throws Exception {
        // Complete both contents
        UserProgress p1 = new UserProgress();
        p1.setUserId(student.getUserId());
        p1.setContent(content1);
        p1.setProgressPercent(BigDecimal.valueOf(100.00));
        p1.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p1);

        UserProgress p2 = new UserProgress();
        p2.setUserId(student.getUserId());
        p2.setContent(content2);
        p2.setProgressPercent(BigDecimal.valueOf(100.00));
        p2.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p2);

        String issuePayload = String.format("""
                {
                    "courseId": %d,
                    "type": "STANDARD"
                }
                """, course.getId());

        // Call 1
        String resp1 = mockMvc.perform(post("/api/certificates/issue")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issuePayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Call 2
        String resp2 = mockMvc.perform(post("/api/certificates/issue")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issuePayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(resp1).isEqualTo(resp2);
        assertThat(certificateRepository.findByUser_UserId(student.getUserId())).hasSize(1);
    }

    @Test
    @DisplayName("Scoring: Real test score is computed when course tests have been completed (C-F9)")
    void shouldComputeRealScoreAndGradeFromCourseTestsWhenAvailable() throws Exception {
        // Complete content items
        UserProgress p1 = new UserProgress();
        p1.setUserId(student.getUserId());
        p1.setContent(content1);
        p1.setProgressPercent(BigDecimal.valueOf(100.00));
        p1.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p1);

        UserProgress p2 = new UserProgress();
        p2.setUserId(student.getUserId());
        p2.setContent(content2);
        p2.setProgressPercent(BigDecimal.valueOf(100.00));
        p2.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p2);

        // Add a course test
        com.MOOC.OnlineLearningPlatfrom.Entity.Test courseTest = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        courseTest.setCourse(course);
        courseTest.setTitle("Distributed Consensus Final Exam");
        courseTest.setDurationMinutes(60);
        courseTest = testRepository.save(courseTest);

        Question q1 = new Question();
        q1.setTest(courseTest);
        q1.setBody("What is the quorum formula?");
        q1.setQuestionType("MCQ");
        q1.setMarks(10);
        q1.setCorrectAnswer("N/2 + 1");
        questionRepository.save(q1);

        Question q2 = new Question();
        q2.setTest(courseTest);
        q2.setBody("Explain split-brain prevention");
        q2.setQuestionType("MCQ");
        q2.setMarks(10);
        q2.setCorrectAnswer("Majority quorum");
        questionRepository.save(q2);

        // Student scored 18 out of 20 (90%)
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(courseTest);
        attempt.setUserId(student.getUserId());
        attempt.setScore(18);
        attempt.setStatus("COMPLETED");
        attempt.setAttemptedAt(LocalDateTime.now().minusHours(2));
        testAttemptRepository.save(attempt);

        String issuePayload = String.format("""
                {
                    "courseId": %d,
                    "type": "STANDARD"
                }
                """, course.getId());

        mockMvc.perform(post("/api/certificates/issue")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(issuePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(90.0))
                .andExpect(jsonPath("$.grade").value("Distinction (90%)"));
    }

    @Test
    @DisplayName("Verification: Public GET /api/certificates/verify/{code} verifies successfully without auth")
    void shouldVerifyCertificatePubliclyWithoutAuthentication() throws Exception {
        Certificate cert = new Certificate();
        cert.setUser(student);
        cert.setCourse(course);
        cert.setType(Certificate.Type.STANDARD);
        cert.setStudentName(student.getFullName());
        cert.setInstructorName(teacher.getFullName());
        cert.setGrade("Distinction (94%)");
        cert.setScore(94.0);
        cert.setTitle("Certificate of Completion - Advanced Distributed Systems");
        cert.setVerificationCode("LS-2026-TESTCODE");
        certificateRepository.save(cert);

        mockMvc.perform(get("/api/certificates/verify/LS-2026-TESTCODE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.verificationCode").value("LS-2026-TESTCODE"))
                .andExpect(jsonPath("$.studentName").value(student.getFullName()))
                .andExpect(jsonPath("$.courseTitle").value(course.getTitle()))
                .andExpect(jsonPath("$.grade").value("Distinction (94%)"))
                .andExpect(jsonPath("$.score").value(94.0));
    }

    @Test
    @DisplayName("Verification: Public GET /api/certificates/verify/{code} returns valid: false for unknown code")
    void shouldReturnInvalidForUnknownVerificationCode() throws Exception {
        mockMvc.perform(get("/api/certificates/verify/LS-UNKNOWN-999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.verificationCode").value("LS-UNKNOWN-999"));
    }

    @Test
    @DisplayName("My Certificates: GET /api/certificates returns user's certificates with private, no-store")
    void shouldReturnMyCertificatesWithPrivateNoStore() throws Exception {
        Certificate cert = new Certificate();
        cert.setUser(student);
        cert.setCourse(course);
        cert.setType(Certificate.Type.STANDARD);
        cert.setStudentName(student.getFullName());
        cert.setGrade("Distinction (95%)");
        cert.setScore(95.0);
        cert.setTitle("Certificate of Completion - Advanced Distributed Systems");
        cert.setVerificationCode("LS-2026-MINE001");
        certificateRepository.save(cert);

        mockMvc.perform(get("/api/certificates")
                        .with(as(student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].verificationCode").value("LS-2026-MINE001"));
    }
}
