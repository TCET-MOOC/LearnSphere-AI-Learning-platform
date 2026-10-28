package com.MOOC.OnlineLearningPlatfrom.learning;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W3-11 / AUD-08: Canonical player route + server enrolment check.
 *
 * Rules:
 * - audit.md C2, C4, C5, C7 (AUD-08);
 * - BR-23, BR-26, A-05, AC-10, W2-12, NFR-03.
 *
 * Validates:
 * 1. GET /api/courses/{id}/lectures rejects unauthenticated requests with 404 (stealth IDOR).
 * 2. GET /api/courses/{id}/lectures rejects un-enrolled students with 404.
 * 3. GET /api/courses/{id}/lectures allows enrolled students with Cache-Control: private, no-store.
 * 4. GET /api/courses/{id}/lectures allows course teacher and admin with Cache-Control: private, no-store.
 * 5. GET /api/courses/{id}/lectures rejects foreign teachers with 404.
 * 6. GET /api/student/courses/{courseId}/lectures/{lectureId} rejects un-enrolled students with 404.
 * 7. GET /api/student/courses/{courseId}/lectures/{lectureId} allows enrolled student with Cache-Control: private, no-store.
 * 8. GET /api/student/courses/{courseId}/lectures/{lectureId} rejects course mismatch with 404.
 * 9. Lecture responses provide contentId and delegate media playback to W2-12 descriptor without exposing raw server paths.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PlayerEnrolmentSecurityTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private CourseContentRepository courseContentRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    private UserAccount teacher;
    private UserAccount foreignTeacher;
    private UserAccount student;
    private UserAccount unenrolledStudent;
    private UserAccount admin;
    private Course course;
    private Lecture lecture1;
    private Lecture lecture2;
    private CourseContent content1;

    @BeforeEach
    void setUp() {
        Department dept = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Software Systems " + System.currentTimeMillis());
            return departmentRepository.save(d);
        });

        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        unenrolledStudent = userAccountRepository.findByEmail("external@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");

        foreignTeacher = userAccountRepository.findByEmail("foreign.teacher@learnsphere.com");
        if (foreignTeacher == null) {
            foreignTeacher = new UserAccount();
            foreignTeacher.setEmail("foreign.teacher@learnsphere.com");
            foreignTeacher.setPassword("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF");
            foreignTeacher.setFullName("Foreign Teacher");
            foreignTeacher.setStatus(UserAccount.Status.ACTIVE);
            foreignTeacher.setDepartment(dept);
            foreignTeacher = userAccountRepository.save(foreignTeacher);

            Role teacherRole = roleRepository.findByName("TEACHER");
            if (teacherRole == null) {
                teacherRole = new Role();
                teacherRole.setName("TEACHER");
                teacherRole = roleRepository.save(teacherRole);
            }
            UserRole userRole = new UserRole();
            userRole.setUser(foreignTeacher);
            userRole.setRole(teacherRole);
            userRoleRepository.save(userRole);
        }

        course = new Course();
        course.setTitle("Distributed Systems Architecture");
        course.setDescription("Production distributed systems");
        course.setTeacher(teacher);
        course.setDepartment(dept);
        course.setPrice(BigDecimal.ZERO);
        course.setStatus(Course.Status.LIVE);
        course.setIsPublished(true);
        course = courseRepository.save(course);

        Module module = new Module();
        module.setCourse(course);
        module.setTitle("Module 1: Foundations");
        module.setPosition(1);
        module = moduleRepository.save(module);

        lecture1 = new Lecture();
        lecture1.setCourse(course);
        lecture1.setTitle("01: Consensus Protocols");
        lecture1.setNumber(1);
        lecture1.setDuration(1200);
        lecture1.setVideoUrl("https://www.youtube.com/watch?v=consensus123");
        lecture1 = lectureRepository.save(lecture1);

        content1 = new CourseContent();
        content1.setModule(module);
        content1.setTitle(lecture1.getTitle());
        content1.setPosition(1);
        content1.setContentType("VIDEO");
        content1.setSourceType("YOUTUBE");
        content1.setProviderVideoId("consensus123");
        content1.setDurationSeconds(1200);
        content1.setLegacyLectureId(lecture1.getId());
        content1 = courseContentRepository.save(content1);

        lecture2 = new Lecture();
        lecture2.setCourse(course);
        lecture2.setTitle("02: Storage Engines");
        lecture2.setNumber(2);
        lecture2.setDuration(1800);
        lecture2.setVideoUrl("uploads/private/disk_storage_internal.mp4");
        lecture2 = lectureRepository.save(lecture2);

        CourseContent content2 = new CourseContent();
        content2.setModule(module);
        content2.setTitle(lecture2.getTitle());
        content2.setPosition(2);
        content2.setContentType("VIDEO");
        content2.setSourceType("UPLOAD");
        content2.setStorageKey("disk_storage_key");
        content2.setFilePath("disk_storage_internal.mp4");
        content2.setDurationSeconds(1800);
        content2.setLegacyLectureId(lecture2.getId());
        courseContentRepository.save(content2);

        // Enroll student
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUserId(student.getUserId());
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
    }

    @Test
    @DisplayName("GET /api/courses/{id}/lectures rejects unauthenticated calls with 404")
    void unauthenticatedCallingCourseLectures_returns404() throws Exception {
        mockMvc.perform(get("/api/courses/" + course.getId() + "/lectures"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/courses/{id}/lectures rejects unenrolled student with 404 (stealth security)")
    void unenrolledStudentCallingCourseLectures_returns404() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(unenrolledStudent.getEmail());

        mockMvc.perform(get("/api/courses/" + course.getId() + "/lectures")
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/courses/{id}/lectures rejects foreign teacher with 404 (IDOR stealth)")
    void foreignTeacherCallingCourseLectures_returns404() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(foreignTeacher.getEmail());

        mockMvc.perform(get("/api/courses/" + course.getId() + "/lectures")
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/courses/{id}/lectures allows enrolled student with Cache-Control: private, no-store")
    void enrolledStudentCallingCourseLectures_returnsLecturesWithPrivateNoStore() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(student.getEmail());

        mockMvc.perform(get("/api/courses/" + course.getId() + "/lectures")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("01: Consensus Protocols"))
                .andExpect(jsonPath("$[0].contentId").value(content1.getContentId()))
                // Raw server path should not be leaked as videoUrl
                .andExpect(jsonPath("$[1].videoUrl", not(containsString("uploads/private"))));
    }

    @Test
    @DisplayName("GET /api/courses/{id}/lectures allows course teacher with Cache-Control: private, no-store")
    void courseTeacherCallingCourseLectures_returnsLecturesWithPrivateNoStore() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(teacher.getEmail());

        mockMvc.perform(get("/api/courses/" + course.getId() + "/lectures")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/courses/{id}/lectures allows admin with Cache-Control: private, no-store")
    void adminCallingCourseLectures_returnsLecturesWithPrivateNoStore() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(admin.getEmail());

        mockMvc.perform(get("/api/courses/" + course.getId() + "/lectures")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /api/student/courses/{courseId}/lectures/{lectureId} rejects unenrolled student with 404")
    void unenrolledStudentCallingStudentLectureDetail_returns404() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(unenrolledStudent.getEmail());

        mockMvc.perform(get("/api/student/courses/" + course.getId() + "/lectures/" + lecture1.getId())
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/student/courses/{courseId}/lectures/{lectureId} allows enrolled student with Cache-Control: private, no-store")
    void enrolledStudentCallingStudentLectureDetail_returnsLectureWithPrivateNoStore() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(student.getEmail());

        mockMvc.perform(get("/api/student/courses/" + course.getId() + "/lectures/" + lecture1.getId())
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.id").value(lecture1.getId()))
                .andExpect(jsonPath("$.title").value("01: Consensus Protocols"))
                .andExpect(jsonPath("$.contentId").value(content1.getContentId()));
    }

    @Test
    @DisplayName("GET /api/student/courses/{courseId}/lectures/{lectureId} rejects course mismatch with 404")
    void studentLectureDetailWithCourseMismatch_returns404() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(student.getEmail());

        // Use a different course ID (999999)
        mockMvc.perform(get("/api/student/courses/999999/lectures/" + lecture1.getId())
                        .with(user(userDetails)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Enrolled student delegates playback to W2-12 descriptor without leaking raw storage paths")
    void enrolledStudentCanFetchPlaybackDescriptor_withoutLeakingRawPaths() throws Exception {
        var userDetails = userDetailsService.loadUserByUsername(student.getEmail());

        mockMvc.perform(get("/api/contents/" + content1.getContentId() + "/playback")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.contentId").value(content1.getContentId()))
                .andExpect(jsonPath("$.sourceType").value("YOUTUBE"))
                .andExpect(jsonPath("$.providerVideoId").value("consensus123"));
    }
}
