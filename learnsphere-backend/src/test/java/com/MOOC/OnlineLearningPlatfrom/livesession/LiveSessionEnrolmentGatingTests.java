package com.MOOC.OnlineLearningPlatfrom.livesession;

import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Story W5-07: Live session enrolment gating
 * Rules: A-18, [E-F16], NFR-03
 *
 * Scenarios:
 * 1. Enrolled student can get session details (GET /api/student/live-sessions/{id}) -> 200 OK + Cache-Control: private, no-store.
 * 2. Unenrolled student getting session details -> 404 NOT_FOUND.
 * 3. Course instructor can access student session endpoint -> 200 OK.
 * 4. Admin can access student session endpoint -> 200 OK.
 * 5. Unauthenticated request to /api/student/live-sessions/{id} -> 401 UNAUTHORIZED.
 * 6. Student session feed (GET /api/student/live-sessions) lists only sessions for enrolled courses.
 * 7. Student with no enrollments gets an empty list from student feed (no leak of all sessions).
 * 8. Course teacher can start session -> 200 OK (status LIVE).
 * 9. Foreign teacher attempting to start/end session -> 404 NOT_FOUND.
 * 10. Student attempting to start session -> 403 FORBIDDEN.
 * 11. Foreign teacher attempting to create session in another teacher's course -> 404 NOT_FOUND.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LiveSessionEnrolmentGatingTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private UserAccount teacher;
    private UserAccount foreignTeacher;
    private UserAccount studentEnrolled;
    private UserAccount studentUnenrolled;
    private UserAccount admin;

    private Course courseA;
    private Course courseB;

    private LiveSession sessionCourseA;
    private LiveSession sessionCourseB;

    @BeforeEach
    void setUp() {
        liveSessionRepository.deleteAll();
        enrollmentRepository.deleteAll();

        teacher = findOrCreateUser("live.teacher@learnsphere.com", "Live Teacher", "TEACHER");
        foreignTeacher = findOrCreateUser("live.foreign@learnsphere.com", "Foreign Teacher", "TEACHER");
        studentEnrolled = findOrCreateUser("live.enrolled@learnsphere.com", "Enrolled Student", "STUDENT");
        studentUnenrolled = findOrCreateUser("live.unenrolled@learnsphere.com", "Unenrolled Student", "STUDENT");
        admin = findOrCreateUser("live.admin@learnsphere.com", "Live Admin", "ADMIN");

        courseA = new Course();
        courseA.setTitle("Course A for Live");
        courseA.setTeacher(teacher);
        courseA.setIsPublished(true);
        courseA = courseRepository.save(courseA);

        courseB = new Course();
        courseB.setTitle("Course B for Live");
        courseB.setTeacher(foreignTeacher);
        courseB.setIsPublished(true);
        courseB = courseRepository.save(courseB);

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(courseA);
        enrollment.setUserId(studentEnrolled.getUserId());
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);

        sessionCourseA = new LiveSession();
        sessionCourseA.setCourse(courseA);
        sessionCourseA.setTeacher(teacher);
        sessionCourseA.setTitle("Intro to Live A");
        sessionCourseA.setScheduledAt(LocalDateTime.now().plusDays(1));
        sessionCourseA.setStatus(LiveSession.Status.SCHEDULED);
        sessionCourseA = liveSessionRepository.save(sessionCourseA);

        sessionCourseB = new LiveSession();
        sessionCourseB.setCourse(courseB);
        sessionCourseB.setTeacher(foreignTeacher);
        sessionCourseB.setTitle("Intro to Live B");
        sessionCourseB.setScheduledAt(LocalDateTime.now().plusDays(2));
        sessionCourseB.setStatus(LiveSession.Status.SCHEDULED);
        sessionCourseB = liveSessionRepository.save(sessionCourseB);
    }

    private RequestPostProcessor asUser(UserAccount account) {
        var userDetails = userDetailsService.loadUserByUsername(account.getEmail());
        return user(userDetails);
    }

    private UserAccount findOrCreateUser(String email, String name, String roleName) {
        UserAccount existing = userAccountRepository.findByEmail(email);
        if (existing != null) {
            return existing;
        }
        UserAccount u = new UserAccount();
        u.setEmail(email);
        u.setFullName(name);
        u.setPassword("hashedpassword");
        u.setStatus(UserAccount.Status.ACTIVE);
        u = userAccountRepository.save(u);

        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            role = new Role();
            role.setName(roleName);
            role = roleRepository.save(role);
        }

        UserRole ur = new UserRole();
        ur.setUser(u);
        ur.setRole(role);
        userRoleRepository.save(ur);
        return u;
    }

    @Test
    @DisplayName("Scenario 1: Enrolled student can get session details -> 200 OK + Cache-Control: private, no-store")
    void enrolledStudentCanGetSessionDetails() throws Exception {
        mockMvc.perform(get("/api/student/live-sessions/{id}", sessionCourseA.getId())
                        .with(asUser(studentEnrolled)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.id").value(sessionCourseA.getId()))
                .andExpect(jsonPath("$.courseId").value(courseA.getId()))
                .andExpect(jsonPath("$.title").value("Intro to Live A"));
    }

    @Test
    @DisplayName("Scenario 2: Unenrolled student gets 404 NOT_FOUND on session details (IDOR protection)")
    void unenrolledStudentGets404OnSessionDetails() throws Exception {
        mockMvc.perform(get("/api/student/live-sessions/{id}", sessionCourseB.getId())
                        .with(asUser(studentEnrolled))) // enrolled in A, not B
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/student/live-sessions/{id}", sessionCourseA.getId())
                        .with(asUser(studentUnenrolled))) // not enrolled in A
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 3: Course instructor can access student session endpoint -> 200 OK")
    void courseInstructorCanAccessSession() throws Exception {
        mockMvc.perform(get("/api/student/live-sessions/{id}", sessionCourseA.getId())
                        .with(asUser(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionCourseA.getId()));
    }

    @Test
    @DisplayName("Scenario 4: Admin can access student session endpoint -> 200 OK")
    void adminCanAccessSession() throws Exception {
        mockMvc.perform(get("/api/student/live-sessions/{id}", sessionCourseA.getId())
                        .with(asUser(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionCourseA.getId()));
    }

    @Test
    @DisplayName("Scenario 5: Unauthenticated request to /api/student/live-sessions/{id} -> 401 UNAUTHORIZED")
    void unauthenticatedRequestRejected() throws Exception {
        mockMvc.perform(get("/api/student/live-sessions/{id}", sessionCourseA.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario 6: Student session feed lists only sessions for enrolled courses")
    void studentSessionFeedIsEnrolmentGated() throws Exception {
        mockMvc.perform(get("/api/student/live-sessions")
                        .with(asUser(studentEnrolled)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(sessionCourseA.getId()))
                .andExpect(jsonPath("$[0].courseId").value(courseA.getId()));
    }

    @Test
    @DisplayName("Scenario 7: Student with no enrollments gets an empty list from student feed")
    void unenrolledStudentGetsEmptyFeed() throws Exception {
        mockMvc.perform(get("/api/student/live-sessions")
                        .with(asUser(studentUnenrolled)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Scenario 8: Course teacher can start session -> 200 OK (status LIVE)")
    void courseTeacherCanStartSession() throws Exception {
        mockMvc.perform(put("/api/teacher/live-sessions/{id}/start", sessionCourseA.getId())
                        .with(asUser(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sessionCourseA.getId()))
                .andExpect(jsonPath("$.status").value("LIVE"));
    }

    @Test
    @DisplayName("Scenario 9: Foreign teacher attempting to start/end session -> 404 NOT_FOUND")
    void foreignTeacherCannotStartOrEndSession() throws Exception {
        mockMvc.perform(put("/api/teacher/live-sessions/{id}/start", sessionCourseA.getId())
                        .with(asUser(foreignTeacher)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/teacher/live-sessions/{id}/end", sessionCourseA.getId())
                        .with(asUser(foreignTeacher)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 10: Student attempting to start session -> 403 FORBIDDEN")
    void studentCannotStartSession() throws Exception {
        mockMvc.perform(put("/api/teacher/live-sessions/{id}/start", sessionCourseA.getId())
                        .with(asUser(studentEnrolled)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 11: Foreign teacher attempting to create session in another teacher's course -> 404 NOT_FOUND")
    void foreignTeacherCannotCreateSessionInOtherCourse() throws Exception {
        LiveSessionRequestDto dto = new LiveSessionRequestDto();
        dto.setCourseId(courseA.getId());
        dto.setTitle("Hacked Session");
        dto.setScheduledAt(LocalDateTime.now().plusDays(3));

        mockMvc.perform(post("/api/teacher/live-sessions")
                        .with(asUser(foreignTeacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }
}
