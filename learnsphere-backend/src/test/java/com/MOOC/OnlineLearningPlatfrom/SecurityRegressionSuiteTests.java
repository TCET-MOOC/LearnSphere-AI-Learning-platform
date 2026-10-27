package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Dto.AuthDtos.LoginRequest;
import com.MOOC.OnlineLearningPlatfrom.Dto.AuthDtos.RegisterRequest;
import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.Security.JwtService;
import com.MOOC.OnlineLearningPlatfrom.Security.WebSocketAuthInterceptor;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W1-10: Comprehensive Auth/IDOR Regression Suite.
 *
 * Validates the core security guarantees landed across Wave 1:
 * 1. Auth/profile responses carry Cache-Control: private, no-store (NFR-03).
 * 2. Login, refresh token rotation, and replay attack family revocation.
 * 3. Role matrix authorization (Student vs Teacher vs Admin boundaries).
 * 4. IDOR defense: foreign resource mutations return 404 (not 403) to prevent resource existence disclosure.
 * 5. Legacy /api/users/** endpoints locked down and retired.
 * 6. WebSocket SUBSCRIBE destination authorization.
 * 7. Account lifecycle status enforcement (FLAGGED read-only, BLACKLISTED login denial).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SecurityRegressionSuiteTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Autowired
    private JwtService jwtService;

    // -------------------------------------------------------------------------
    // 1. Auth & Profile Responses Carry Cache-Control: private, no-store
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Auth endpoints (login, register, refresh, logout, me) return Cache-Control: private, no-store")
    void authEndpoints_returnPrivateNoStoreHeaders() throws Exception {
        UserAccount student = createTestUser("cache_student@learnsphere.com", "ACTIVE", "STUDENT");

        // 1. Login
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(student.getEmail());
        loginReq.setPassword("TestPass1!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(containsString("private"), containsString("no-store"))))
                .andExpect(cookie().exists("refreshToken"))
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String accessToken = jsonNode.get("token").asText();
        Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");

        // 2. Auth /me
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(containsString("private"), containsString("no-store"))));

        // 3. User /me
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(containsString("private"), containsString("no-store"))));

        // 4. Refresh
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(containsString("private"), containsString("no-store"))));

        // 5. Logout
        mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(containsString("private"), containsString("no-store"))));

        // 6. Register
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setFullName("Cache Reg User");
        registerReq.setEmail("cache_reg_user@learnsphere.com");
        registerReq.setPassword("StrongPass123!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(containsString("private"), containsString("no-store"))));
    }

    // -------------------------------------------------------------------------
    // 2. Refresh Token Rotation and Replay Family Revocation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Refresh token rotation issues new token; replay of rotated token revokes entire family")
    void refreshTokens_rotationAndReplayFamilyRevocation() throws Exception {
        UserAccount student = createTestUser("replay_student@learnsphere.com", "ACTIVE", "STUDENT");

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(student.getEmail());
        loginReq.setPassword("TestPass1!");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie originalCookie = loginResult.getResponse().getCookie("refreshToken");
        assertThat(originalCookie).isNotNull();
        String token1 = originalCookie.getValue();

        // Perform legitimate rotation (token1 -> token2)
        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(originalCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Cookie rotatedCookie = refreshResult.getResponse().getCookie("refreshToken");
        assertThat(rotatedCookie).isNotNull();
        String token2 = rotatedCookie.getValue();
        assertThat(token2).isNotEqualTo(token1);

        // Replay attack: Attacker re-uses token1
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", token1))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // Now victim's token2 must also be rejected because the family was revoked!
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(rotatedCookie)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // 3. Role Matrix Authorization
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Role matrix: Students and Teachers are denied from Admin endpoints with 403; unauthenticated with 401")
    void roleMatrix_enforcesStrictPermissions() throws Exception {
        UserAccount student = createTestUser("matrix_student@learnsphere.com", "ACTIVE", "STUDENT");
        UserAccount teacher = createTestUser("matrix_teacher@learnsphere.com", "ACTIVE", "TEACHER");

        // 1. Unauthenticated access -> 401
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized());

        // 2. Student accessing admin endpoints -> 403
        mockMvc.perform(get("/api/admin/users")
                        .with(principal(student.getEmail())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/audit")
                        .with(principal(student.getEmail())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/roles")
                        .with(principal(student.getEmail())))
                .andExpect(status().isForbidden());

        // 3. Student attempting to author a course -> 403
        mockMvc.perform(post("/api/courses")
                        .with(principal(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked Course\",\"description\":\"Desc\"}"))
                .andExpect(status().isForbidden());

        // 4. Teacher accessing admin endpoints -> 403
        mockMvc.perform(get("/api/admin/users")
                        .with(principal(teacher.getEmail())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/audit")
                        .with(principal(teacher.getEmail())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/roles")
                        .with(principal(teacher.getEmail())))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // 4. IDOR Defense: Foreign Resource ID Access Returns 404 (Not 403)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("IDOR protection: Foreign curriculum and attempt mutations return 404 (not 403) to prevent probe leakage")
    void foreignResourceId_returns404Not403() throws Exception {
        UserAccount teacher1 = createTestUser("idor_t1@learnsphere.com", "ACTIVE", "TEACHER");
        UserAccount teacher2 = createTestUser("idor_t2@learnsphere.com", "ACTIVE", "TEACHER");
        UserAccount student1 = createTestUser("idor_s1@learnsphere.com", "ACTIVE", "STUDENT");
        UserAccount student2 = createTestUser("idor_s2@learnsphere.com", "ACTIVE", "STUDENT");

        // Teacher 1 creates a course
        Course course1 = new Course();
        course1.setTitle("Teacher 1 Course");
        course1.setDescription("Desc");
        course1.setTeacher(teacher1);
        course1.setPrice(BigDecimal.valueOf(19.99));
        course1 = courseRepository.save(course1);

        // Module in course 1
        com.MOOC.OnlineLearningPlatfrom.Entity.Module module1 = new com.MOOC.OnlineLearningPlatfrom.Entity.Module();
        module1.setTitle("Module 1");
        module1.setCourse(course1);
        module1.setPosition(1);
        module1 = moduleRepository.save(module1);

        // Test in course 1
        com.MOOC.OnlineLearningPlatfrom.Entity.Test test1 = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test1.setTitle("Exam 1");
        test1.setCourse(course1);
        test1.setDurationMinutes(30);
        test1 = testRepository.save(test1);

        // Student 1 attempt on test 1
        TestAttempt attempt1 = new TestAttempt();
        attempt1.setUserId(student1.getUserId());
        attempt1.setTest(test1);
        attempt1.setAttemptedAt(LocalDateTime.now());
        attempt1.setStatus("IN_PROGRESS");
        attempt1 = testAttemptRepository.save(attempt1);

        // Foreign teacher 2 attempts to mutate teacher 1's course -> must return 404
        mockMvc.perform(put("/api/courses/" + course1.getId())
                        .with(principal(teacher2.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hijacked Course Title\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Foreign teacher 2 attempts to mutate module 1 -> must return 404
        mockMvc.perform(put("/api/modules/" + module1.getModuleId())
                        .with(principal(teacher2.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hijacked Module\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Foreign student 2 attempts to read or complete student 1's test attempt -> must return 404
        mockMvc.perform(get("/api/attempts/" + attempt1.getAttemptId())
                        .with(principal(student2.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(post("/api/attempts/" + attempt1.getAttemptId() + "/submit")
                        .with(principal(student2.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // 5. Legacy User API Denial
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Legacy /api/users/** endpoints are retired/denied; only /users/me is self-service")
    void legacyUserApi_deniesUnsafeEndpoints() throws Exception {
        UserAccount student = createTestUser("legacy_s@learnsphere.com", "ACTIVE", "STUDENT");

        // Retired endpoints return 404 or 405
        mockMvc.perform(get("/api/users").with(principal(student.getEmail())))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/users")
                        .with(principal(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"fake@example.com\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/users/" + student.getUserId())
                        .with(principal(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated\"}"))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(delete("/api/users/" + student.getUserId())
                        .with(principal(student.getEmail())))
                .andExpect(status().isMethodNotAllowed());

        mockMvc.perform(patch("/api/users/" + student.getUserId() + "/status")
                        .with(principal(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isNotFound());

        // Foreign profile read by non-admin is forbidden (403)
        UserAccount otherUser = createTestUser("other_u@learnsphere.com", "ACTIVE", "STUDENT");
        mockMvc.perform(get("/api/users/" + otherUser.getUserId())
                        .with(principal(student.getEmail())))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // 6. WebSocket SUBSCRIBE Destination Authorization
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("WebSocket SUBSCRIBE enforces destination authorization: admin topic, foreign course, foreign queue")
    void webSocketSubscribe_destinationAuthorization() {
        UserAccount student = createTestUser("ws_student@learnsphere.com", "ACTIVE", "STUDENT");
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(student.getEmail());
        Principal studentPrincipal = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        // 1. Subscribe to /topic/admin/** without ADMIN role -> AccessDeniedException
        StompHeaderAccessor adminSub = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        adminSub.setDestination("/topic/admin/alerts");
        adminSub.setUser(studentPrincipal);
        Message<byte[]> adminMsg = MessageBuilder.createMessage(new byte[0], adminSub.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(adminMsg, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Admin access required for /topic/admin/alerts");

        // 2. Subscribe to foreign unenrolled course topic -> AccessDeniedException
        StompHeaderAccessor courseSub = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        courseSub.setDestination("/topic/course/99999/chat");
        courseSub.setUser(studentPrincipal);
        Message<byte[]> courseMsg = MessageBuilder.createMessage(new byte[0], courseSub.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(courseMsg, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Enrolment required for course topic /topic/course/99999/chat");

        // 3. Subscribe to foreign user queue -> AccessDeniedException
        StompHeaderAccessor foreignQueueSub = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        foreignQueueSub.setDestination("/user/other_user_queue_888/queue/notifications");
        foreignQueueSub.setUser(studentPrincipal);
        Message<byte[]> foreignQueueMsg = MessageBuilder.createMessage(new byte[0], foreignQueueSub.getMessageHeaders());

        assertThatThrownBy(() -> webSocketAuthInterceptor.preSend(foreignQueueMsg, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Cannot subscribe to foreign user destination: /user/other_user_queue_888/queue/notifications");
    }

    // -------------------------------------------------------------------------
    // 7. Account Lifecycle Enforcement (FLAGGED read-only, BLACKLISTED denial)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Account status: BLACKLISTED rejected at login; FLAGGED permitted GET but denied POST/PUT with 403")
    void accountStatus_lifecycleEnforcement() throws Exception {
        UserAccount blacklisted = createTestUser("blacklisted_user@learnsphere.com", "BLACKLISTED", "STUDENT");
        UserAccount flagged = createTestUser("flagged_user@learnsphere.com", "FLAGGED", "STUDENT");

        // 1. BLACKLISTED login rejected with 401
        LoginRequest blacklistedReq = new LoginRequest();
        blacklistedReq.setEmail(blacklisted.getEmail());
        blacklistedReq.setPassword("TestPass1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blacklistedReq)))
                .andExpect(status().isUnauthorized());

        // 2. FLAGGED can read profile (GET) -> 200
        mockMvc.perform(get("/api/users/me")
                        .with(principal(flagged.getEmail())))
                .andExpect(status().isOk());

        // 3. FLAGGED cannot execute mutating writes (PUT) -> 403
        mockMvc.perform(put("/api/users/me")
                        .with(principal(flagged.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Flagged Attempt\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private UserAccount createTestUser(String email, String status, String roleName) {
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setFullName("User " + roleName);
        user.setPassword(passwordEncoder.encode("TestPass1!"));
        user.setStatus(UserAccount.Status.valueOf(status));
        user.setCreatedAt(LocalDateTime.now());
        user = userAccountRepository.save(user);

        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            role = new Role();
            role.setName(roleName);
            role = roleRepository.save(role);
        }

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRoleRepository.save(userRole);

        return user;
    }

    private RequestPostProcessor principal(String email) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
        return user(userDetails);
    }
}
