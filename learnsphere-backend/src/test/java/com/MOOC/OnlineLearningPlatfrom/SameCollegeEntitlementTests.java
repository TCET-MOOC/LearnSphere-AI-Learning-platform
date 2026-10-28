package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SameCollegeEntitlementTests extends PostgresIntegrationTest {

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
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private College verifiedCollegeA;
    private College verifiedCollegeB;
    private College pendingCollegeC;
    private Department department;

    private UserAccount teacherA;
    private UserAccount studentA;
    private UserAccount studentB;
    private UserAccount studentC;
    private UserAccount adminUser;

    private Course paidCourseTeacherA;

    @BeforeEach
    void setUp() {
        // Colleges
        verifiedCollegeA = new College();
        verifiedCollegeA.setName("Institute of Technology Alpha " + System.currentTimeMillis());
        verifiedCollegeA.setVerificationStatus(College.VerificationStatus.VERIFIED);
        verifiedCollegeA = collegeRepository.save(verifiedCollegeA);

        verifiedCollegeB = new College();
        verifiedCollegeB.setName("University of Beta " + System.currentTimeMillis());
        verifiedCollegeB.setVerificationStatus(College.VerificationStatus.VERIFIED);
        verifiedCollegeB = collegeRepository.save(verifiedCollegeB);

        pendingCollegeC = new College();
        pendingCollegeC.setName("Gamma College Unverified " + System.currentTimeMillis());
        pendingCollegeC.setVerificationStatus(College.VerificationStatus.PENDING);
        pendingCollegeC = collegeRepository.save(pendingCollegeC);

        // Department
        department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Engineering " + System.currentTimeMillis());
            return departmentRepository.save(d);
        });

        // Seeded users
        teacherA = userAccountRepository.findByEmail("teacher@learnsphere.com");
        teacherA.setCollege(verifiedCollegeA);
        userAccountRepository.save(teacherA);

        studentA = userAccountRepository.findByEmail("student@learnsphere.com");
        studentA.setCollege(verifiedCollegeA);
        userAccountRepository.save(studentA);

        studentB = userAccountRepository.findByEmail("external@learnsphere.com");
        studentB.setCollege(verifiedCollegeB);
        userAccountRepository.save(studentB);

        studentC = userAccountRepository.findByEmail("pending@learnsphere.com");
        studentC.setCollege(pendingCollegeC);
        userAccountRepository.save(studentC);

        adminUser = userAccountRepository.findByEmail("admin@learnsphere.com");

        // Paid Course by Teacher A (INR 999.00)
        paidCourseTeacherA = new Course();
        paidCourseTeacherA.setTitle("Advanced Distributed Systems " + System.currentTimeMillis());
        paidCourseTeacherA.setTeacher(teacherA);
        paidCourseTeacherA.setDepartment(department);
        paidCourseTeacherA.setStatus(Course.Status.LIVE);
        paidCourseTeacherA.setPrice(new BigDecimal("999.00"));
        paidCourseTeacherA = courseRepository.save(paidCourseTeacherA);
    }

    @Test
    @DisplayName("Verified student in same verified college gets free enroll with 0 payment rows created")
    void verifiedStudentSameCollege_GetsFreeEnrol_NoPaymentRow() throws Exception {
        long initialPayments = paymentRepository.count();

        mockMvc.perform(post("/api/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(studentA.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "course", Map.of("id", paidCourseTeacherA.getId())
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.course.id").value(paidCourseTeacherA.getId()));

        assertThat(enrollmentRepository.existsByUserIdAndCourse_Id(studentA.getUserId(), paidCourseTeacherA.getId())).isTrue();
        assertThat(paymentRepository.count()).isEqualTo(initialPayments);
    }

    @Test
    @DisplayName("Different college student cannot free enroll without payment (rejected 400)")
    void differentCollegeStudent_CannotFreeEnrol_Rejected() throws Exception {
        mockMvc.perform(post("/api/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(studentB.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "course", Map.of("id", paidCourseTeacherA.getId())
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Payment required")));

        assertThat(enrollmentRepository.existsByUserIdAndCourse_Id(studentB.getUserId(), paidCourseTeacherA.getId())).isFalse();
    }

    @Test
    @DisplayName("Unverified college student cannot free enroll (rejected 400)")
    void unverifiedCollegeStudent_CannotFreeEnrol_Rejected() throws Exception {
        mockMvc.perform(post("/api/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(studentC.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "course", Map.of("id", paidCourseTeacherA.getId())
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Payment required")));
    }

    @Test
    @DisplayName("Checkout is blocked with 400 for student entitled to same-college free access")
    void checkoutBlocked_ForEntitledStudent() throws Exception {
        mockMvc.perform(post("/api/payments/checkout")
                        .with(user(userDetailsService.loadUserByUsername(studentA.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "courseId", paidCourseTeacherA.getId()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("This course is included by your college")));
    }

    @Test
    @DisplayName("Admin can grant entitlement override with audit log, enabling free enroll for different-college student")
    void adminGrantOverride_EnablesFreeEnrol_Audited() throws Exception {
        long initialPayments = paymentRepository.count();

        // Admin grants override
        mockMvc.perform(post("/api/admin/entitlements")
                        .with(user(userDetailsService.loadUserByUsername(adminUser.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "studentId", studentB.getUserId(),
                                "courseId", paidCourseTeacherA.getId(),
                                "reason", "Exchange student institutional waiver"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grantType").value("ADMIN_OVERRIDE"))
                .andExpect(jsonPath("$.studentId").value(studentB.getUserId()));

        // Check audit log recorded
        assertThat(auditLogRepository.findAll().stream()
                .anyMatch(l -> "ENTITLEMENT_GRANTED".equals(l.getAction()))).isTrue();

        // Now studentB can enroll freely
        mockMvc.perform(post("/api/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(studentB.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "course", Map.of("id", paidCourseTeacherA.getId())
                        ))))
                .andExpect(status().isOk());

        assertThat(enrollmentRepository.existsByUserIdAndCourse_Id(studentB.getUserId(), paidCourseTeacherA.getId())).isTrue();
        assertThat(paymentRepository.count()).isEqualTo(initialPayments);
    }

    @Test
    @DisplayName("Admin can revoke entitlement override with audit log, restoring payment requirement")
    void adminRevokeOverride_RestoresPaymentRequirement() throws Exception {
        // Admin grants override
        var grantRes = mockMvc.perform(post("/api/admin/entitlements")
                        .with(user(userDetailsService.loadUserByUsername(adminUser.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "studentId", studentB.getUserId(),
                                "courseId", paidCourseTeacherA.getId(),
                                "reason", "Temporary test waiver"
                        ))))
                .andExpect(status().isOk())
                .andReturn();

        Map<?, ?> grantBody = objectMapper.readValue(grantRes.getResponse().getContentAsString(), Map.class);
        Number entitlementId = (Number) grantBody.get("id");

        // Admin revokes override
        mockMvc.perform(delete("/api/admin/entitlements/" + entitlementId)
                        .with(user(userDetailsService.loadUserByUsername(adminUser.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "Waiver period expired"
                        ))))
                .andExpect(status().isNoContent());

        // Check audit log recorded revocation
        assertThat(auditLogRepository.findAll().stream()
                .anyMatch(l -> "ENTITLEMENT_REVOKED".equals(l.getAction()))).isTrue();

        // StudentB free enroll should now fail with 400 Payment required
        mockMvc.perform(post("/api/enrollments")
                        .with(user(userDetailsService.loadUserByUsername(studentB.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "course", Map.of("id", paidCourseTeacherA.getId())
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Payment required")));
    }

    @Test
    @DisplayName("GET /api/courses/{id}/entitlement returns eligibility status with Cache-Control: private, no-store")
    void getCourseEntitlement_ReturnsStatusAndPrivateNoStore() throws Exception {
        // Student A (same college) is eligible
        mockMvc.perform(get("/api/courses/" + paidCourseTeacherA.getId() + "/entitlement")
                        .with(user(userDetailsService.loadUserByUsername(studentA.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private, no-store")))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.reason").value("SAME_COLLEGE"))
                .andExpect(jsonPath("$.collegeName").value(org.hamcrest.Matchers.containsString("Institute of Technology Alpha")));

        // Student B (different college) is not eligible
        mockMvc.perform(get("/api/courses/" + paidCourseTeacherA.getId() + "/entitlement")
                        .with(user(userDetailsService.loadUserByUsername(studentB.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private, no-store")))
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.reason").value("NONE"));
    }
}
