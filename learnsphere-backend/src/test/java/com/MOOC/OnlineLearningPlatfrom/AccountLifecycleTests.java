package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Dto.UserAdminDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.AuditLog;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.AuditLogRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountLifecycleTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("BLACKLISTED account cannot authenticate at login")
    void shouldRejectLoginForBlacklistedAccount() throws Exception {
        UserAccount blacklisted = new UserAccount();
        blacklisted.setFullName("Blacklisted User");
        blacklisted.setEmail("blacklisted.user@learnsphere.com");
        blacklisted.setPassword(passwordEncoder.encode("Password@123"));
        blacklisted.setStatus(UserAccount.Status.BLACKLISTED);
        userAccountRepository.save(blacklisted);

        String loginJson = """
                {
                    "email": "blacklisted.user@learnsphere.com",
                    "password": "Password@123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("JWT token belonging to a BLACKLISTED account is rejected")
    void shouldRejectTokenForBlacklistedAccount() throws Exception {
        UserAccount user = new UserAccount();
        user.setFullName("Token Revocation User");
        user.setEmail("token.revoked@learnsphere.com");
        user.setPassword(passwordEncoder.encode("Password@123"));
        user.setStatus(UserAccount.Status.ACTIVE);
        user = userAccountRepository.save(user);

        // Login while active to get token
        String loginJson = """
                {
                    "email": "token.revoked@learnsphere.com",
                    "password": "Password@123"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        String token = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.token");

        // Now blacklist the user
        user.setStatus(UserAccount.Status.BLACKLISTED);
        userAccountRepository.save(user);

        // Accessing /api/users/me with the token must now be rejected
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("FLAGGED user can authenticate and perform read operations")
    void shouldAllowFlaggedUserToLoginAndRead() throws Exception {
        UserAccount flagged = new UserAccount();
        flagged.setFullName("Flagged Reader");
        flagged.setEmail("flagged.reader@learnsphere.com");
        flagged.setPassword(passwordEncoder.encode("Password@123"));
        flagged.setStatus(UserAccount.Status.FLAGGED);
        userAccountRepository.save(flagged);

        String loginJson = """
                {
                    "email": "flagged.reader@learnsphere.com",
                    "password": "Password@123"
                }
                """;

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.status").value("FLAGGED"))
                .andExpect(jsonPath("$.token").isString());

        mockMvc.perform(get("/api/users/me")
                        .with(as(flagged.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FLAGGED"))
                .andExpect(jsonPath("$.isActive").value(true));

        mockMvc.perform(get("/api/courses")
                        .with(as(flagged.getEmail())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FLAGGED user is read-only and blocked from mutating write operations")
    void shouldBlockFlaggedUserFromWriteOperations() throws Exception {
        UserAccount flagged = new UserAccount();
        flagged.setFullName("Flagged Writer");
        flagged.setEmail("flagged.writer@learnsphere.com");
        flagged.setPassword(passwordEncoder.encode("Password@123"));
        flagged.setStatus(UserAccount.Status.FLAGGED);
        flagged = userAccountRepository.save(flagged);

        String updateProfileJson = """
                {
                    "fullName": "Attempted Edit",
                    "bio": "Hacked bio"
                }
                """;

        mockMvc.perform(put("/api/users/me")
                        .with(as(flagged.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateProfileJson))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        String verifyCollegeJson = """
                {
                    "collegeId": 1
                }
                """;

        mockMvc.perform(post("/api/auth/verify-college")
                        .with(as(flagged.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyCollegeJson))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can update account status with audit log recording")
    void shouldAllowAdminToUpdateStatusWithAuditLog() throws Exception {
        UserAccount student = userAccountRepository.findByEmail("student@learnsphere.com");
        assertThat(student).isNotNull();

        String updateStatusJson = """
                {
                    "status": "FLAGGED"
                }
                """;

        mockMvc.perform(put("/api/admin/users/" + student.getUserId() + "/status")
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateStatusJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FLAGGED"))
                .andExpect(jsonPath("$.isActive").value(true));

        UserAccount reloaded = userAccountRepository.findById(student.getUserId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(UserAccount.Status.FLAGGED);
        assertThat(reloaded.isActive()).isTrue();

        List<AuditLog> logs = auditLogRepository.findByResourceTypeAndResourceIdOrderByIdAsc("USER", student.getUserId());
        assertThat(logs).isNotEmpty();
        AuditLog latest = logs.get(logs.size() - 1);
        assertThat(latest.getAction()).isEqualTo("USER_STATUS_CHANGED");
        assertThat(latest.getAfterJson()).containsEntry("status", "FLAGGED");

        // Reject invalid status
        mockMvc.perform(put("/api/admin/users/" + student.getUserId() + "/status")
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DEACTIVATED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Non-admin user cannot update account status")
    void shouldRejectNonAdminStatusUpdate() throws Exception {
        UserAccount student = userAccountRepository.findByEmail("student@learnsphere.com");

        mockMvc.perform(put("/api/admin/users/" + student.getUserId() + "/status")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"BLACKLISTED\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can query append-only audit log list with filtering and private no-store")
    void shouldAllowAdminToQueryAuditLogs() throws Exception {
        auditLogRepository.deleteAll();

        AuditLog log1 = new AuditLog();
        log1.setAction("USER_ROLE_ASSIGNED");
        log1.setResourceType("USER");
        log1.setResourceId(2L);
        log1.setBeforeJson(java.util.Map.of("role", "STUDENT"));
        log1.setAfterJson(java.util.Map.of("role", "TEACHER"));
        auditLogRepository.save(log1);

        AuditLog log2 = new AuditLog();
        log2.setAction("USER_STATUS_CHANGED");
        log2.setResourceType("USER");
        log2.setResourceId(2L);
        log2.setBeforeJson(java.util.Map.of("status", "ACTIVE"));
        log2.setAfterJson(java.util.Map.of("status", "FLAGGED"));
        auditLogRepository.save(log2);

        mockMvc.perform(get("/api/admin/audit")
                        .with(as("admin@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").isNumber());

        // Filter by action
        mockMvc.perform(get("/api/admin/audit")
                        .param("action", "USER_STATUS_CHANGED")
                        .with(as("admin@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].action").value("USER_STATUS_CHANGED"));
    }

    @Test
    @DisplayName("Non-admin and unauthenticated requests cannot access /api/admin/audit")
    void shouldProtectAuditEndpointFromNonAdminAccess() throws Exception {
        mockMvc.perform(get("/api/admin/audit"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/audit")
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Derived isActive correctly reflects account status")
    void shouldVerifyDerivedIsActive() {
        UserAccount active = new UserAccount();
        active.setStatus(UserAccount.Status.ACTIVE);
        assertThat(active.isActive()).isTrue();

        UserAccount flagged = new UserAccount();
        flagged.setStatus(UserAccount.Status.FLAGGED);
        assertThat(flagged.isActive()).isTrue();

        UserAccount blacklisted = new UserAccount();
        blacklisted.setStatus(UserAccount.Status.BLACKLISTED);
        assertThat(blacklisted.isActive()).isFalse();

        UserResponseDto activeDto = UserResponseDto.from(active, List.of("STUDENT"));
        assertThat(activeDto.getIsActive()).isTrue();

        UserResponseDto blacklistedDto = UserResponseDto.from(blacklisted, List.of("STUDENT"));
        assertThat(blacklistedDto.getIsActive()).isFalse();

        UserAdminDto adminDto = UserAdminDto.from(active, List.of("STUDENT"));
        assertThat(adminDto.getIsActive()).isTrue();
    }
}
