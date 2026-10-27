package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.AuditLogRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W2-02 integration tests: Course state machine (DRAFT/PENDING/LIVE/ARCHIVED),
 * isPublished projection, admin approval only, audit logging, and public catalog draft filtering.
 * Rules: A-04, AC-10.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourseStateMachineTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private UserAccount teacher;
    private UserAccount admin;
    private UserAccount student;
    private Department department;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        assertThat(teacher).isNotNull();
        assertThat(admin).isNotNull();
        assertThat(student).isNotNull();

        department = departmentRepository.findByNameIgnoreCase("Computer Science")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Computer Science", null)));
    }

    private RequestPostProcessor asTeacher() {
        return user(userDetailsService.loadUserByUsername(teacher.getEmail()));
    }

    private RequestPostProcessor asAdmin() {
        return user(userDetailsService.loadUserByUsername(admin.getEmail()));
    }

    private RequestPostProcessor asStudent() {
        return user(userDetailsService.loadUserByUsername(student.getEmail()));
    }

    private Course createCourse(String title, Course.Status status) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription("Course description for " + title);
        course.setTeacher(teacher);
        course.setDepartment(department);
        course.setStatus(status);
        course.setPrice(BigDecimal.ZERO);
        return courseRepository.save(course);
    }

    @Test
    @DisplayName("Course creation unconditionally starts in DRAFT even if client requests LIVE or isPublished=true")
    void shouldEnforceDraftStatusOnCourseCreation() throws Exception {
        String body = """
                {
                    "title": "Autonomous AI Systems",
                    "description": "Building agentic systems",
                    "departmentId": %d,
                    "status": "LIVE",
                    "isPublished": true
                }
                """.formatted(department.getDepartmentId());

        mockMvc.perform(post("/api/courses")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.isPublished").value(false));
    }

    @Test
    @DisplayName("Teacher can transition course from DRAFT to PENDING (submit for review)")
    void shouldAllowTeacherToTransitionDraftToPending() throws Exception {
        Course course = createCourse("Draft Course For Submission", Course.Status.DRAFT);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.isPublished").value(false));

        Course updated = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Course.Status.PENDING);
        assertThat(updated.getIsPublished()).isFalse();
    }

    @Test
    @DisplayName("Teacher can transition course from PENDING to DRAFT (withdraw submission)")
    void shouldAllowTeacherToTransitionPendingToDraft() throws Exception {
        Course course = createCourse("Pending Course To Withdraw", Course.Status.PENDING);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DRAFT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.isPublished").value(false));

        Course updated = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Course.Status.DRAFT);
    }

    @Test
    @DisplayName("Teacher is forbidden from setting status to LIVE directly (admin approval only)")
    void shouldForbidTeacherFromSettingStatusToLive() throws Exception {
        Course course = createCourse("Course Pending Direct Publish", Course.Status.PENDING);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LIVE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Verify status remains unchanged
        Course unchanged = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(Course.Status.PENDING);
    }

    @Test
    @DisplayName("Non-admin is forbidden from calling admin status endpoint")
    void shouldForbidNonAdminFromCallingAdminStatusEndpoint() throws Exception {
        Course course = createCourse("Admin Protected Status Course", Course.Status.PENDING);

        mockMvc.perform(put("/api/admin/courses/" + course.getId() + "/status")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LIVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can approve course PENDING -> LIVE via admin endpoint and action is audited")
    void shouldAllowAdminToApproveCourseAndRecordAuditLog() throws Exception {
        Course course = createCourse("Course Awaiting Admin Approval", Course.Status.PENDING);

        mockMvc.perform(put("/api/admin/courses/" + course.getId() + "/status")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"))
                .andExpect(jsonPath("$.isPublished").value(true));

        Course liveCourse = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(liveCourse.getStatus()).isEqualTo(Course.Status.LIVE);
        assertThat(liveCourse.getIsPublished()).isTrue();

        // Verify AuditLog entry
        assertThat(auditLogRepository.findAll()).anySatisfy(log -> {
            assertThat(log.getAction()).isEqualTo("COURSE_STATUS_CHANGED");
            assertThat(log.getResourceType()).isEqualTo("COURSE");
            assertThat(log.getResourceId()).isEqualTo(course.getId());
            assertThat(log.getBeforeJson()).containsEntry("status", "PENDING");
            assertThat(log.getAfterJson()).containsEntry("status", "LIVE");
        });
    }

    @Test
    @DisplayName("Admin status endpoint rejects invalid or blank status with 400 Bad Request")
    void shouldRejectInvalidOrBlankStatusOnAdminEndpoint() throws Exception {
        Course course = createCourse("Invalid Status Course", Course.Status.DRAFT);

        mockMvc.perform(put("/api/admin/courses/" + course.getId() + "/status")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"UNKNOWN_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        mockMvc.perform(put("/api/admin/courses/" + course.getId() + "/status")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Public catalog GET /api/courses returns only LIVE courses, excluding DRAFT and PENDING")
    void shouldExcludeNonLiveCoursesFromPublicCatalog() throws Exception {
        Course liveCourse = createCourse("Public Live Course", Course.Status.LIVE);
        Course draftCourse = createCourse("Secret Draft Course", Course.Status.DRAFT);
        Course pendingCourse = createCourse("Secret Pending Course", Course.Status.PENDING);

        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", hasItem(liveCourse.getTitle())))
                .andExpect(jsonPath("$[*].title", not(hasItem(draftCourse.getTitle()))))
                .andExpect(jsonPath("$[*].title", not(hasItem(pendingCourse.getTitle()))));
    }

    @Test
    @DisplayName("Public GET /api/courses/{id} returns 404 for draft course unless requester is staff/admin")
    void shouldHideDraftCourseFromPublicAndStudents() throws Exception {
        Course draftCourse = createCourse("Hidden Draft Course", Course.Status.DRAFT);

        // Anonymous unauthenticated -> 404
        mockMvc.perform(get("/api/courses/" + draftCourse.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Student -> 404
        mockMvc.perform(get("/api/courses/" + draftCourse.getId())
                        .with(asStudent()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Course owner (teacher) -> 200
        mockMvc.perform(get("/api/courses/" + draftCourse.getId())
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(draftCourse.getTitle()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.isPublished").value(false));

        // Admin -> 200
        mockMvc.perform(get("/api/courses/" + draftCourse.getId())
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(draftCourse.getTitle()));
    }
}
