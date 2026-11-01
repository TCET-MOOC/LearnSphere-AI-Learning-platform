package com.MOOC.OnlineLearningPlatfrom.commerce;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W6-12: Commerce / Security Regression Suite
 *
 * Validates every security invariant introduced across W6-01…W6-11:
 *  - Tamper defence (body amount ignored, server price authoritative)
 *  - Auth rejection on /internal/enroll
 *  - Idempotency (no duplicate payments / royalties / enrolments on replay)
 *  - No fabricated history rows
 *  - Status normalisation  PENDING→CREATED, SUCCESS→PAID, FAILED→FAILED
 *  - Failed attempts preserved after a later success
 *  - Receipt fields completeness  (gatewayOrderId, gatewayPaymentId, paidAt …)
 *  - IDOR stealth-404 on /api/payments/history
 *  - Cache-Control: private, no-store on all per-user/payment/entitlement responses
 *  - Same-college free enrolment leaves zero payment rows
 *  - Payout sub-threshold rejection (< ₹500)
 *  - Checkout retired
 *  - Verify retired
 *  - Royalty credited at 70% of server-side course price (not body amount)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CommerceSecuritySuiteTests extends PostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CustomUserDetailsService userDetailsService;

    @Autowired private CourseRepository courseRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private RoyaltyRepository royaltyRepository;
    @Autowired private CollegeRepository collegeRepository;
    @Autowired private CollegeEntitlementRepository collegeEntitlementRepository;
    @Autowired private com.MOOC.OnlineLearningPlatfrom.Service.CollegeEntitlementService collegeEntitlementService;
    @Autowired private PayoutRepository payoutRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private com.MOOC.OnlineLearningPlatfrom.Security.JwtService jwtService;

    @Value("${internal.api.key}")
    private String internalApiKey;

    private UserAccount teacher;
    private UserAccount student;
    private UserAccount anotherStudent;
    private Course paidCourse;

    private static final BigDecimal COURSE_PRICE = new BigDecimal("2500.00");

    @BeforeEach
    void setUp() {
        // Clean slate for every test
        payoutRepository.deleteAll();
        royaltyRepository.deleteAll();
        paymentRepository.deleteAll();
        collegeEntitlementRepository.deleteAll();
        enrollmentRepository.findAll().forEach(e -> {
            if (e.getRole() != null && e.getRole().equals("STUDENT")) {
                enrollmentRepository.delete(e);
            }
        });

        teacher = findOrCreate("teacher.suite@learnsphere.test", "Suite Teacher");
        teacher.setRoyaltyBalance(BigDecimal.ZERO);
        teacher.setCollege(null);   // clear any college set by prior tests
        teacher = userAccountRepository.save(teacher);

        student = findOrCreate("student.suite@learnsphere.test", "Suite Student");
        student.setCollege(null);
        student = userAccountRepository.save(student);
        anotherStudent = findOrCreate("student.suite2@learnsphere.test", "Suite Student 2");
        anotherStudent.setCollege(null);
        anotherStudent = userAccountRepository.save(anotherStudent);

        paidCourse = new Course();
        paidCourse.setTitle("Commerce Security Suite Course " + System.currentTimeMillis());
        paidCourse.setStatus(Course.Status.LIVE);
        paidCourse.setIsPublished(true);
        paidCourse.setPrice(COURSE_PRICE);
        paidCourse.setTeacher(teacher);
        paidCourse = courseRepository.save(paidCourse);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Amount Tamper Defence
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-01: Body amount is ignored; payment recorded at server course price")
    void amountTamperDefence_bodyAmountIgnored() throws Exception {
        String payload = buildEnrollPayload(student.getUserId(), paidCourse.getId(),
                "order_css01", "pay_css01", "1.00");

        mockMvc.perform(post("/internal/enroll")
                        .header("X-Internal-Key", internalApiKey)
                        .header("X-Service-Id", "payments-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getAmount()).isEqualByComparingTo(COURSE_PRICE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Internal Auth Rejection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-02: /internal/enroll without valid X-Internal-Key+X-Service-Id returns 401")
    void internalEnroll_noHeaders_returns401() throws Exception {
        String payload = buildEnrollPayload(student.getUserId(), paidCourse.getId(),
                "order_css02", "pay_css02", null);

        // No headers at all
        mockMvc.perform(post("/internal/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        // Only key, missing X-Service-Id
        mockMvc.perform(post("/internal/enroll")
                        .header("X-Internal-Key", internalApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());

        // Wrong key
        mockMvc.perform(post("/internal/enroll")
                        .header("X-Internal-Key", "wrong-key")
                        .header("X-Service-Id", "payments-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Replay / Idempotency
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-03: Replaying /internal/enroll with same paymentId mints exactly one payment, royalty and enrolment")
    void replay_idempotencyPreventsDuplicates() throws Exception {
        String payload = buildEnrollPayload(student.getUserId(), paidCourse.getId(),
                "order_css03", "pay_css03", null);

        // First call
        mockMvc.perform(post("/internal/enroll")
                        .header("X-Internal-Key", internalApiKey)
                        .header("X-Service-Id", "payments-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // Second call (replay)
        MvcResult result = mockMvc.perform(post("/internal/enroll")
                        .header("X-Internal-Key", internalApiKey)
                        .header("X-Service-Id", "payments-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).containsIgnoringCase("ALREADY_PROCESSED");

        // Exactly one payment, one royalty, one enrolment
        long payCount = paymentRepository.findAll().stream()
                .filter(p -> "pay_css03".equals(p.getGatewayPaymentId())).count();
        assertThat(payCount).isEqualTo(1);

        assertThat(royaltyRepository.findByTeacher_UserIdOrderByCreatedAtDesc(teacher.getUserId())).hasSize(1);
        assertThat(enrollmentRepository.findByUserId(student.getUserId())).hasSize(1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. No Fabricated History Rows
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-04: Student enrolled without any payment row → history returns empty list (no fabrication)")
    void historyNotFabricated_whenNoPaymentRow() throws Exception {
        // Directly enroll student via enrollment repo (bypass payment flow)
        Enrollment e = new Enrollment();
        e.setUserId(student.getUserId());
        e.setCourse(paidCourse);
        e.setRole("STUDENT");
        enrollmentRepository.save(e);

        var userDetails = (org.springframework.security.core.userdetails.UserDetails)
                userDetailsService.loadUserByUsername(student.getEmail());

        MvcResult result = mockMvc.perform(get("/api/payments/history")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Status Mapping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-05: Status mapping — PENDING→CREATED, SUCCESS→PAID, FAILED→FAILED in /api/payments/history")
    void statusMapping_normalizedCorrectly() throws Exception {
        savePayment(student, paidCourse, Payment.Status.PENDING, "order_pend", "pay_pend");
        savePayment(student, paidCourse, Payment.Status.SUCCESS, "order_succ", "pay_succ");
        savePayment(student, paidCourse, Payment.Status.FAILED,  "order_fail", "pay_fail");

        var userDetails = (org.springframework.security.core.userdetails.UserDetails)
                userDetailsService.loadUserByUsername(student.getEmail());

        MvcResult result = mockMvc.perform(get("/api/payments/history")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(3);

        boolean foundCreated = false, foundPaid = false, foundFailed = false;
        for (JsonNode node : arr) {
            String status = node.path("status").asText();
            String gatewayPaymentId = node.path("gatewayPaymentId").asText();
            switch (gatewayPaymentId) {
                case "pay_pend" -> { assertThat(status).isEqualTo("CREATED"); foundCreated = true; }
                case "pay_succ" -> { assertThat(status).isEqualTo("PAID");    foundPaid    = true; }
                case "pay_fail" -> { assertThat(status).isEqualTo("FAILED");  foundFailed  = true; }
            }
        }
        assertThat(foundCreated).isTrue();
        assertThat(foundPaid).isTrue();
        assertThat(foundFailed).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Failed Attempt Preserved After Later Success
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-06: Failed payment attempt is preserved in history even after a subsequent success for the same course")
    void failedAttemptPreserved_afterLaterSuccess() throws Exception {
        savePayment(student, paidCourse, Payment.Status.FAILED,  "order_f_first", "pay_f_first");
        savePayment(student, paidCourse, Payment.Status.SUCCESS, "order_s_after", "pay_s_after");

        var userDetails = (org.springframework.security.core.userdetails.UserDetails)
                userDetailsService.loadUserByUsername(student.getEmail());

        MvcResult result = mockMvc.perform(get("/api/payments/history")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        // Both rows must be present
        assertThat(arr.size()).isGreaterThanOrEqualTo(2);

        boolean hasFailed = false, hasPaid = false;
        for (JsonNode node : arr) {
            String status = node.path("status").asText();
            if ("FAILED".equals(status)) hasFailed = true;
            if ("PAID".equals(status))   hasPaid   = true;
        }
        assertThat(hasFailed).withFailMessage("FAILED row must be preserved in history").isTrue();
        assertThat(hasPaid).withFailMessage("PAID row must also appear in history").isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Receipt Fields Completeness
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-07: PAID payment in history has gatewayOrderId, gatewayPaymentId, paidAt, amount, currency, status=PAID")
    void receiptFields_allPresentForPaidPayment() throws Exception {
        savePayment(student, paidCourse, Payment.Status.SUCCESS, "order_receipt_01", "pay_receipt_01");

        var userDetails = (org.springframework.security.core.userdetails.UserDetails)
                userDetailsService.loadUserByUsername(student.getEmail());

        MvcResult result = mockMvc.perform(get("/api/payments/history")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isGreaterThanOrEqualTo(1);

        JsonNode row = arr.get(0);
        assertThat(row.path("gatewayOrderId").asText()).isEqualTo("order_receipt_01");
        assertThat(row.path("gatewayPaymentId").asText()).isEqualTo("pay_receipt_01");
        assertThat(row.path("status").asText()).isEqualTo("PAID");
        assertThat(row.path("amount").decimalValue()).isEqualByComparingTo(COURSE_PRICE);
        assertThat(row.path("currency").asText()).isNotBlank();
        assertThat(row.path("paidAt").isMissingNode()).isFalse();
        assertThat(row.path("paidAt").isNull()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 8. IDOR Stealth 404 — /api/payments/history is owner-scoped
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-08: GET /api/payments/history is owner-scoped — another user sees only their own (empty) list, not victim's payments")
    void historyIdor_anotherUserSeesOnlyOwnPayments() throws Exception {
        // student has a payment
        savePayment(student, paidCourse, Payment.Status.SUCCESS, "order_idor", "pay_idor");

        // anotherStudent has NO payments
        var otherDetails = (org.springframework.security.core.userdetails.UserDetails)
                userDetailsService.loadUserByUsername(anotherStudent.getEmail());

        MvcResult result = mockMvc.perform(get("/api/payments/history")
                        .with(user(otherDetails)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        // anotherStudent must see 0 payments, never student's payment
        assertThat(arr.size()).isEqualTo(0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 9. Cache-Control: private, no-store on per-user/payment responses
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-09a: GET /api/payments/history has Cache-Control: private, no-store")
    void cacheControl_paymentsHistory() throws Exception {
        var userDetails = (org.springframework.security.core.userdetails.UserDetails)
                userDetailsService.loadUserByUsername(student.getEmail());

        mockMvc.perform(get("/api/payments/history").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
    }

    @Test
    @DisplayName("CSS-09b: GET /api/payments/status has Cache-Control: private, no-store")
    void cacheControl_paymentsStatus() throws Exception {
        var userDetails = (org.springframework.security.core.userdetails.UserDetails)
                userDetailsService.loadUserByUsername(student.getEmail());

        mockMvc.perform(get("/api/payments/status")
                        .param("courseId", paidCourse.getId().toString())
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
    }

    @Test
    @DisplayName("CSS-09c: GET /api/courses/{id}/entitlement has Cache-Control: private, no-store")
    void cacheControl_entitlementCheck() throws Exception {
        var userDetails = (org.springframework.security.core.userdetails.UserDetails)
                userDetailsService.loadUserByUsername(student.getEmail());

        mockMvc.perform(get("/api/courses/{id}/entitlement", paidCourse.getId())
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
    }

    @Test
    @DisplayName("CSS-09d: GET /api/admin/ledger/reconciliation has Cache-Control: private, no-store")
    void cacheControl_ledgerReconciliation() throws Exception {
        // Create admin user
        UserAccount admin = findOrCreate("admin.suite@learnsphere.test", "Suite Admin");

        // Need ADMIN role for this endpoint
        var adminDetails = userDetailsService.loadUserByUsername(admin.getEmail());

        // The endpoint requires ADMIN role; test that it at least sends cache header
        // (Spring Security will 403 if no ADMIN role — test the pattern via MockMvc with role override)
        mockMvc.perform(get("/api/admin/ledger/reconciliation")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin.suite@learnsphere.test").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
    }

    @Test
    @DisplayName("CSS-09e: GET /api/teacher/payouts/statement has Cache-Control: private, no-store")
    void cacheControl_teacherPayoutStatement() throws Exception {
        // Construct CustomUserDetails with TEACHER role so security check passes
        CustomUserDetails teacherPrincipal = new CustomUserDetails(teacher, List.of("TEACHER"));

        mockMvc.perform(get("/api/teacher/payouts/statement")
                        .with(user(teacherPrincipal)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 10. Same-College Free Enrolment Leaves Zero Payment Rows
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-10: Same-college free enrolment creates zero payment rows")
    void sameCollegeEntitlement_noPaymentRow() throws Exception {
        // Set up a verified college and assign BOTH teacher and student to it
        College college = new College();
        college.setName("Test College CSS10 " + System.currentTimeMillis());
        college.setVerificationStatus(College.VerificationStatus.VERIFIED);
        college = collegeRepository.save(college);

        teacher.setCollege(college);
        teacher = userAccountRepository.save(teacher);

        student.setCollege(college);
        student = userAccountRepository.save(student);

        // Grant entitlement via service (uses isSameVerifiedCollege internally)
        collegeEntitlementService.grantSameCollegeEntitlement(student.getUserId(), paidCourse.getId());
        // Directly enroll (as the entitled path does — no payment row created)
        Enrollment e = new Enrollment();
        e.setUserId(student.getUserId());
        e.setCourse(paidCourse);
        e.setRole("STUDENT");
        enrollmentRepository.save(e);

        // History must be empty — no payment was fabricated
        var userDetails = (org.springframework.security.core.userdetails.UserDetails)
                userDetailsService.loadUserByUsername(student.getEmail());

        MvcResult result = mockMvc.perform(get("/api/payments/history")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(arr.isArray()).isTrue();
        assertThat(arr.size()).isEqualTo(0);

        // Also verify payment table has 0 rows for this student
        long payCount = paymentRepository.findAll().stream()
                .filter(p -> p.getUser() != null && p.getUser().getUserId().equals(student.getUserId()))
                .count();
        assertThat(payCount).isEqualTo(0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 11. Payout Sub-Threshold Rejection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-11: POST /api/admin/payouts/{teacherId}/pay with balance < ₹500 returns 400")
    void payoutSubThreshold_returns400() throws Exception {
        // Teacher has ₹100 balance — below ₹500 threshold
        teacher.setRoyaltyBalance(new BigDecimal("100.00"));
        userAccountRepository.save(teacher);

        mockMvc.perform(post("/api/admin/payouts/{teacherId}/pay", teacher.getUserId())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin.suite@learnsphere.test").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 12. Checkout Retired
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-12: POST /api/payments/checkout returns 400 (retired rail)")
    void checkoutRetired_returns400() throws Exception {
        var userDetails = (org.springframework.security.core.userdetails.UserDetails)
                userDetailsService.loadUserByUsername(student.getEmail());

        mockMvc.perform(post("/api/payments/checkout")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\": " + paidCourse.getId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsStringIgnoringCase("retired")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 13. Verify Retired
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-13: POST /api/payments/verify returns 400 (retired rail)")
    void verifyRetired_returns400() throws Exception {
        var userDetails = (org.springframework.security.core.userdetails.UserDetails)
                userDetailsService.loadUserByUsername(student.getEmail());

        mockMvc.perform(post("/api/payments/verify")
                        .with(user(userDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"x\",\"paymentId\":\"y\",\"signature\":\"z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsStringIgnoringCase("retired")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 14. Royalty credited at 70% of server-side course price (not body amount)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CSS-14: Royalty is exactly 70% of server course price regardless of body amount")
    void royalty_creditedAt70PercentOfServerPrice() throws Exception {
        // Send a tampered amount of ₹1 — royalty must still be 70% of ₹2500 = ₹1750
        String payload = buildEnrollPayload(student.getUserId(), paidCourse.getId(),
                "order_css14", "pay_css14", "1.00");

        mockMvc.perform(post("/internal/enroll")
                        .header("X-Internal-Key", internalApiKey)
                        .header("X-Service-Id", "payments-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        List<Royalty> royalties = royaltyRepository.findByTeacher_UserIdOrderByCreatedAtDesc(teacher.getUserId());
        assertThat(royalties).hasSize(1);
        // 70% of 2500 = 1750
        assertThat(royalties.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1750.00"));

        UserAccount updatedTeacher = userAccountRepository.findById(teacher.getUserId()).orElseThrow();
        assertThat(updatedTeacher.getRoyaltyBalance()).isEqualByComparingTo(new BigDecimal("1750.00"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 15–16. Payout lock under concurrency (A-22, BR-20)
    // ─────────────────────────────────────────────────────────────────────────

    private static final int PAYOUT_RACERS = 8;

    @Test
    @DisplayName("CSS-15: Concurrent payouts with distinct Idempotency-Keys disburse the balance exactly once")
    void concurrentPayouts_distinctKeys_disburseOnce() throws Exception {
        teacher.setRoyaltyBalance(new BigDecimal("900.00"));
        userAccountRepository.save(teacher);
        String adminToken = adminToken();

        List<int[]> statuses = racePayouts(adminToken, i -> "css15-" + i + "-" + System.nanoTime());

        List<Payout> payouts = payoutRepository.findByTeacher_UserIdOrderByCreatedAtDesc(teacher.getUserId());
        assertThat(payouts).withFailMessage("double-pay: %s payout rows", payouts.size()).hasSize(1);
        assertThat(payouts.get(0).getAmount()).isEqualByComparingTo("900.00");
        assertThat(userAccountRepository.findById(teacher.getUserId()).orElseThrow().getRoyaltyBalance())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(statuses.stream().filter(s -> s[0] == 200).count()).isEqualTo(1);
        assertThat(statuses.stream().filter(s -> s[0] != 200).allMatch(s -> s[0] == 400)).isTrue();
    }

    @Test
    @DisplayName("CSS-16: Concurrent replays of one Idempotency-Key all return 200 with the same single payout")
    void concurrentPayouts_sameKey_allReturnTheOnePayout() throws Exception {
        teacher.setRoyaltyBalance(new BigDecimal("900.00"));
        userAccountRepository.save(teacher);
        String adminToken = adminToken();
        String key = "css16-" + System.nanoTime();

        List<int[]> statuses = racePayouts(adminToken, i -> key);

        List<Payout> payouts = payoutRepository.findByTeacher_UserIdOrderByCreatedAtDesc(teacher.getUserId());
        assertThat(payouts).hasSize(1);
        assertThat(payouts.get(0).getAmount()).isEqualByComparingTo("900.00");
        assertThat(statuses).withFailMessage("replay of key %s was not idempotent: statuses %s", key,
                        statuses.stream().map(s -> s[0]).toList())
                .allMatch(s -> s[0] == 200);
    }

    /** Fires PAYOUT_RACERS simultaneous POST /pay requests; returns [status] per request. */
    private List<int[]> racePayouts(String adminToken, java.util.function.IntFunction<String> keyFor) throws Exception {
        var pool = java.util.concurrent.Executors.newFixedThreadPool(PAYOUT_RACERS);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            List<java.util.concurrent.Future<int[]>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < PAYOUT_RACERS; i++) {
                String key = keyFor.apply(i);
                futures.add(pool.submit(() -> {
                    start.await();
                    MvcResult r = mockMvc.perform(post("/api/admin/payouts/{teacherId}/pay", teacher.getUserId())
                                    .header("Authorization", "Bearer " + adminToken)
                                    .header("Idempotency-Key", key))
                            .andReturn();
                    return new int[]{r.getResponse().getStatus()};
                }));
            }
            start.countDown();
            List<int[]> out = new java.util.ArrayList<>();
            for (var f : futures) out.add(f.get(60, java.util.concurrent.TimeUnit.SECONDS));
            return out;
        } finally {
            pool.shutdownNow();
        }
    }

    private String adminToken() {
        Role adminRole = roleRepository.findByName("ADMIN");
        UserAccount admin = findOrCreate("admin.suite@learnsphere.test", "Suite Admin");
        admin.setStatus(UserAccount.Status.ACTIVE);
        admin = userAccountRepository.save(admin);
        Long adminId = admin.getUserId();
        boolean hasRole = userRoleRepository.findAll().stream()
                .anyMatch(ur -> ur.getUser().getUserId().equals(adminId) && "ADMIN".equals(ur.getRole().getName()));
        if (!hasRole) {
            UserRole ur = new UserRole();
            ur.setUser(admin);
            ur.setRole(adminRole);
            userRoleRepository.save(ur);
        }
        return jwtService.generateToken(admin.getEmail(), adminId, List.of("ADMIN"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private UserAccount findOrCreate(String email, String name) {
        UserAccount u = userAccountRepository.findByEmail(email);
        if (u != null) return u;
        u = new UserAccount();
        u.setEmail(email);
        u.setFullName(name);
        u.setPassword("hash");
        u.setRoyaltyBalance(BigDecimal.ZERO);
        return userAccountRepository.save(u);
    }

    private Payment savePayment(UserAccount user, Course course, Payment.Status status,
                                String orderId, String paymentId) {
        Payment p = new Payment();
        p.setUser(user);
        p.setCourse(course);
        p.setAmount(COURSE_PRICE);
        p.setCurrency("INR");
        p.setStatus(status);
        p.setGatewayOrderId(orderId);
        p.setGatewayPaymentId(paymentId);
        if (status == Payment.Status.SUCCESS) {
            p.setPaidAt(LocalDateTime.now());
        }
        return paymentRepository.save(p);
    }

    private String buildEnrollPayload(Long userId, Long courseId,
                                       String orderId, String paymentId, String amount) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"userId\":").append(userId).append(",");
        sb.append("\"courseId\":").append(courseId).append(",");
        sb.append("\"orderId\":\"").append(orderId).append("\",");
        sb.append("\"paymentId\":\"").append(paymentId).append("\"");
        if (amount != null) {
            sb.append(",\"amount\":").append(amount);
        }
        sb.append("}");
        return sb.toString();
    }
}
