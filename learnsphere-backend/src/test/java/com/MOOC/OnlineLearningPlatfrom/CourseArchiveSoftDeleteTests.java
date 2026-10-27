package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W2-03 integration tests: Course archive/soft-delete and restore.
 * Replaces hand-written destructive hard cascade with soft-delete preserving historical records.
 * Rules: [B-F5], spec §5 soft-delete.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourseArchiveSoftDeleteTests extends PostgresIntegrationTest {

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
    private ModuleRepository moduleRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private UserAccount teacher;
    private UserAccount otherTeacher;
    private UserAccount admin;
    private UserAccount student;
    private Department department;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        otherTeacher = userAccountRepository.findByEmail("pending@learnsphere.com");

        assertThat(teacher).isNotNull();
        assertThat(admin).isNotNull();
        assertThat(student).isNotNull();
        assertThat(otherTeacher).isNotNull();

        department = departmentRepository.findByNameIgnoreCase("Computer Science")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Computer Science", null)));
    }

    private RequestPostProcessor asTeacher() {
        return user(userDetailsService.loadUserByUsername(teacher.getEmail()));
    }

    private RequestPostProcessor asOtherTeacher() {
        return user(userDetailsService.loadUserByUsername(otherTeacher.getEmail()));
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
        course.setDescription("Description for " + title);
        course.setTeacher(teacher);
        course.setDepartment(department);
        course.setStatus(status);
        course.setPrice(BigDecimal.ZERO);
        return courseRepository.save(course);
    }

    @Test
    @DisplayName("Teacher deleting course performs soft-delete (status becomes ARCHIVED, row preserved)")
    void shouldSoftDeleteCourseInsteadOfHardCascade() throws Exception {
        Course course = createCourse("Course To Archive", Course.Status.LIVE);

        mockMvc.perform(delete("/api/courses/" + course.getId())
                        .with(asTeacher()))
                .andExpect(status().isNoContent());

        Course archived = courseRepository.findById(course.getId()).orElse(null);
        assertThat(archived).isNotNull();
        assertThat(archived.getStatus()).isEqualTo(Course.Status.ARCHIVED);
        assertThat(archived.getIsPublished()).isFalse();
    }

    @Test
    @DisplayName("Soft-delete preserves modules, lectures, and enrollments (no hard cascade)")
    void shouldPreserveRelatedEntitiesOnSoftDelete() throws Exception {
        Course course = createCourse("Course With Relations", Course.Status.LIVE);

        // Add module
        com.MOOC.OnlineLearningPlatfrom.Entity.Module module = new com.MOOC.OnlineLearningPlatfrom.Entity.Module();
        module.setCourse(course);
        module.setTitle("Module 1");
        module.setPosition(1);
        module = moduleRepository.save(module);

        // Add lecture
        Lecture lecture = new Lecture();
        lecture.setCourse(course);
        lecture.setTitle("Lecture 1");
        lecture.setNumber(1);
        lecture.setDuration(600);
        lecture = lectureRepository.save(lecture);

        // Add enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUserId(student.getUserId());
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment = enrollmentRepository.save(enrollment);

        // Perform delete
        mockMvc.perform(delete("/api/courses/" + course.getId())
                        .with(asTeacher()))
                .andExpect(status().isNoContent());

        // Assert all rows remain in database
        assertThat(courseRepository.findById(course.getId())).isPresent();
        assertThat(courseRepository.findById(course.getId()).get().getStatus()).isEqualTo(Course.Status.ARCHIVED);
        assertThat(moduleRepository.findById(module.getModuleId())).isPresent();
        assertThat(lectureRepository.findById(lecture.getId())).isPresent();
        assertThat(enrollmentRepository.findById(enrollment.getEnrollmentId())).isPresent();
    }

    @Test
    @DisplayName("Restoring an archived course transitions status to DRAFT")
    void shouldRestoreArchivedCourseToDraft() throws Exception {
        Course course = createCourse("Course To Restore", Course.Status.ARCHIVED);

        mockMvc.perform(post("/api/courses/" + course.getId() + "/restore")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.isPublished").value(false));

        Course restored = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(Course.Status.DRAFT);
    }

    @Test
    @DisplayName("Restoring a non-archived course returns 400 Bad Request")
    void shouldRejectRestoreForNonArchivedCourse() throws Exception {
        Course course = createCourse("Active Course", Course.Status.LIVE);

        mockMvc.perform(post("/api/courses/" + course.getId() + "/restore")
                        .with(asTeacher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Foreign teacher cannot archive or restore course (IDOR 404)")
    void shouldRejectForeignTeacherArchiveAndRestore() throws Exception {
        Course course = createCourse("Protected Teacher Course", Course.Status.LIVE);

        // Foreign teacher attempt delete
        mockMvc.perform(delete("/api/courses/" + course.getId())
                        .with(asOtherTeacher()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Set to ARCHIVED
        course.setStatus(Course.Status.ARCHIVED);
        courseRepository.save(course);

        // Foreign teacher attempt restore
        mockMvc.perform(post("/api/courses/" + course.getId() + "/restore")
                        .with(asOtherTeacher()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Admin can archive and restore any teacher's course")
    void shouldAllowAdminToArchiveAndRestoreCourse() throws Exception {
        Course course = createCourse("Course Moderated By Admin", Course.Status.LIVE);

        // Admin archives
        mockMvc.perform(delete("/api/courses/" + course.getId())
                        .with(asAdmin()))
                .andExpect(status().isNoContent());

        assertThat(courseRepository.findById(course.getId()).orElseThrow().getStatus())
                .isEqualTo(Course.Status.ARCHIVED);

        // Admin restores
        mockMvc.perform(post("/api/courses/" + course.getId() + "/restore")
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        assertThat(courseRepository.findById(course.getId()).orElseThrow().getStatus())
                .isEqualTo(Course.Status.DRAFT);
    }

    @Test
    @DisplayName("Archived course is excluded from public catalog and hidden from public detail (404)")
    void shouldExcludeArchivedCourseFromPublicCatalogAndDetail() throws Exception {
        Course archived = createCourse("Archived Course Inactive", Course.Status.ARCHIVED);

        // Excluded from public catalog
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title", not(hasItem(archived.getTitle()))));

        // Public detail returns 404
        mockMvc.perform(get("/api/courses/" + archived.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Student detail returns 404
        mockMvc.perform(get("/api/courses/" + archived.getId())
                        .with(asStudent()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Owner teacher detail returns 200 with status ARCHIVED
        mockMvc.perform(get("/api/courses/" + archived.getId())
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.isPublished").value(false));
    }
}
