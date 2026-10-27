package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Role;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserRole;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W1-01 test suite: Freeze role vocabulary to ADMIN/TEACHER/STUDENT,
 * seed roles idempotently, and map INSTRUCTOR to TEACHER at the edge (A-01).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleVocabularyTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("Public registration with INSTRUCTOR alias creates STUDENT and is accepted as valid vocabulary")
    void shouldAcceptInstructorAliasOnRegistrationCreatingStudent() throws Exception {
        String registerJson = """
                {
                    "fullName": "Instructor Alias User",
                    "email": "instructor.alias@learnsphere.com",
                    "password": "Password@123",
                    "role": "INSTRUCTOR"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.token").isString());

        UserAccount registered = userAccountRepository.findByEmail("instructor.alias@learnsphere.com");
        assertThat(registered).isNotNull();

        List<UserRole> roles = userRoleRepository.findByUser_UserId(registered.getUserId());
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getRole().getName()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("Registration with unknown role outside frozen vocabulary is rejected")
    void shouldRejectRegistrationWithUnknownRole() throws Exception {
        String registerJson = """
                {
                    "fullName": "Invalid Role User",
                    "email": "invalid.role@learnsphere.com",
                    "password": "Password@123",
                    "role": "MANAGER"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.detail").value(containsString("MANAGER")));
    }

    @Test
    @DisplayName("Admin user filter with role=INSTRUCTOR matches users having TEACHER role")
    void shouldMapInstructorToTeacherInAdminUserFilter() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .param("role", "INSTRUCTOR")
                        .with(as("admin@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem("teacher@learnsphere.com")));
    }

    @Test
    @DisplayName("/api/roles endpoints are forbidden to non-admin users")
    void shouldEnforceAdminOnlyOnRoleEndpoints() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/roles").with(as("student@learnsphere.com")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/roles")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"TEACHER\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can list roles and receives frozen vocabulary with private no-store")
    void shouldReturnFrozenRoleVocabularyToAdmin() throws Exception {
        mockMvc.perform(get("/api/roles").with(as("admin@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(containsString("private"), containsString("no-store"))))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("ADMIN", "TEACHER", "STUDENT")));
    }

    @Test
    @DisplayName("Admin creating role outside frozen vocabulary is rejected")
    void shouldRejectCreatingRoleOutsideFrozenVocabulary() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"SUPERUSER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Admin creating role with INSTRUCTOR alias maps to TEACHER and returns 409 Conflict if TEACHER exists")
    void shouldMapInstructorToTeacherAndConflictIfAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"INSTRUCTOR\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("Admin cannot delete a reference role")
    void shouldPreventDeletingReferenceRoles() throws Exception {
        Role teacherRole = roleRepository.findByName("TEACHER");
        assertThat(teacherRole).isNotNull();

        mockMvc.perform(delete("/api/roles/{id}", teacherRole.getRoleId())
                        .with(as("admin@learnsphere.com")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }
}
