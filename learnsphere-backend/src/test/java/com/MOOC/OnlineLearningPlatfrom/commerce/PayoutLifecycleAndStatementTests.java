package com.MOOC.OnlineLearningPlatfrom.commerce;

import com.MOOC.OnlineLearningPlatfrom.Dto.TeacherPayoutStatementDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.JwtService;
import com.MOOC.OnlineLearningPlatfrom.Service.AuditService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Story W6-09: Payout threshold/schedule/lock/statement
 * Rules: A-22, BR-20, [F-F18], spec §4 SCR-55
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PayoutLifecycleAndStatementTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private RoyaltyRepository royaltyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    private UserAccount admin;
    private UserAccount eligibleTeacher;
    private UserAccount subThresholdTeacher;
    private UserAccount student;

    private String adminToken;
    private String eligibleTeacherToken;
    private String studentToken;

    @BeforeEach
    void setUp() {
        Role adminRole = findOrCreateRole("ADMIN");
        Role teacherRole = findOrCreateRole("TEACHER");
        Role studentRole = findOrCreateRole("STUDENT");

        admin = findOrCreateUser("admin.payout@learnsphere.test", "Admin Payout", adminRole);
        eligibleTeacher = findOrCreateUser("eligible.teacher@learnsphere.test", "Eligible Teacher", teacherRole);
        subThresholdTeacher = findOrCreateUser("sub.teacher@learnsphere.test", "Sub Threshold Teacher", teacherRole);
        student = findOrCreateUser("student.payout@learnsphere.test", "Student Payout", studentRole);

        // Clear existing payouts for test teachers
        payoutRepository.findByTeacher_UserIdOrderByCreatedAtDesc(eligibleTeacher.getUserId()).forEach(payoutRepository::delete);
        payoutRepository.findByTeacher_UserIdOrderByCreatedAtDesc(subThresholdTeacher.getUserId()).forEach(payoutRepository::delete);

        // Clear existing royalties for test teachers
        royaltyRepository.findByTeacher_UserIdOrderByCreatedAtDesc(eligibleTeacher.getUserId()).forEach(royaltyRepository::delete);
        royaltyRepository.findByTeacher_UserIdOrderByCreatedAtDesc(subThresholdTeacher.getUserId()).forEach(royaltyRepository::delete);

        eligibleTeacher.setRoyaltyBalance(new BigDecimal("750.00"));
        userAccountRepository.save(eligibleTeacher);

        subThresholdTeacher.setRoyaltyBalance(new BigDecimal("350.00"));
        userAccountRepository.save(subThresholdTeacher);

        adminToken = jwtService.generateToken(admin.getEmail(), admin.getUserId(), List.of("ADMIN"));
        eligibleTeacherToken = jwtService.generateToken(eligibleTeacher.getEmail(), eligibleTeacher.getUserId(), List.of("TEACHER"));
        studentToken = jwtService.generateToken(student.getEmail(), student.getUserId(), List.of("STUDENT"));
    }

    private Role findOrCreateRole(String name) {
        Role r = roleRepository.findByName(name);
        if (r != null) {
            return r;
        }
        Role newRole = new Role();
        newRole.setName(name);
        return roleRepository.save(newRole);
    }

    private UserAccount findOrCreateUser(String email, String fullName, Role role) {
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

        UserRole ur = new UserRole();
        ur.setUser(u);
        ur.setRole(role);
        userRoleRepository.save(ur);
        return u;
    }

    @Test
    @DisplayName("Single payout rejects teacher whose balance is below ₹500 threshold (400 Bad Request)")
    void payOneRejectsSubThresholdTeacher() throws Exception {
        mockMvc.perform(post("/api/admin/payouts/" + subThresholdTeacher.getUserId() + "/pay")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("below the minimum payout threshold")));

        UserAccount reloaded = userAccountRepository.findById(subThresholdTeacher.getUserId()).orElseThrow();
        assertThat(reloaded.getRoyaltyBalance()).isEqualByComparingTo(new BigDecimal("350.00"));
        assertThat(payoutRepository.findByTeacher_UserIdOrderByCreatedAtDesc(subThresholdTeacher.getUserId())).isEmpty();
    }

    @Test
    @DisplayName("Single payout disburses teacher with balance >= ₹500, zeroes balance, and creates audit log")
    void payOneDisbursesEligibleTeacherAndAudits() throws Exception {
        mockMvc.perform(post("/api/admin/payouts/" + eligibleTeacher.getUserId() + "/pay")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(750.00))
                .andExpect(jsonPath("$.status").value("PAID"));

        UserAccount reloaded = userAccountRepository.findById(eligibleTeacher.getUserId()).orElseThrow();
        assertThat(reloaded.getRoyaltyBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        List<Payout> payouts = payoutRepository.findByTeacher_UserIdOrderByCreatedAtDesc(eligibleTeacher.getUserId());
        assertThat(payouts).hasSize(1);
        assertThat(payouts.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(payouts.get(0).getStatus()).isEqualTo(Payout.Status.PAID);

        // Verify audit log
        List<AuditLog> logs = auditLogRepository.findByResourceTypeAndResourceIdOrderByIdAsc(
                AuditService.RESOURCE_PAYOUT, payouts.get(0).getId());
        assertThat(logs).isNotEmpty();
        AuditLog latest = logs.get(0);
        assertThat(latest.getAction()).isEqualTo(AuditService.PAYOUT_DISBURSED);
        assertThat(latest.getResourceId()).isEqualTo(payouts.get(0).getId());
    }

    @Test
    @DisplayName("Payout is idempotent when Idempotency-Key header is supplied")
    void payOneIsIdempotentWithKey() throws Exception {
        String idemKey = "test-idem-" + System.currentTimeMillis();

        mockMvc.perform(post("/api/admin/payouts/" + eligibleTeacher.getUserId() + "/pay")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(750.00))
                .andExpect(jsonPath("$.status").value("PAID"));

        // Second request with same Idempotency-Key
        mockMvc.perform(post("/api/admin/payouts/" + eligibleTeacher.getUserId() + "/pay")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", idemKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(750.00))
                .andExpect(jsonPath("$.status").value("PAID"));

        List<Payout> payouts = payoutRepository.findByTeacher_UserIdOrderByCreatedAtDesc(eligibleTeacher.getUserId());
        assertThat(payouts).hasSize(1); // Exactly one payout recorded
    }

    @Test
    @DisplayName("Process-all only disburses teachers meeting ₹500 threshold and skips sub-threshold teachers")
    void processAllFiltersByThreshold() throws Exception {
        mockMvc.perform(post("/api/admin/payouts/process-all")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processed").value(1));

        UserAccount eligibleReloaded = userAccountRepository.findById(eligibleTeacher.getUserId()).orElseThrow();
        assertThat(eligibleReloaded.getRoyaltyBalance()).isEqualByComparingTo(BigDecimal.ZERO);

        UserAccount subReloaded = userAccountRepository.findById(subThresholdTeacher.getUserId()).orElseThrow();
        assertThat(subReloaded.getRoyaltyBalance()).isEqualByComparingTo(new BigDecimal("350.00"));
    }

    @Test
    @DisplayName("Teacher accesses own monthly statement at /api/teacher/payouts/statement with Cache-Control private, no-store")
    void teacherPayoutStatementReturnsEligibleDetails() throws Exception {
        // Seed a royalty record for current month
        String currentPeriod = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Royalty r = new Royalty();
        r.setTeacher(eligibleTeacher);
        r.setPeriod(currentPeriod);
        r.setAmount(new BigDecimal("750.00"));
        r.setSource(Royalty.Source.EXTERNAL_SALES);
        royaltyRepository.save(r);

        mockMvc.perform(get("/api/teacher/payouts/statement")
                        .header("Authorization", "Bearer " + eligibleTeacherToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")))
                .andExpect(jsonPath("$.teacherId").value(eligibleTeacher.getUserId()))
                .andExpect(jsonPath("$.currentBalance").value(750.00))
                .andExpect(jsonPath("$.minimumThreshold").value(500.00))
                .andExpect(jsonPath("$.eligibleForPayout").value(true))
                .andExpect(jsonPath("$.thisMonthEarnings").value(750.00));
    }

    @Test
    @DisplayName("Student is forbidden (403) from accessing teacher payout statement")
    void studentForbiddenFromPayoutStatement() throws Exception {
        mockMvc.perform(get("/api/teacher/payouts/statement")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }
}
