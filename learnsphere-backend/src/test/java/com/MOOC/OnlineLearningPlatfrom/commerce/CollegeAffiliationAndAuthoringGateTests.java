package com.MOOC.OnlineLearningPlatfrom.commerce;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Story W6-10: College verification + affiliation gate
 * Rules: A-24, [F-F12...F-F14], spec §4 SCR-58, README.md:81
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CollegeAffiliationAndAuthoringGateTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private AffiliationRequestRepository affiliationRequestRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtService jwtService;

    private UserAccount admin;
    private UserAccount approvedTeacher;
    private UserAccount pendingTeacher;
    private College verifiedCollege;
    private College pendingCollege;
    private Department department;

    private String adminToken;
    private String approvedTeacherToken;
    private String pendingTeacherToken;

    @BeforeEach
    void setUp() {
        Role adminRole = findOrCreateRole("ADMIN");
        Role teacherRole = findOrCreateRole("TEACHER");

        verifiedCollege = findOrCreateCollege("Gate Verified College", "Bengaluru", College.VerificationStatus.VERIFIED);
        pendingCollege = findOrCreateCollege("Gate Pending College", "Mysuru", College.VerificationStatus.PENDING);

        department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Affiliation Test Dept " + System.currentTimeMillis());
            return departmentRepository.save(d);
        });

        admin = findOrCreateUser("admin.affil@learnsphere.test", "Admin Affil", adminRole, verifiedCollege);
        approvedTeacher = findOrCreateUser("approved.teacher@learnsphere.test", "Approved Teacher", teacherRole, verifiedCollege);
        pendingTeacher = findOrCreateUser("pending.teacher@learnsphere.test", "Pending Teacher", teacherRole, null);

        // Clear existing affiliations for test teachers
        affiliationRequestRepository.findAll().stream()
                .filter(r -> r.getTeacher() != null &&
                        (r.getTeacher().getUserId().equals(approvedTeacher.getUserId()) ||
                         r.getTeacher().getUserId().equals(pendingTeacher.getUserId())))
                .forEach(affiliationRequestRepository::delete);

        // Seed approved affiliation for approvedTeacher
        AffiliationRequest approvedReq = new AffiliationRequest();
        approvedReq.setTeacher(approvedTeacher);
        approvedReq.setClaimedCollege(verifiedCollege);
        approvedReq.setClaimedDepartment(department.getName());
        approvedReq.setStatus(AffiliationRequest.Status.APPROVED);
        approvedReq.setCreatedAt(LocalDateTime.now());
        affiliationRequestRepository.save(approvedReq);

        // Clear notifications
        notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(approvedTeacher.getUserId()).forEach(notificationRepository::delete);
        notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(pendingTeacher.getUserId()).forEach(notificationRepository::delete);

        adminToken = jwtService.generateToken(admin.getEmail(), admin.getUserId(), List.of("ADMIN"));
        approvedTeacherToken = jwtService.generateToken(approvedTeacher.getEmail(), approvedTeacher.getUserId(), List.of("TEACHER"));
        pendingTeacherToken = jwtService.generateToken(pendingTeacher.getEmail(), pendingTeacher.getUserId(), List.of("TEACHER"));
    }

    private Role findOrCreateRole(String name) {
        Role r = roleRepository.findByName(name);
        if (r != null) return r;
        Role nr = new Role();
        nr.setName(name);
        return roleRepository.save(nr);
    }

    private College findOrCreateCollege(String name, String city, College.VerificationStatus status) {
        return collegeRepository.findByName(name).orElseGet(() -> {
            College c = new College();
            c.setName(name);
            c.setCity(city);
            c.setVerificationStatus(status);
            return collegeRepository.save(c);
        });
    }

    private UserAccount findOrCreateUser(String email, String fullName, Role role, College college) {
        UserAccount existing = userAccountRepository.findByEmail(email);
        if (existing != null) {
            existing.setCollege(college);
            return userAccountRepository.save(existing);
        }
        UserAccount u = new UserAccount();
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPassword("hashedpassword");
        u.setStatus(UserAccount.Status.ACTIVE);
        u.setCollege(college);
        u = userAccountRepository.save(u);

        UserRole ur = new UserRole();
        ur.setUser(u);
        ur.setRole(role);
        userRoleRepository.save(ur);
        return u;
    }

    @Test
    @DisplayName("Admin rejectCollege transitions college to REJECTED rather than hard deleting the row")
    void rejectCollegeArchivesInsteadOfHardDelete() throws Exception {
        College toReject = findOrCreateCollege("To Reject College", "Hubli", College.VerificationStatus.PENDING);

        mockMvc.perform(post("/api/admin/colleges/" + toReject.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        College reloaded = collegeRepository.findById(toReject.getId()).orElse(null);
        assertThat(reloaded).isNotNull();
        assertThat(reloaded.getVerificationStatus()).isEqualTo(College.VerificationStatus.REJECTED);
    }

    @Test
    @DisplayName("Admin approveAffiliation updates teacher college and sends approval notification")
    void approveAffiliationNotifiesTeacher() throws Exception {
        AffiliationRequest req = new AffiliationRequest();
        req.setTeacher(pendingTeacher);
        req.setClaimedCollege(verifiedCollege);
        req.setClaimedDepartment(department.getName());
        req.setStatus(AffiliationRequest.Status.PENDING);
        req.setCreatedAt(LocalDateTime.now());
        req = affiliationRequestRepository.save(req);

        mockMvc.perform(post("/api/admin/affiliations/" + req.getId() + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        AffiliationRequest approved = affiliationRequestRepository.findById(req.getId()).orElseThrow();
        assertThat(approved.getStatus()).isEqualTo(AffiliationRequest.Status.APPROVED);

        UserAccount teacherReloaded = userAccountRepository.findById(pendingTeacher.getUserId()).orElseThrow();
        assertThat(teacherReloaded.getCollege()).isNotNull();
        assertThat(teacherReloaded.getCollege().getId()).isEqualTo(verifiedCollege.getId());

        List<Notification> notifications = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(pendingTeacher.getUserId());
        assertThat(notifications).isNotEmpty();
        Notification latest = notifications.get(0);
        assertThat(latest.getTitle()).containsIgnoringCase("Approved");
    }

    @Test
    @DisplayName("Admin rejectAffiliation sets REJECTED status and sends rejection notification")
    void rejectAffiliationNotifiesTeacher() throws Exception {
        AffiliationRequest req = new AffiliationRequest();
        req.setTeacher(pendingTeacher);
        req.setClaimedCollege(verifiedCollege);
        req.setClaimedDepartment(department.getName());
        req.setStatus(AffiliationRequest.Status.PENDING);
        req.setCreatedAt(LocalDateTime.now());
        req = affiliationRequestRepository.save(req);

        mockMvc.perform(post("/api/admin/affiliations/" + req.getId() + "/reject")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        AffiliationRequest rejected = affiliationRequestRepository.findById(req.getId()).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(AffiliationRequest.Status.REJECTED);

        List<Notification> notifications = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(pendingTeacher.getUserId());
        assertThat(notifications).isNotEmpty();
        Notification latest = notifications.get(0);
        assertThat(latest.getTitle()).containsIgnoringCase("Rejected");
    }

    @Test
    @DisplayName("Teacher without approved affiliation to a verified college is forbidden from creating courses (403)")
    void teacherWithoutApprovedAffiliationBlockedFromAuthoring() throws Exception {
        String body = """
                {
                    "title": "Unverified Course Creation Attempt",
                    "description": "Should fail with 403",
                    "departmentId": %d
                }
                """.formatted(department.getDepartmentId());

        mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + pendingTeacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Teacher with approved affiliation to verified college can create course in DRAFT")
    void teacherWithApprovedAffiliationCanCreateCourse() throws Exception {
        String body = """
                {
                    "title": "Verified Faculty Course %d",
                    "description": "Should succeed in DRAFT",
                    "departmentId": %d
                }
                """.formatted(System.currentTimeMillis(), department.getDepartmentId());

        mockMvc.perform(post("/api/courses")
                        .header("Authorization", "Bearer " + approvedTeacherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }
}

