package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PaymentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W7-12 (audit D5, AUD-12 inspect part; BR-03, AC-03): the admin user list is
 * paginated, the tabs get server counts, and Inspect loads one account's roles,
 * recent payments and audit history from {@code GET /api/admin/users/{id}}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserInspectTests extends PostgresIntegrationTest {

    private static final String ADMIN = "admin@learnsphere.com";
    private static final String STUDENT = "student@learnsphere.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private PaymentRepository paymentRepository;

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    private Long userId(String email) {
        return userAccountRepository.findByEmail(email).getUserId();
    }

    private long count(String sql) {
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    @Test
    void listIsAPageEnvelopeOrderedById() throws Exception {
        long total = count("select count(*) from users");
        assertThat(total).as("seeded personas").isGreaterThanOrEqualTo(3);

        mockMvc.perform(get("/api/admin/users").param("page", "0").param("size", "2").with(as(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(containsString("no-store"), containsString("private"))))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.totalPages").value((int) ((total + 1) / 2)))
                .andExpect(jsonPath("$.content[0].id").value(userId("teacher@learnsphere.com")))
                .andExpect(jsonPath("$.content[0].password").doesNotExist());

        mockMvc.perform(get("/api/admin/users").param("page", "1").param("size", "2").with(as(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content[0].id").value(userId(ADMIN)));
    }

    @Test
    void listFiltersBeforePaging() throws Exception {
        long students = count("select count(*) from user_roles ur join roles r on r.role_id = ur.role_id where r.name = 'STUDENT'");

        mockMvc.perform(get("/api/admin/users").param("role", "STUDENT").with(as(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(students))
                .andExpect(jsonPath("$.content[*].role", everyItem(containsString("STUDENT"))));

        mockMvc.perform(get("/api/admin/users").param("search", "STUDENT@learn").with(as(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value(STUDENT));

        mockMvc.perform(get("/api/admin/users").param("page", "9").param("size", "20").with(as(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page").value(9));
    }

    @Test
    void countsDriveTheTabs() throws Exception {
        mockMvc.perform(put("/api/admin/users/{id}/status", userId(STUDENT))
                        .with(as(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FLAGGED\"}"))
                .andExpect(status().isOk());

        long all = count("select count(*) from users");
        long students = count("select count(distinct ur.user_id) from user_roles ur join roles r on r.role_id = ur.role_id where r.name = 'STUDENT'");
        long teachers = count("select count(distinct ur.user_id) from user_roles ur join roles r on r.role_id = ur.role_id where r.name = 'TEACHER'");
        long blacklisted = count("select count(*) from users where status = 'BLACKLISTED'");

        mockMvc.perform(get("/api/admin/users/counts").with(as(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(containsString("no-store"), containsString("private"))))
                .andExpect(jsonPath("$.all").value(all))
                .andExpect(jsonPath("$.students").value(students))
                .andExpect(jsonPath("$.teachers").value(teachers))
                .andExpect(jsonPath("$.flagged").value(1))
                .andExpect(jsonPath("$.blacklisted").value(blacklisted));
    }

    @Test
    void inspectLoadsRolesPaymentsAndAuditHistory() throws Exception {
        Long studentId = userId(STUDENT);
        UserAccount student = userAccountRepository.findById(studentId).orElseThrow();
        Course course = courseRepository.findAll().get(0);

        Payment payment = new Payment();
        payment.setUser(student);
        payment.setCourse(course);
        payment.setAmount(new BigDecimal("499.00"));
        payment.setStatus(Payment.Status.SUCCESS);
        payment.setGatewayOrderId("order_w712");
        payment.setGatewayPaymentId("pay_w712");
        payment.setPaidAt(LocalDateTime.of(2025, 1, 2, 10, 0));
        paymentRepository.save(payment);

        mockMvc.perform(put("/api/admin/users/{id}/status", studentId)
                        .with(as(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FLAGGED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/users/{id}", studentId).with(as(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(containsString("no-store"), containsString("private"))))
                .andExpect(jsonPath("$.user.id").value(studentId))
                .andExpect(jsonPath("$.user.email").value(STUDENT))
                .andExpect(jsonPath("$.user.status").value("FLAGGED"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.roles[*].roleName", hasItem("STUDENT")))
                .andExpect(jsonPath("$.roles[0].roleId").isNumber())
                .andExpect(jsonPath("$.payments[0].gatewayPaymentId").value("pay_w712"))
                .andExpect(jsonPath("$.payments[0].courseTitle").value(course.getTitle()))
                .andExpect(jsonPath("$.payments[0].amount").value(499.00))
                .andExpect(jsonPath("$.payments[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.payments[0].user").doesNotExist())
                .andExpect(jsonPath("$.audit[0].action").value("USER_STATUS_CHANGED"))
                .andExpect(jsonPath("$.audit[0].actorId").value(userId(ADMIN)))
                .andExpect(jsonPath("$.audit[0].ip").doesNotExist());
    }

    @Test
    void inspectIsAdminOnlyAndProblemJsonForUnknownIds() throws Exception {
        Long studentId = userId(STUDENT);

        mockMvc.perform(get("/api/admin/users/{id}", 999_999L).with(as(ADMIN)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(get("/api/admin/users/{id}", studentId).with(as(STUDENT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/admin/users/{id}", studentId).with(as("teacher@learnsphere.com")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users/counts").with(as(STUDENT)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users/{id}", studentId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anAdminCannotRevokeTheirOwnAdminRole() throws Exception {
        Long adminId = userId(ADMIN);
        Long adminRoleId = roleRepository.findByName("ADMIN").getRoleId();

        mockMvc.perform(delete("/api/users/{id}/roles/{roleId}", adminId, adminRoleId).with(as(ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertThat(userRoleRepository.findByUser_UserIdAndRole_RoleId(adminId, adminRoleId)).isPresent();
        assertThat(count("select count(*) from audit_log where action = 'USER_ROLE_REVOKED'")).isZero();

        mockMvc.perform(get("/api/admin/users/{id}", adminId).with(as(ADMIN)))
                .andExpect(jsonPath("$.roles[*].roleName", hasItem("ADMIN")))
                .andExpect(jsonPath("$.audit[*].action", not(hasItem("USER_ROLE_REVOKED"))));
    }
}
