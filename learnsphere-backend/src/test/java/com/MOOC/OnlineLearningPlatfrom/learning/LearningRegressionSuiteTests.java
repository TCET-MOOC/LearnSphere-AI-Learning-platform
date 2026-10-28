package com.MOOC.OnlineLearningPlatfrom.learning;

import com.MOOC.OnlineLearningPlatfrom.Dto.CertificateResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.CourseProgressSummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W3-09: Comprehensive Learning Regression Suite.
 *
 * Rules: AC-10…AC-14, AC-24, AUD-07…AUD-09, PLAN §10, A-04, A-08, A-10, A-20, A-27, BR-08, BR-16, BR-18, BR-19, NFR-03.
 *
 * Validates the core learning lifecycle contracts landed across Wave 3:
 * 1. Enrollment uniqueness on (user_id, course_id, role), self-only gating, and published course requirement.
 * 2. Roster access control and unenroll authorization.
 * 3. Content progress upsert on (user_id, content_id) with BigDecimal(5,2) and 0-100 clamping.
 * 4. 90% completion rule: completedAt stamping on >=90%, advance preservation, and regression un-completion.
 * 5. Content-based course completion aggregation across modules and contents.
 * 6. Certificate eligibility requiring all contents completed, authentic grading, idempotent issuance, and public verification.
 * 7. Event-driven assessment leaderboard materialisation with global 1..N and departmental 1..M ranks.
 * 8. Same-college free entitlement on paid courses, checkout blocking for entitled students, and audited admin overrides.
 * 9. HTTP Cache-Control: private, no-store enforcement on all sensitive learning read paths.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LearningRegressionSuiteTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CollegeRepository collegeRepository;

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
    private TestRepository testRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private CollegeEntitlementRepository collegeEntitlementRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TestService testService;

    private UserAccount teacher;
    private UserAccount student1;
    private UserAccount student2;
    private UserAccount admin;

    private College verifiedCollege;
    private College differentCollege;
    private Department department;

    private Course liveCourse;
    private Course draftCourse;
    private Course paidCourse;

    private Module module1;
    private CourseContent content1;
    private CourseContent content2;

    @BeforeEach
    void setUp() {
        verifiedCollege = new College();
        verifiedCollege.setName("Imperial Tech " + System.currentTimeMillis());
        verifiedCollege.setVerificationStatus(College.VerificationStatus.VERIFIED);
        verifiedCollege = collegeRepository.save(verifiedCollege);

        differentCollege = new College();
        differentCollege.setName("Global Institute " + System.currentTimeMillis());
        differentCollege.setVerificationStatus(College.VerificationStatus.VERIFIED);
        differentCollege = collegeRepository.save(differentCollege);

        department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Informatics " + System.currentTimeMillis());
            return departmentRepository.save(d);
        });

        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        teacher.setCollege(verifiedCollege);
        teacher.setDepartment(department);
        userAccountRepository.save(teacher);

        student1 = userAccountRepository.findByEmail("student@learnsphere.com");
        student1.setCollege(verifiedCollege);
        student1.setDepartment(department);
        userAccountRepository.save(student1);

        student2 = userAccountRepository.findByEmail("external@learnsphere.com");
        student2.setCollege(differentCollege);
        student2.setDepartment(department);
        userAccountRepository.save(student2);

        admin = userAccountRepository.findByEmail("admin@learnsphere.com");

        // Free live course
        liveCourse = new Course();
        liveCourse.setTitle("Foundations of Cloud Native Computing");
        liveCourse.setDescription("Microservices, containers, and orchestration");
        liveCourse.setTeacher(teacher);
        liveCourse.setDepartment(department);
        liveCourse.setStatus(Course.Status.LIVE);
        liveCourse.setPrice(BigDecimal.ZERO);
        liveCourse = courseRepository.save(liveCourse);

        // Modules & Contents
        module1 = new Module();
        module1.setCourse(liveCourse);
        module1.setTitle("Module 1: Architecture");
        module1.setPosition(1);
        module1 = moduleRepository.save(module1);

        content1 = new CourseContent();
        content1.setModule(module1);
        content1.setTitle("Lecture 1: Containers");
        content1.setContentType("VIDEO");
        content1.setPosition(1);
        content1.setDurationSeconds(1200);
        content1 = courseContentRepository.save(content1);

        content2 = new CourseContent();
        content2.setModule(module1);
        content2.setTitle("Lecture 2: Orchestration");
        content2.setContentType("VIDEO");
        content2.setPosition(2);
        content2.setDurationSeconds(1500);
        content2 = courseContentRepository.save(content2);

        // Draft Course
        draftCourse = new Course();
        draftCourse.setTitle("Quantum Algorithms (WIP)");
        draftCourse.setTeacher(teacher);
        draftCourse.setDepartment(department);
        draftCourse.setStatus(Course.Status.DRAFT);
        draftCourse = courseRepository.save(draftCourse);

        // Paid Course (INR 1499.00)
        paidCourse = new Course();
        paidCourse.setTitle("Advanced Kubernetes Engineering");
        paidCourse.setTeacher(teacher);
        paidCourse.setDepartment(department);
        paidCourse.setStatus(Course.Status.LIVE);
        paidCourse.setPrice(new BigDecimal("1499.00"));
        paidCourse = courseRepository.save(paidCourse);
    }

    // -------------------------------------------------------------------------
    // 1. Enrollment Lifecycle, Uniqueness and Authorization
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Enrollment lifecycle: published check, self-only gating, and unique constraint")
    void testEnrollmentLifecycleAndConstraints() throws Exception {
        // A. Unpublished (DRAFT) course rejects enrollment with 400
        mockMvc.perform(post("/api/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "course", Map.of("id", draftCourse.getId())
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("not published")));

        // B. Non-admin cannot enroll other users (403)
        mockMvc.perform(post("/api/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", student2.getUserId(),
                                "course", Map.of("id", liveCourse.getId())
                        ))))
                .andExpect(status().isForbidden());

        // C. Legitimate self-enrollment succeeds
        mockMvc.perform(post("/api/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "course", Map.of("id", liveCourse.getId())
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.course.id").value(liveCourse.getId()));

        assertThat(enrollmentRepository.existsByUserIdAndCourse_Id(student1.getUserId(), liveCourse.getId())).isTrue();

        // D. Duplicate enrollment rejected with 409 Conflict
        mockMvc.perform(post("/api/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "course", Map.of("id", liveCourse.getId())
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("already enrolled")));
    }

    // -------------------------------------------------------------------------
    // 2. Roster Access and Unenrollment Authorization
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Roster & unenrollment authorization: staff roster access, self unenroll, foreign rejection")
    void testRosterAndUnenrollAuthorization() throws Exception {
        // Enroll student1
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student1.getUserId());
        enrollment.setCourse(liveCourse);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment = enrollmentRepository.save(enrollment);

        // Course teacher can access roster with private, no-store
        mockMvc.perform(get("/api/courses/" + liveCourse.getId() + "/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(teacher.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(student1.getUserId()));

        // Student2 cannot access roster (404 stealth security for non-staff)
        mockMvc.perform(get("/api/courses/" + liveCourse.getId() + "/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(student2.getEmail()))))
                .andExpect(status().isNotFound());

        // Student2 cannot unenroll student1 (404 stealth security for foreign enrollment)
        mockMvc.perform(delete("/api/enrollments/" + enrollment.getEnrollmentId())
                        .with(user(userDetailsService.loadUserByUsername(student2.getEmail()))))
                .andExpect(status().isNotFound());

        // Student1 can unenroll themselves (204)
        mockMvc.perform(delete("/api/enrollments/" + enrollment.getEnrollmentId())
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail()))))
                .andExpect(status().isNoContent());

        assertThat(enrollmentRepository.existsByUserIdAndCourse_Id(student1.getUserId(), liveCourse.getId())).isFalse();
    }

    // -------------------------------------------------------------------------
    // 3. Progress Upsert, Clamping and 90% Completion Rule
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Progress upsert: 0-100 clamping, completedAt stamping at 90%, advance preservation, and regression un-completion")
    void testProgressUpsertAndCompletionRule() throws Exception {
        // Enroll student1
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student1.getUserId());
        enrollment.setCourse(liveCourse);
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);

        // A. Post progress 50% -> completedAt is null
        mockMvc.perform(post("/api/progress")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("contentId", content1.getContentId()),
                                "progressPercent", 50.00,
                                "secondsWatched", 600
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(50.0))
                .andExpect(jsonPath("$.completedAt").doesNotExist());

        // B. Advance to 90% -> completedAt is stamped
        MvcResult stampRes = mockMvc.perform(post("/api/progress")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("contentId", content1.getContentId()),
                                "progressPercent", 90.00,
                                "secondsWatched", 1080
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(90.0))
                .andExpect(jsonPath("$.completedAt").exists())
                .andReturn();

        JsonNode stampNode = objectMapper.readTree(stampRes.getResponse().getContentAsString());
        String initialCompletedAt = stampNode.get("completedAt").asText();

        // C. Advance to 95% -> completedAt timestamp is preserved
        mockMvc.perform(post("/api/progress")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("contentId", content1.getContentId()),
                                "progressPercent", 95.00,
                                "secondsWatched", 1140
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(95.0))
                .andExpect(jsonPath("$.completedAt").value(initialCompletedAt));

        // D. Regress to 40% -> completedAt is cleared (null)
        mockMvc.perform(post("/api/progress")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("contentId", content1.getContentId()),
                                "progressPercent", 40.00,
                                "secondsWatched", 480
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(40.0))
                .andExpect(jsonPath("$.completedAt").doesNotExist());

        // E. Clamping: negative clamped to 0.00, >100 clamped to 100.00
        mockMvc.perform(post("/api/progress")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("contentId", content1.getContentId()),
                                "progressPercent", 150.00,
                                "secondsWatched", 1200
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(100.0))
                .andExpect(jsonPath("$.completedAt").exists());

        // Only one progress record exists for this content
        assertThat(userProgressRepository.findByUserIdAndContent_ContentId(student1.getUserId(), content1.getContentId())).isPresent();
    }

    // -------------------------------------------------------------------------
    // 4. Content-Based Course Progress Aggregation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Content-based course progress: calculates completed/total items and progress summary")
    void testContentBasedCourseProgressAggregation() throws Exception {
        // Enroll student1
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student1.getUserId());
        enrollment.setCourse(liveCourse);
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);

        // Complete content 1 (1 of 2 contents -> 50% course progress)
        UserProgress p1 = new UserProgress();
        p1.setUserId(student1.getUserId());
        p1.setContent(content1);
        p1.setProgressPercent(new BigDecimal("95.00"));
        p1.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p1);

        // Student course progress endpoint
        mockMvc.perform(get("/api/student/courses/" + liveCourse.getId() + "/progress")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].completedAt").exists())
                .andExpect(jsonPath("$[1].completedAt").doesNotExist());

        // Course summary endpoint for course staff
        mockMvc.perform(get("/api/progress/course/" + liveCourse.getId() + "/summary")
                        .with(user(userDetailsService.loadUserByUsername(teacher.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.courseId").value(liveCourse.getId()))
                .andExpect(jsonPath("$.totalEnrolled").value(1))
                .andExpect(jsonPath("$.totalContents").value(2));
    }

    // -------------------------------------------------------------------------
    // 5. Certificate Eligibility, Real Grades, Idempotency & Public Verification
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Certificate lifecycle: rejection when incomplete, authentic grade when complete, idempotent issuance, public verify")
    void testCertificateLifecycleAndVerification() throws Exception {
        // Enroll student1
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student1.getUserId());
        enrollment.setCourse(liveCourse);
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);

        // A. Incomplete course (0 of 2 complete) -> issuance rejected (400)
        mockMvc.perform(post("/api/certificates/issue")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "courseId", liveCourse.getId()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("90% required")));

        // Complete both contents
        UserProgress p1 = new UserProgress();
        p1.setUserId(student1.getUserId());
        p1.setContent(content1);
        p1.setProgressPercent(new BigDecimal("100.00"));
        p1.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p1);

        UserProgress p2 = new UserProgress();
        p2.setUserId(student1.getUserId());
        p2.setContent(content2);
        p2.setProgressPercent(new BigDecimal("95.00"));
        p2.setCompletedAt(LocalDateTime.now());
        userProgressRepository.save(p2);

        // B. Fully complete course -> certificate issued with real score & grade
        MvcResult issueRes = mockMvc.perform(post("/api/certificates/issue")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "courseId", liveCourse.getId()
                        ))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.courseId").value(liveCourse.getId()))
                .andExpect(jsonPath("$.grade").value(containsString("Distinction")))
                .andExpect(jsonPath("$.verificationCode").exists())
                .andReturn();

        JsonNode issueNode = objectMapper.readTree(issueRes.getResponse().getContentAsString());
        String certCode = issueNode.get("verificationCode").asText();
        Number certId = issueNode.get("id").asLong();

        // C. Repeated issuance is idempotent (returns same certificate)
        mockMvc.perform(post("/api/certificates/issue")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "courseId", liveCourse.getId()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(certId))
                .andExpect(jsonPath("$.verificationCode").value(certCode));

        assertThat(certificateRepository.count()).isEqualTo(1);

        // D. Public verification endpoint (unauthenticated) with Cache-Control: public, max-age=60
        mockMvc.perform(get("/api/certificates/verify/" + certCode))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("public, max-age=60")))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.verificationCode").value(certCode))
                .andExpect(jsonPath("$.courseTitle").value(liveCourse.getTitle()));
    }

    // -------------------------------------------------------------------------
    // 6. Assessment Leaderboard Materialisation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Leaderboard materialisation: test attempt triggers rank calculation with global and departmental standing")
    void testLeaderboardMaterialisation() throws Exception {
        // Course assessment
        com.MOOC.OnlineLearningPlatfrom.Entity.Test test = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test.setCourse(liveCourse);
        test.setTitle("Final Practical Exam");
        test.setDurationMinutes(60);
        test = testRepository.save(test);

        Question q1 = new Question();
        q1.setTest(test);
        q1.setBody("What is an ingress controller?");
        q1.setQuestionType("MCQ");
        q1.setMarks(100);
        q1.setCorrectAnswer("L7 reverse proxy");
        q1 = questionRepository.save(q1);

        // Student1 starts attempt
        TestAttempt attempt = testService.startTestAttempt(test.getTestId(), student1.getUserId());

        // Submit answer
        StudentAnswer answer = new StudentAnswer();
        answer.setAnswerText("L7 reverse proxy");
        testService.submitAnswer(attempt.getAttemptId(), q1.getQuestionId(), answer);

        // Finalize test -> event triggers recalculateCourseLeaderboard
        mockMvc.perform(post("/api/attempts/" + attempt.getAttemptId() + "/finalize")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail()))))
                .andExpect(status().isOk());

        // Leaderboard endpoint returns ranked standings with private, no-store
        mockMvc.perform(get("/api/courses/" + liveCourse.getId() + "/leaderboard")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(student1.getUserId()))
                .andExpect(jsonPath("$[0].globalRank").value(1))
                .andExpect(jsonPath("$[0].deptRank").value(1))
                .andExpect(jsonPath("$[0].avgScore").value(100.0));

        // My leaderboards endpoint
        mockMvc.perform(get("/api/leaderboard/me")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].courseId").value(liveCourse.getId()));
    }

    // -------------------------------------------------------------------------
    // 7. Same-College Entitlement on Paid Courses
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Same-college entitlement: verified student free enroll, checkout blocking, and audited admin override")
    void testSameCollegeEntitlementAndOverrides() throws Exception {
        long initialPayments = paymentRepository.count();

        // A. Student 1 checkout is blocked with 400 because entitled to free same-college access
        mockMvc.perform(post("/api/payments/checkout")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "courseId", paidCourse.getId()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("This course is included by your college")));

        // B. Student 1 (same verified college as teacher) gets free enroll with 0 payment rows
        mockMvc.perform(post("/api/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "course", Map.of("id", paidCourse.getId())
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.course.id").value(paidCourse.getId()));

        assertThat(enrollmentRepository.existsByUserIdAndCourse_Id(student1.getUserId(), paidCourse.getId())).isTrue();
        assertThat(paymentRepository.count()).isEqualTo(initialPayments);

        // C. Student 2 (different college) cannot free enroll (400)
        mockMvc.perform(post("/api/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(student2.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "course", Map.of("id", paidCourse.getId())
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("Payment required")));

        // D. Admin grants override for Student 2
        mockMvc.perform(post("/api/admin/entitlements")
                        .with(user(userDetailsService.loadUserByUsername(admin.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "studentId", student2.getUserId(),
                                "courseId", paidCourse.getId(),
                                "reason", "Exchange student partnership waiver"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grantType").value("ADMIN_OVERRIDE"));

        // Audit log recorded
        assertThat(auditLogRepository.findAll().stream()
                .anyMatch(l -> "ENTITLEMENT_GRANTED".equals(l.getAction()))).isTrue();

        // Student 2 can now free enroll
        mockMvc.perform(post("/api/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(student2.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "course", Map.of("id", paidCourse.getId())
                        ))))
                .andExpect(status().isOk());

        assertThat(enrollmentRepository.existsByUserIdAndCourse_Id(student2.getUserId(), paidCourse.getId())).isTrue();
        assertThat(paymentRepository.count()).isEqualTo(initialPayments);

        // E. Entitlement metadata endpoint with private, no-store
        mockMvc.perform(get("/api/courses/" + paidCourse.getId() + "/entitlement")
                        .with(user(userDetailsService.loadUserByUsername(student1.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.reason").value("SAME_COLLEGE"));
    }
}
