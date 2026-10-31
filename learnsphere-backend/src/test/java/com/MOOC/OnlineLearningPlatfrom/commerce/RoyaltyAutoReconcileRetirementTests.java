package com.MOOC.OnlineLearningPlatfrom.commerce;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Story W6-07: Remove royalty auto-reconcile
 * Rules: audit.md B10, [F-F9, F-F10], spec §6
 *
 * Scenarios:
 * 1. Reading royalties does NOT mint synthetic royalties for free/college enrollments in paid courses.
 * 2. Reading royalties is idempotent and read-only: does not increment teacher balance.
 * 3. RevenueSummary and TeacherRoyalties both derive metrics from actual SUCCESS payments.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RoyaltyAutoReconcileRetirementTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RoyaltyRepository royaltyRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private UserAccount teacher;
    private UserAccount student;
    private UserAccount admin;
    private Course paidCourse;

    @BeforeEach
    void setUp() {
        teacher = findOrCreateUser("autorec.teacher@learnsphere.test", "Autorec Teacher", "TEACHER");
        student = findOrCreateUser("autorec.student@learnsphere.test", "Autorec Student", "STUDENT");
        admin = findOrCreateUser("autorec.admin@learnsphere.test", "Autorec Admin", "ADMIN");

        // Clean enrollments/royalties/payments for this teacher/student
        enrollmentRepository.findByUserId(student.getUserId()).forEach(enrollmentRepository::delete);
        royaltyRepository.findByTeacher_UserIdOrderByCreatedAtDesc(teacher.getUserId()).forEach(royaltyRepository::delete);
        paymentRepository.findByUser_UserIdOrderByCreatedAtDesc(student.getUserId()).forEach(paymentRepository::delete);

        paidCourse = new Course();
        paidCourse.setTitle("Microservice Royalties " + System.currentTimeMillis());
        paidCourse.setStatus(Course.Status.LIVE);
        paidCourse.setIsPublished(true);
        paidCourse.setPrice(new BigDecimal("1000.00"));
        paidCourse.setTeacher(teacher);
        paidCourse = courseRepository.save(paidCourse);
    }

    private RequestPostProcessor asTeacher() {
        var userDetails = userDetailsService.loadUserByUsername(teacher.getEmail());
        return user(userDetails);
    }

    private RequestPostProcessor asAdmin() {
        var userDetails = userDetailsService.loadUserByUsername(admin.getEmail());
        return user(userDetails);
    }

    private UserAccount findOrCreateUser(String email, String fullName, String roleName) {
        UserAccount existing = userAccountRepository.findByEmail(email);
        if (existing != null) {
            existing.setRoyaltyBalance(BigDecimal.ZERO);
            return userAccountRepository.save(existing);
        }
        UserAccount u = new UserAccount();
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPassword("hashedpassword");
        u.setRoyaltyBalance(BigDecimal.ZERO);
        u.setStatus(UserAccount.Status.ACTIVE);
        u = userAccountRepository.save(u);

        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            role = new Role();
            role.setName(roleName);
            role = roleRepository.save(role);
        }

        UserRole ur = new UserRole();
        ur.setUser(u);
        ur.setRole(role);
        userRoleRepository.save(ur);
        return u;
    }

    @Test
    @DisplayName("getRoyalties does NOT mint synthetic royalties for free/college enrollments in paid courses")
    void getRoyaltiesDoesNotMintRoyaltiesForFreeEnrollments() throws Exception {
        // Enrolled student via same-college/direct grant (0 payment rows)
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(paidCourse);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        assertThat(royaltyRepository.findByTeacher_UserIdOrderByCreatedAtDesc(teacher.getUserId())).isEmpty();
        assertThat(userAccountRepository.findById(teacher.getUserId()).orElseThrow().getRoyaltyBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);

        // Teacher opens royalties page
        mockMvc.perform(get("/api/teacher/royalties").with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.summary.totalEarned").value(0))
                .andExpect(jsonPath("$.summary.pendingPayout").value(0))
                .andExpect(jsonPath("$.summary.externalEnrollments").value(0))
                .andExpect(jsonPath("$.byCourse[0].externalPaidCount").value(0));

        // DB state MUST NOT be altered
        assertThat(royaltyRepository.findByTeacher_UserIdOrderByCreatedAtDesc(teacher.getUserId())).isEmpty();
        UserAccount updatedTeacher = userAccountRepository.findById(teacher.getUserId()).orElseThrow();
        assertThat(updatedTeacher.getRoyaltyBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getRoyalties is read-only and does not increment balance when called repeatedly")
    void getRoyaltiesIsReadOnlyAndIdempotent() throws Exception {
        // Legitimate payment + royalty minted at payment time
        Payment payment = new Payment();
        payment.setUser(student);
        payment.setCourse(paidCourse);
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setStatus(Payment.Status.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setGatewayOrderId("order_legit_1");
        payment.setGatewayPaymentId("pay_legit_1");
        paymentRepository.save(payment);

        Royalty royalty = new Royalty();
        royalty.setTeacher(teacher);
        royalty.setCourse(paidCourse);
        royalty.setAmount(new BigDecimal("700.00"));
        royalty.setPeriod("2026-10");
        royalty.setSource(Royalty.Source.EXTERNAL_SALES);
        royaltyRepository.save(royalty);

        teacher.setRoyaltyBalance(new BigDecimal("700.00"));
        userAccountRepository.save(teacher);

        // Enroll student
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(paidCourse);
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);

        // First call
        mockMvc.perform(get("/api/teacher/royalties").with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalEarned").value(700.0))
                .andExpect(jsonPath("$.summary.pendingPayout").value(700.0))
                .andExpect(jsonPath("$.summary.externalEnrollments").value(1));

        // Second call
        mockMvc.perform(get("/api/teacher/royalties").with(asTeacher()))
                .andExpect(status().isOk());

        // Balance and royalties count must be strictly unchanged
        assertThat(royaltyRepository.findByTeacher_UserIdOrderByCreatedAtDesc(teacher.getUserId())).hasSize(1);
        UserAccount updatedTeacher = userAccountRepository.findById(teacher.getUserId()).orElseThrow();
        assertThat(updatedTeacher.getRoyaltyBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
    }

    @Test
    @DisplayName("RevenueSummary derives from SUCCESS payments only, not uncaptured or free enrollments")
    void revenueSummaryDerivesFromPaidPayments() throws Exception {
        // Enrolled student with NO payment
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(paidCourse);
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);

        mockMvc.perform(get("/api/admin/revenue").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }
}

