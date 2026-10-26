package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserRole;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W1-11 regression suite for the legacy user surface.
 *
 * <p>Locks the target contract from {@code Docs/plan/api-catalogue.md}:
 * {@code /users/me} is self-only, the generic list/create/update/delete/status
 * writes are retired, and the remaining user/role management endpoints are
 * ADMIN-only, DTO-shaped and audited. The audit contract is checked directly
 * against the database so a future controller refactor cannot silently stop
 * writing history.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserApiAuthorizationTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    private Long userId(String email) {
        UserAccount account = userAccountRepository.findByEmail(email);
        assertThat(account).as("seeded user %s", email).isNotNull();
        return account.getUserId();
    }

    private Long roleId(String name) {
        return roleRepository.findByName(name).getRoleId();
    }

    @Test
    void shouldRetireTheGenericUserCollectionAndWrites() throws Exception {
        Long studentId = userId("student@learnsphere.com");
        Long adminId = userId("admin@learnsphere.com");

        mockMvc.perform(get("/api/users").with(as("admin@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(post("/api/users")
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"created@example.com\",\"password\":\"Bypass1!\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(put("/api/users/{id}", studentId)
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Hijacked\"}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));

        mockMvc.perform(delete("/api/users/{id}", studentId)
                        .with(as("admin@learnsphere.com")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));

        mockMvc.perform(patch("/api/users/{id}/status", studentId)
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLACKLISTED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        UserAccount student = userAccountRepository.findById(studentId).orElseThrow();
        assertThat(student.getFullName()).isNotEqualTo("Hijacked");
        assertThat(student.getStatus()).isEqualTo(UserAccount.Status.ACTIVE);
        assertThat(userAccountRepository.findByEmail("created@example.com")).isNull();
        assertThat(userId("admin@learnsphere.com")).isEqualTo(adminId);
    }

    @Test
    void shouldRejectNonAdminsFromUserAdministration() throws Exception {
        Long studentId = userId("student@learnsphere.com");
        Long adminId = userId("admin@learnsphere.com");
        Long adminRoleId = roleId("ADMIN");
        Long adminRoleAssignmentId = userRoleRepository
                .findByUser_UserIdAndRole_RoleId(adminId, adminRoleId)
                .orElseThrow()
                .getUserRoleId();

        mockMvc.perform(get("/api/users/{id}", adminId).with(as("student@learnsphere.com")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/users/{id}/roles", adminId).with(as("student@learnsphere.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/users/{id}/roles", studentId)
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":" + roleId("TEACHER") + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(delete("/api/users/{id}/roles/{roleId}", adminId, adminRoleId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/user-roles").with(as("student@learnsphere.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/user-roles")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + studentId + ",\"roleId\":" + roleId("ADMIN") + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(delete("/api/user-roles/{id}", adminRoleAssignmentId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(put("/api/admin/users/{id}/status", studentId)
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLACKLISTED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertThat(userRoleRepository.findByUser_UserIdAndRole_RoleId(adminId, adminRoleId))
                .as("the student must not be able to remove an admin role")
                .isPresent();
    }

    @Test
    void shouldReturnAdminUserViewsWithoutPasswordOrEntities() throws Exception {
        Long studentId = userId("student@learnsphere.com");
        UserRole studentRole = userRoleRepository.findByUser_UserId(studentId).stream()
                .filter(assignment -> "STUDENT".equals(assignment.getRole().getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/users/{id}", studentId).with(as("admin@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(
                        containsString("no-store"), containsString("private"))))
                .andExpect(jsonPath("$.id").value(studentId))
                .andExpect(jsonPath("$.email").value("student@learnsphere.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/users/{id}/roles", studentId).with(as("admin@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(
                        containsString("no-store"), containsString("private"))))
                .andExpect(jsonPath("$[0].id").value(studentRole.getUserRoleId()))
                .andExpect(jsonPath("$[0].userId").value(studentId))
                .andExpect(jsonPath("$[0].roleId").value(studentRole.getRole().getRoleId()))
                .andExpect(jsonPath("$[0].roleName").value("STUDENT"))
                .andExpect(jsonPath("$[0].user").doesNotExist())
                .andExpect(jsonPath("$[0].role").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());

        mockMvc.perform(get("/api/user-roles/{id}", studentRole.getUserRoleId())
                        .with(as("admin@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentRole.getUserRoleId()))
                .andExpect(jsonPath("$.userId").value(studentId))
                .andExpect(jsonPath("$.roleName").value("STUDENT"))
                .andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());

        mockMvc.perform(get("/api/user-roles").with(as("admin@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(
                        containsString("no-store"), containsString("private"))))
                .andExpect(jsonPath("$[0].roleName").exists())
                .andExpect(jsonPath("$[0].user").doesNotExist());

        mockMvc.perform(get("/api/users/{id}", 999_999L).with(as("admin@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldAssignAndRevokeRolesWithAudit() throws Exception {
        Long studentId = userId("student@learnsphere.com");
        Long adminId = userId("admin@learnsphere.com");
        Long teacherRoleId = roleId("TEACHER");

        mockMvc.perform(post("/api/users/{id}/roles", studentId)
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":" + teacherRoleId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(studentId))
                .andExpect(jsonPath("$.roleId").value(teacherRoleId))
                .andExpect(jsonPath("$.roleName").value("TEACHER"))
                .andExpect(jsonPath("$.user").doesNotExist());

        assertThat(userRoleRepository.findByUser_UserIdAndRole_RoleId(studentId, teacherRoleId))
                .isPresent();

        mockMvc.perform(post("/api/users/{id}/roles", studentId)
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":" + teacherRoleId + "}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        Map<String, Object> assigned = singleAuditRow("USER_ROLE_ASSIGNED", studentId);
        assertThat(((Number) assigned.get("actor_id")).longValue()).isEqualTo(adminId);
        assertThat(assigned.get("resource_type")).isEqualTo("USER");
        assertThat((String) assigned.get("after_json"))
                .contains("\"TEACHER\"")
                .contains(String.valueOf(teacherRoleId));
        assertThat(assigned.get("before_json")).isNull();

        mockMvc.perform(delete("/api/users/{id}/roles/{roleId}", studentId, teacherRoleId)
                        .with(as("admin@learnsphere.com")))
                .andExpect(status().isNoContent());

        assertThat(userRoleRepository.findByUser_UserIdAndRole_RoleId(studentId, teacherRoleId))
                .isEmpty();

        Map<String, Object> revoked = singleAuditRow("USER_ROLE_REVOKED", studentId);
        assertThat(((Number) revoked.get("actor_id")).longValue()).isEqualTo(adminId);
        assertThat((String) revoked.get("before_json"))
                .contains("\"TEACHER\"")
                .contains(String.valueOf(teacherRoleId));
        assertThat(revoked.get("after_json")).isNull();

        mockMvc.perform(post("/api/user-roles")
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + studentId + ",\"roleId\":" + teacherRoleId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(studentId))
                .andExpect(jsonPath("$.roleName").value("TEACHER"));

        Long assignmentId = userRoleRepository
                .findByUser_UserIdAndRole_RoleId(studentId, teacherRoleId)
                .orElseThrow()
                .getUserRoleId();

        mockMvc.perform(delete("/api/user-roles/{id}", assignmentId)
                        .with(as("admin@learnsphere.com")))
                .andExpect(status().isNoContent());

        assertThat(userRoleRepository.findByUser_UserIdAndRole_RoleId(studentId, teacherRoleId))
                .isEmpty();
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from audit_log where action = 'USER_ROLE_REVOKED' and resource_id = ?",
                Integer.class, studentId)).isEqualTo(2);
    }

    @Test
    void shouldAuditAdminStatusChanges() throws Exception {
        Long studentId = userId("student@learnsphere.com");
        Long adminId = userId("admin@learnsphere.com");

        mockMvc.perform(put("/api/admin/users/{id}/status", studentId)
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FLAGGED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentId))
                .andExpect(jsonPath("$.status").value("FLAGGED"));

        Map<String, Object> changed = singleAuditRow("USER_STATUS_CHANGED", studentId);
        assertThat(((Number) changed.get("actor_id")).longValue()).isEqualTo(adminId);
        assertThat(changed.get("resource_type")).isEqualTo("USER");
        assertThat((String) changed.get("before_json")).contains("\"ACTIVE\"");
        assertThat((String) changed.get("after_json")).contains("\"FLAGGED\"");
        assertThat(changed.get("ip")).isNotNull();

        mockMvc.perform(put("/api/admin/users/{id}/status", studentId)
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_A_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(put("/api/admin/users/{id}/status", studentId)
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void shouldOnlyUpdateSafeSelfProfileFields() throws Exception {
        Long studentId = userId("student@learnsphere.com");
        UserAccount before = userAccountRepository.findById(studentId).orElseThrow();
        String originalEmail = before.getEmail();
        String originalPasswordHash = before.getPassword();

        mockMvc.perform(put("/api/users/me")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Updated Student",
                                  "bio": "New bio",
                                  "avatarUrl": "https://example.com/avatar.png",
                                  "email": "attacker@example.com",
                                  "status": "BLACKLISTED",
                                  "password": "Attacker1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(
                        containsString("no-store"), containsString("private"))))
                .andExpect(jsonPath("$.id").value(studentId))
                .andExpect(jsonPath("$.fullName").value("Updated Student"))
                .andExpect(jsonPath("$.email").value(originalEmail))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        entityManager.flush();
        entityManager.clear();

        UserAccount reloaded = userAccountRepository.findById(studentId).orElseThrow();
        assertThat(reloaded.getEmail()).isEqualTo(originalEmail);
        assertThat(reloaded.getStatus()).isEqualTo(UserAccount.Status.ACTIVE);
        assertThat(reloaded.getPassword()).isEqualTo(originalPasswordHash);
        assertThat(reloaded.getFullName()).isEqualTo("Updated Student");

        mockMvc.perform(get("/api/users/me").with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(
                        containsString("no-store"), containsString("private"))))
                .andExpect(jsonPath("$.id").value(studentId))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldRequireAuthenticationForTheUserSurface() throws Exception {
        Long studentId = userId("student@learnsphere.com");

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/users/{id}", studentId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/user-roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private Map<String, Object> singleAuditRow(String action, Long resourceId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select actor_id, action, resource_type, resource_id, "
                        + "before_json::text as before_json, after_json::text as after_json, ip "
                        + "from audit_log where action = ? and resource_id = ? order by id",
                action, resourceId);
        assertThat(rows)
                .as("exactly one %s audit row for resource %s", action, resourceId)
                .hasSize(1);
        return rows.get(0);
    }
}
