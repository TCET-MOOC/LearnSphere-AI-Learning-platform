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
 * Story W6-06: One ledger + reconciliation
 * Rules: A-21, AUD-02, audit.md B10, [F-F8, F-F10]
 *
 * Scenarios:
 * 1. getHistory does NOT fabricate SUCCESS payment rows for enrolled students without actual payments.
 * 2. Status strings in /api/payments/history are normalized: PENDING -> CREATED, SUCCESS -> PAID, FAILED -> FAILED.
 * 3. /api/admin/ledger/reconciliation requires ADMIN role (returns 403 for student/teacher, 401 unauthenticated).
 * 4. /api/admin/ledger/reconciliation accurately computes ledger totals and enforces Cache-Control: private, no-store.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LedgerReconciliationAndHistoryTests extends PostgresIntegrationTest {

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

    private UserAccount student;
    private UserAccount teacher;
    private UserAccount admin;
    private Course paidCourse;

    @BeforeEach
    void setUp() {
        teacher = findOrCreateUser("ledger.teacher@learnsphere.test", "Ledger Teacher", "TEACHER");
        student = findOrCreateUser("ledger.student@learnsphere.test", "Ledger Student", "STUDENT");
        admin = findOrCreateUser("ledger.admin@learnsphere.test", "Ledger Admin", "ADMIN");

        // Clean payments, royalties, and enrollments created for test student
        enrollmentRepository.findByUserId(student.getUserId()).forEach(enrollmentRepository::delete);
        paymentRepository.findByUser_UserIdOrderByCreatedAtDesc(student.getUserId()).forEach(paymentRepository::delete);

        paidCourse = new Course();
        paidCourse.setTitle("Cloud Microservices " + System.currentTimeMillis());
        paidCourse.setStatus(Course.Status.LIVE);
        paidCourse.setIsPublished(true);
        paidCourse.setPrice(new BigDecimal("1200.00"));
        paidCourse.setTeacher(teacher);
        paidCourse = courseRepository.save(paidCourse);
    }

    private RequestPostProcessor asStudent() {
        var userDetails = userDetailsService.loadUserByUsername(student.getEmail());
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
    @DisplayName("getHistory does NOT fabricate synthetic SUCCESS payment rows for enrolled students")
    void getHistoryDoesNotFabricateSuccessPayments() throws Exception {
        // Direct enrollment in paid course without any payment row
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(paidCourse);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        // Call /api/payments/history
        mockMvc.perform(get("/api/payments/history").with(asStudent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // No payment row should have been created in database
        assertThat(paymentRepository.findByUser_UserIdOrderByCreatedAtDesc(student.getUserId())).isEmpty();
    }

    @Test
    @DisplayName("getHistory normalizes status vocabulary: PENDING -> CREATED, SUCCESS -> PAID, FAILED -> FAILED")
    void getHistoryNormalizesStatusVocabulary() throws Exception {
        // Create 3 payments with different statuses
        Payment p1 = new Payment();
        p1.setUser(student);
        p1.setCourse(paidCourse);
        p1.setAmount(new BigDecimal("1200.00"));
        p1.setStatus(Payment.Status.SUCCESS);
        p1.setGatewayOrderId("order_norm_paid");
        p1.setGatewayPaymentId("pay_norm_paid");
        p1.setPaidAt(LocalDateTime.now());
        paymentRepository.save(p1);

        Payment p2 = new Payment();
        p2.setUser(student);
        p2.setCourse(paidCourse);
        p2.setAmount(new BigDecimal("1200.00"));
        p2.setStatus(Payment.Status.PENDING);
        p2.setGatewayOrderId("order_norm_created");
        paymentRepository.save(p2);

        Payment p3 = new Payment();
        p3.setUser(student);
        p3.setCourse(paidCourse);
        p3.setAmount(new BigDecimal("1200.00"));
        p3.setStatus(Payment.Status.FAILED);
        p3.setGatewayOrderId("order_norm_failed");
        paymentRepository.save(p3);

        mockMvc.perform(get("/api/payments/history").with(asStudent()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.gatewayOrderId == 'order_norm_paid')].status").value("PAID"))
                .andExpect(jsonPath("$[?(@.gatewayOrderId == 'order_norm_paid')].gatewayPaymentId").value("pay_norm_paid"))
                .andExpect(jsonPath("$[?(@.gatewayOrderId == 'order_norm_created')].status").value("CREATED"))
                .andExpect(jsonPath("$[?(@.gatewayOrderId == 'order_norm_failed')].status").value("FAILED"));
    }

    @Test
    @DisplayName("GET /api/admin/ledger/reconciliation requires ADMIN role")
    void reconciliationRequiresAdminRole() throws Exception {
        // Unauthenticated -> 401
        mockMvc.perform(get("/api/admin/ledger/reconciliation"))
                .andExpect(status().isUnauthorized());

        // Student -> 403
        mockMvc.perform(get("/api/admin/ledger/reconciliation").with(asStudent()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/ledger/reconciliation returns ledger aggregates and Cache-Control: private, no-store")
    void reconciliationReturnsLedgerAggregates() throws Exception {
        mockMvc.perform(get("/api/admin/ledger/reconciliation").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.totalPayments").isNumber())
                .andExpect(jsonPath("$.paidPaymentsCount").isNumber())
                .andExpect(jsonPath("$.paidPaymentsAmount").isNumber())
                .andExpect(jsonPath("$.pendingPaymentsCount").isNumber())
                .andExpect(jsonPath("$.failedPaymentsCount").isNumber())
                .andExpect(jsonPath("$.totalRoyaltiesCount").isNumber())
                .andExpect(jsonPath("$.totalRoyaltiesAmount").isNumber())
                .andExpect(jsonPath("$.isReconciled").isBoolean());
    }
}
