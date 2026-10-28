package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W3-01 integration tests:
 * - Unique (user_id, course_id, role)
 * - Self-only enrollment (foreign user enrollment denied for non-admin)
 * - Require course status == LIVE (AC-10 / BR-08)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EnrollmentUniquenessAndPublishTests extends PostgresIntegrationTest {

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
    private EnrollmentRepository enrollmentRepository;

    private UserAccount teacher;
    private UserAccount admin;
    private UserAccount student;
    private UserAccount otherStudent;
    private Department department;
    private Course liveCourse;
    private Course draftCourse;
    private Course pendingCourse;
    private Course archivedCourse;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        assertThat(teacher).isNotNull();
        assertThat(admin).isNotNull();
        assertThat(student).isNotNull();

        otherStudent = userAccountRepository.findByEmail("other.student@learnsphere.com");
        if (otherStudent == null) {
            otherStudent = new UserAccount();
            otherStudent.setFullName("Other Student");
            otherStudent.setEmail("other.student@learnsphere.com");
            otherStudent.setPassword("hashedpassword123");
            otherStudent = userAccountRepository.save(otherStudent);
        }

        department = departmentRepository.findByNameIgnoreCase("Computer Science")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Computer Science", null)));

        liveCourse = createCourse("Live Operating Systems", Course.Status.LIVE);
        draftCourse = createCourse("Draft Compiler Design", Course.Status.DRAFT);
        pendingCourse = createCourse("Pending Distributed Systems", Course.Status.PENDING);
        archivedCourse = createCourse("Archived Legacy Networks", Course.Status.ARCHIVED);
    }

    private Course createCourse(String title, Course.Status status) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription("Description for " + title);
        course.setTeacher(teacher);
        course.setDepartment(department);
        course.setStatus(status);
        course.setPrice(BigDecimal.ZERO);
        return courseRepository.save(course);
    }

    private RequestPostProcessor asStudent() {
        return user(userDetailsService.loadUserByUsername(student.getEmail()));
    }

    private RequestPostProcessor asOtherStudent() {
        return user(userDetailsService.loadUserByUsername(otherStudent.getEmail()));
    }

    private RequestPostProcessor asAdmin() {
        return user(userDetailsService.loadUserByUsername(admin.getEmail()));
    }

    @Test
    @DisplayName("A student can successfully enroll in a LIVE course for themselves")
    void shouldEnrollSuccessfullyInLiveCourse() throws Exception {
        String body = """
                {
                    "course": {
                        "id": %d
                    }
                }
                """.formatted(liveCourse.getId());

        mockMvc.perform(post("/api/enrollments")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(student.getUserId()))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.course.id").value(liveCourse.getId()));

        assertThat(enrollmentRepository.existsByUserIdAndCourse_Id(student.getUserId(), liveCourse.getId())).isTrue();
    }

    @Test
    @DisplayName("Enrollment rejects non-existent course with 404 NOT_FOUND")
    void shouldRejectEnrollmentInNonExistentCourse() throws Exception {
        String body = """
                {
                    "course": {
                        "id": 999999
                    }
                }
                """;

        mockMvc.perform(post("/api/enrollments")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Enrollment in a DRAFT course is rejected with 400 BAD_REQUEST (AC-10 / BR-08)")
    void shouldRejectEnrollmentInDraftCourse() throws Exception {
        String body = """
                {
                    "course": {
                        "id": %d
                    }
                }
                """.formatted(draftCourse.getId());

        mockMvc.perform(post("/api/enrollments")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Enrollment in a PENDING course is rejected with 400 BAD_REQUEST")
    void shouldRejectEnrollmentInPendingCourse() throws Exception {
        String body = """
                {
                    "course": {
                        "id": %d
                    }
                }
                """.formatted(pendingCourse.getId());

        mockMvc.perform(post("/api/enrollments")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Enrollment in an ARCHIVED course is rejected with 400 BAD_REQUEST")
    void shouldRejectEnrollmentInArchivedCourse() throws Exception {
        String body = """
                {
                    "course": {
                        "id": %d
                    }
                }
                """.formatted(archivedCourse.getId());

        mockMvc.perform(post("/api/enrollments")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Non-admin student attempting to enroll another user is rejected with 403 FORBIDDEN")
    void shouldRejectForeignEnrollmentByNonAdmin() throws Exception {
        String body = """
                {
                    "userId": %d,
                    "course": {
                        "id": %d
                    }
                }
                """.formatted(otherStudent.getUserId(), liveCourse.getId());

        mockMvc.perform(post("/api/enrollments")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Admin can enroll another user into a LIVE course")
    void shouldAllowAdminToEnrollAnotherUser() throws Exception {
        String body = """
                {
                    "userId": %d,
                    "course": {
                        "id": %d
                    }
                }
                """.formatted(otherStudent.getUserId(), liveCourse.getId());

        mockMvc.perform(post("/api/enrollments")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(otherStudent.getUserId()))
                .andExpect(jsonPath("$.course.id").value(liveCourse.getId()));
    }

    @Test
    @DisplayName("Duplicate enrollment with the same (user_id, course_id, role) is rejected with 409 CONFLICT")
    void shouldRejectDuplicateEnrollment() throws Exception {
        String body = """
                {
                    "course": {
                        "id": %d
                    }
                }
                """.formatted(liveCourse.getId());

        // First enrollment succeeds
        mockMvc.perform(post("/api/enrollments")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // Second enrollment attempts duplicate -> 409 Conflict
        mockMvc.perform(post("/api/enrollments")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("Database unique constraint enforces (user_id, course_id, role) uniqueness at persistence layer")
    void shouldEnforceDatabaseUniqueConstraint() {
        Enrollment e1 = new Enrollment();
        e1.setUserId(student.getUserId());
        e1.setCourse(liveCourse);
        e1.setRole("STUDENT");
        e1.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.saveAndFlush(e1);

        Enrollment e2 = new Enrollment();
        e2.setUserId(student.getUserId());
        e2.setCourse(liveCourse);
        e2.setRole("STUDENT");
        e2.setEnrolledAt(LocalDateTime.now());

        assertThatThrownBy(() -> enrollmentRepository.saveAndFlush(e2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
