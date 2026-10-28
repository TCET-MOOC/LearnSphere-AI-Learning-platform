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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W3-02 integration tests:
 * - Roster authorization: course staff / admin only; 404 for foreign ids/students/unauthenticated (IDOR protection)
 * - Roster response Cache-Control: private, no-store (NFR-03)
 * - Unenrol authorization: self or admin only; 404 for foreign ids/teachers (IDOR protection)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RosterAndUnenrollAuthorizationTests extends PostgresIntegrationTest {

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

    private UserAccount courseTeacher;
    private UserAccount foreignTeacher;
    private UserAccount admin;
    private UserAccount student;
    private UserAccount otherStudent;
    private Course course;
    private Enrollment studentEnrollment;

    @BeforeEach
    void setUp() {
        courseTeacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        otherStudent = userAccountRepository.findByEmail("external@learnsphere.com");

        foreignTeacher = userAccountRepository.findByEmail("foreign-teacher-w302@learnsphere.com");
        if (foreignTeacher == null) {
            foreignTeacher = new UserAccount();
            foreignTeacher.setEmail("foreign-teacher-w302@learnsphere.com");
            foreignTeacher.setFullName("Foreign Teacher");
            foreignTeacher.setPassword("$2a$10$dummyHashNotUsedDirectlyInMockAuthTests");
            foreignTeacher.setStatus(UserAccount.Status.ACTIVE);
            foreignTeacher = userAccountRepository.save(foreignTeacher);
        }

        Department department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Engineering W302");
            return departmentRepository.save(d);
        });

        course = new Course();
        course.setTitle("Distributed Systems Architecture");
        course.setDescription("Advanced systems engineering");
        course.setStatus(Course.Status.LIVE);
        course.setTeacher(courseTeacher);
        course.setDepartment(department);
        course.setPrice(BigDecimal.valueOf(199.99));
        course = courseRepository.save(course);

        studentEnrollment = new Enrollment();
        studentEnrollment.setUserId(student.getUserId());
        studentEnrollment.setCourse(course);
        studentEnrollment.setRole("STUDENT");
        studentEnrollment.setEnrolledAt(LocalDateTime.now());
        studentEnrollment = enrollmentRepository.save(studentEnrollment);
    }

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("Roster: Course owner teacher can view course roster and receives private, no-store")
    void teacherOwner_canViewCourseRoster_returns200AndPrivateNoStore() throws Exception {
        mockMvc.perform(get("/api/courses/{id}/enrollments", course.getId())
                        .with(as(courseTeacher.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").value(student.getUserId()))
                .andExpect(jsonPath("$[0].role").value("STUDENT"));
    }

    @Test
    @DisplayName("Roster: Admin can view course roster and receives private, no-store")
    void admin_canViewCourseRoster_returns200AndPrivateNoStore() throws Exception {
        mockMvc.perform(get("/api/courses/{id}/enrollments", course.getId())
                        .with(as(admin.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").value(student.getUserId()));
    }

    @Test
    @DisplayName("Roster: Foreign teacher cannot view course roster (returns 404 per IDOR rule)")
    void foreignTeacher_cannotViewCourseRoster_returns404() throws Exception {
        mockMvc.perform(get("/api/courses/{id}/enrollments", course.getId())
                        .with(as(foreignTeacher.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Roster: Student cannot view course roster (returns 404 per IDOR rule)")
    void student_cannotViewCourseRoster_returns404() throws Exception {
        mockMvc.perform(get("/api/courses/{id}/enrollments", course.getId())
                        .with(as(student.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Roster: Unauthenticated request to course roster returns 404 (IDOR masking)")
    void unauthenticated_cannotViewCourseRoster_returns404() throws Exception {
        mockMvc.perform(get("/api/courses/{id}/enrollments", course.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Roster: Non-existent course id returns 404")
    void nonExistentCourse_cannotViewCourseRoster_returns404() throws Exception {
        mockMvc.perform(get("/api/courses/{id}/enrollments", 999999L)
                        .with(as(courseTeacher.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Unenrol: Enrolled student can unenroll themselves (returns 204)")
    void enrolledStudent_canUnenrollSelf_returns204() throws Exception {
        mockMvc.perform(delete("/api/enrollments/{id}", studentEnrollment.getEnrollmentId())
                        .with(as(student.getEmail())))
                .andExpect(status().isNoContent());

        assertThat(enrollmentRepository.findById(studentEnrollment.getEnrollmentId())).isEmpty();
    }

    @Test
    @DisplayName("Unenrol: Admin can unenroll any student (returns 204)")
    void admin_canUnenrollStudent_returns204() throws Exception {
        mockMvc.perform(delete("/api/enrollments/{id}", studentEnrollment.getEnrollmentId())
                        .with(as(admin.getEmail())))
                .andExpect(status().isNoContent());

        assertThat(enrollmentRepository.findById(studentEnrollment.getEnrollmentId())).isEmpty();
    }

    @Test
    @DisplayName("Unenrol: Foreign student cannot unenroll another student (returns 404 per IDOR rule)")
    void foreignStudent_cannotUnenrollOtherStudent_returns404() throws Exception {
        mockMvc.perform(delete("/api/enrollments/{id}", studentEnrollment.getEnrollmentId())
                        .with(as(otherStudent.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        assertThat(enrollmentRepository.findById(studentEnrollment.getEnrollmentId())).isPresent();
    }

    @Test
    @DisplayName("Unenrol: Course owner teacher cannot unenroll student (self or admin only, returns 404)")
    void teacherOwner_cannotUnenrollStudent_returns404() throws Exception {
        mockMvc.perform(delete("/api/enrollments/{id}", studentEnrollment.getEnrollmentId())
                        .with(as(courseTeacher.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        assertThat(enrollmentRepository.findById(studentEnrollment.getEnrollmentId())).isPresent();
    }

    @Test
    @DisplayName("Unenrol: Foreign teacher cannot unenroll student (returns 404)")
    void foreignTeacher_cannotUnenrollStudent_returns404() throws Exception {
        mockMvc.perform(delete("/api/enrollments/{id}", studentEnrollment.getEnrollmentId())
                        .with(as(foreignTeacher.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        assertThat(enrollmentRepository.findById(studentEnrollment.getEnrollmentId())).isPresent();
    }

    @Test
    @DisplayName("Unenrol: Non-existent enrollment returns 404")
    void nonExistentEnrollment_returns404() throws Exception {
        mockMvc.perform(delete("/api/enrollments/{id}", 888888L)
                        .with(as(student.getEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
