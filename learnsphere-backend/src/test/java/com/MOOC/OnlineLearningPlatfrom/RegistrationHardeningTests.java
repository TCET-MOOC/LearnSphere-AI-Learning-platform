package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationHardeningTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private AffiliationRequestRepository affiliationRequestRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("Self-registration with ADMIN role is rejected with 400 Bad Request")
    void shouldRejectSelfRegistrationWithAdminRole() throws Exception {
        String registerJson = """
                {
                    "fullName": "Malicious Admin Wannabe",
                    "email": "hacker.admin@learnsphere.com",
                    "password": "Password@123",
                    "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.detail").value(containsString("ADMIN accounts cannot be self-registered")));

        assertThat(userAccountRepository.findByEmail("hacker.admin@learnsphere.com")).isNull();
    }

    @Test
    @DisplayName("Public registration with TEACHER role creates a STUDENT account")
    void shouldCreateStudentAccountWhenRegisteringWithTeacherRole() throws Exception {
        String registerJson = """
                {
                    "fullName": "Teacher Applicant",
                    "email": "teacher.applicant@learnsphere.com",
                    "password": "Password@123",
                    "role": "TEACHER"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.token").isString());

        UserAccount registered = userAccountRepository.findByEmail("teacher.applicant@learnsphere.com");
        assertThat(registered).isNotNull();

        List<UserRole> roles = userRoleRepository.findByUser_UserId(registered.getUserId());
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getRole().getName()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("Public registration without role field creates a STUDENT account")
    void shouldCreateStudentAccountWhenRegisteringWithoutRole() throws Exception {
        String registerJson = """
                {
                    "fullName": "Default Student",
                    "email": "default.student@learnsphere.com",
                    "password": "Password@123"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("STUDENT"))
                .andExpect(jsonPath("$.token").isString());

        UserAccount registered = userAccountRepository.findByEmail("default.student@learnsphere.com");
        assertThat(registered).isNotNull();

        List<UserRole> roles = userRoleRepository.findByUser_UserId(registered.getUserId());
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getRole().getName()).isEqualTo("STUDENT");
    }

    @Test
    @DisplayName("Public registration with unverified college is rejected with 400 Bad Request")
    void shouldRejectRegistrationWithUnverifiedCollege() throws Exception {
        College pendingCollege = new College();
        pendingCollege.setName("Unverified Academy " + System.currentTimeMillis());
        pendingCollege.setCity("Mumbai");
        pendingCollege.setVerificationStatus(College.VerificationStatus.PENDING);
        pendingCollege = collegeRepository.save(pendingCollege);

        String registerJson = String.format("""
                {
                    "fullName": "Pending College Registrant",
                    "email": "pending.registrant%d@learnsphere.com",
                    "password": "Password@123",
                    "collegeId": %d
                }
                """, System.currentTimeMillis(), pendingCollege.getId());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.detail").value(containsString("verified")));
    }

    @Test
    @DisplayName("Student teacher application with valid details creates PENDING affiliation request")
    void shouldCreatePendingAffiliationRequestWhenStudentApplies() throws Exception {
        College verifiedCollege = new College();
        verifiedCollege.setName("Verified Institute " + System.currentTimeMillis());
        verifiedCollege.setCity("Mumbai");
        verifiedCollege.setVerificationStatus(College.VerificationStatus.VERIFIED);
        verifiedCollege = collegeRepository.save(verifiedCollege);

        String verifyJson = String.format("""
                {
                    "collegeId": %d,
                    "claimedDepartment": "Computer Engineering",
                    "idDocumentUrl": "https://storage.learnsphere.com/docs/teacher-id-123.pdf"
                }
                """, verifiedCollege.getId());

        mockMvc.perform(post("/api/auth/verify-college")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(status().isOk());

        UserAccount student = userAccountRepository.findByEmail("student@learnsphere.com");
        List<AffiliationRequest> requests = affiliationRequestRepository.findAll().stream()
                .filter(r -> r.getTeacher().getUserId().equals(student.getUserId()))
                .toList();

        assertThat(requests).isNotEmpty();
        AffiliationRequest latest = requests.get(requests.size() - 1);
        assertThat(latest.getStatus()).isEqualTo(AffiliationRequest.Status.PENDING);
        assertThat(latest.getClaimedCollege().getId()).isEqualTo(verifiedCollege.getId());
        assertThat(latest.getClaimedDepartment()).isEqualTo("Computer Engineering");
        assertThat(latest.getIdDocumentUrl()).isEqualTo("https://storage.learnsphere.com/docs/teacher-id-123.pdf");
    }

    @Test
    @DisplayName("Teacher application with unverified college is rejected with 400 Bad Request")
    void shouldRejectTeacherApplicationWithUnverifiedCollege() throws Exception {
        College unverifiedCollege = new College();
        unverifiedCollege.setName("Unverified College " + System.currentTimeMillis());
        unverifiedCollege.setCity("Pune");
        unverifiedCollege.setVerificationStatus(College.VerificationStatus.PENDING);
        unverifiedCollege = collegeRepository.save(unverifiedCollege);

        String verifyJson = String.format("""
                {
                    "collegeId": %d,
                    "claimedDepartment": "Mechanical Engineering",
                    "idDocumentUrl": "https://storage.learnsphere.com/docs/pending-col-id.pdf"
                }
                """, unverifiedCollege.getId());

        mockMvc.perform(post("/api/auth/verify-college")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.detail").value(containsString("verified")));
    }

    @Test
    @DisplayName("Duplicate pending affiliation request is rejected with 409 Conflict")
    void shouldRejectDuplicatePendingAffiliationRequestWithConflict() throws Exception {
        UserAccount applicant = new UserAccount();
        applicant.setFullName("Duplicate Applicant");
        applicant.setEmail("dup.applicant" + System.currentTimeMillis() + "@learnsphere.com");
        applicant.setPassword("Password@123");
        applicant = userAccountRepository.save(applicant);

        College college = collegeRepository.findAll().stream()
                .filter(c -> c.getVerificationStatus() == College.VerificationStatus.VERIFIED)
                .findFirst()
                .orElseThrow();

        AffiliationRequest existingPending = new AffiliationRequest();
        existingPending.setTeacher(applicant);
        existingPending.setClaimedCollege(college);
        existingPending.setClaimedDepartment("Information Technology");
        existingPending.setIdDocumentUrl("https://storage.learnsphere.com/docs/app1.pdf");
        existingPending.setStatus(AffiliationRequest.Status.PENDING);
        affiliationRequestRepository.save(existingPending);

        String verifyJson = String.format("""
                {
                    "collegeId": %d,
                    "claimedDepartment": "Information Technology",
                    "idDocumentUrl": "https://storage.learnsphere.com/docs/app2.pdf"
                }
                """, college.getId());

        mockMvc.perform(post("/api/auth/verify-college")
                        .with(as(applicant.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verifyJson))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.detail").value(containsString("pending affiliation request")));
    }

    @Test
    @DisplayName("Admin approving affiliation promotes user to TEACHER and writes audit log")
    void shouldPromoteUserToTeacherWhenAdminApprovesAffiliation() throws Exception {
        UserAccount applicant = new UserAccount();
        applicant.setFullName("Promotion Candidate");
        applicant.setEmail("promo.candidate" + System.currentTimeMillis() + "@learnsphere.com");
        applicant.setPassword("Password@123");
        applicant = userAccountRepository.save(applicant);

        College college = collegeRepository.findAll().stream()
                .filter(c -> c.getVerificationStatus() == College.VerificationStatus.VERIFIED)
                .findFirst()
                .orElseThrow();

        AffiliationRequest request = new AffiliationRequest();
        request.setTeacher(applicant);
        request.setClaimedCollege(college);
        request.setClaimedDepartment("Data Science");
        request.setIdDocumentUrl("https://storage.learnsphere.com/docs/promo.pdf");
        request.setStatus(AffiliationRequest.Status.PENDING);
        request = affiliationRequestRepository.save(request);

        mockMvc.perform(put("/api/admin/colleges/affiliations/" + request.getId() + "/approve")
                        .with(as("admin@learnsphere.com")))
                .andExpect(status().isOk());

        AffiliationRequest reloadedRequest = affiliationRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(reloadedRequest.getStatus()).isEqualTo(AffiliationRequest.Status.APPROVED);

        UserAccount reloadedUser = userAccountRepository.findById(applicant.getUserId()).orElseThrow();
        assertThat(reloadedUser.getCollege()).isNotNull();
        assertThat(reloadedUser.getCollege().getId()).isEqualTo(college.getId());

        List<UserRole> roles = userRoleRepository.findByUser_UserId(applicant.getUserId());
        assertThat(roles.stream().anyMatch(r -> "TEACHER".equals(r.getRole().getName()))).isTrue();

        List<AuditLog> auditLogs = auditLogRepository.findByResourceTypeAndResourceIdOrderByIdAsc("USER", applicant.getUserId());
        assertThat(auditLogs).isNotEmpty();
        AuditLog audit = auditLogs.get(auditLogs.size() - 1);
        assertThat(audit.getAction()).isEqualTo("USER_ROLE_ASSIGNED");
        assertThat(audit.getAfterJson()).containsEntry("role", "TEACHER");
    }
}
