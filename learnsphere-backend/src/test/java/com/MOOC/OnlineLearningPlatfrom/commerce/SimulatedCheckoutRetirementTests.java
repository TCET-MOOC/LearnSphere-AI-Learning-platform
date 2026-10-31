package com.MOOC.OnlineLearningPlatfrom.commerce;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Story W6-01: Remove simulated checkout rail
 * Rules: A-06, audit.md B10, E3 (AUD-06), [F-F1], NFR-03
 *
 * Scenarios:
 * 1. POST /api/payments/checkout is disabled -> returns 400 BAD_REQUEST stating simulated rail is retired.
 * 2. POST /api/payments/verify is disabled -> returns 400 BAD_REQUEST stating simulated rail is retired.
 * 3. Unauthenticated requests to /api/payments/checkout or /verify -> return 401 UNAUTHORIZED.
 * 4. GET /api/payments/status remains active and enforces Cache-Control: private, no-store.
 * 5. GET /api/payments/history remains active and enforces Cache-Control: private, no-store.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SimulatedCheckoutRetirementTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private UserAccount student;
    private Course paidCourse;

    @BeforeEach
    void setUp() {
        student = findOrCreateUser("checkout.student@learnsphere.com", "Checkout Student", "STUDENT");
        UserAccount teacher = findOrCreateUser("checkout.teacher@learnsphere.com", "Checkout Teacher", "TEACHER");

        paidCourse = new Course();
        paidCourse.setTitle("Distributed Systems Architecture");
        paidCourse.setTeacher(teacher);
        paidCourse.setPrice(BigDecimal.valueOf(1499));
        paidCourse.setIsPublished(true);
        paidCourse = courseRepository.save(paidCourse);
    }

    private RequestPostProcessor asStudent() {
        var userDetails = userDetailsService.loadUserByUsername(student.getEmail());
        return user(userDetails);
    }

    private UserAccount findOrCreateUser(String email, String name, String roleName) {
        UserAccount existing = userAccountRepository.findByEmail(email);
        if (existing != null) {
            return existing;
        }
        UserAccount u = new UserAccount();
        u.setEmail(email);
        u.setFullName(name);
        u.setPassword("hashedpassword");
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
    @DisplayName("Scenario 1: POST /api/payments/checkout returns 400 stating simulated rail is retired")
    void simulatedCheckoutReturns400Retired() throws Exception {
        mockMvc.perform(post("/api/payments/checkout")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseId", paidCourse.getId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("retired")));
    }

    @Test
    @DisplayName("Scenario 2: POST /api/payments/verify returns 400 stating simulated rail is retired")
    void simulatedVerifyReturns400Retired() throws Exception {
        mockMvc.perform(post("/api/payments/verify")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("orderId", "order_sim_12345"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("retired")));
    }

    @Test
    @DisplayName("Scenario 3: Unauthenticated requests return 401 UNAUTHORIZED")
    void unauthenticatedCheckoutAndVerifyReturn401() throws Exception {
        mockMvc.perform(post("/api/payments/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("courseId", paidCourse.getId()))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("orderId", "order_sim_12345"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario 4: GET /api/payments/status remains active and enforces private, no-store")
    void paymentStatusEnforcesPrivateNoStore() throws Exception {
        mockMvc.perform(get("/api/payments/status")
                        .with(asStudent())
                        .param("courseId", paidCourse.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.paid").isBoolean());
    }

    @Test
    @DisplayName("Scenario 5: GET /api/payments/history remains active and enforces private, no-store")
    void paymentHistoryEnforcesPrivateNoStore() throws Exception {
        mockMvc.perform(get("/api/payments/history")
                        .with(asStudent()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }
}
