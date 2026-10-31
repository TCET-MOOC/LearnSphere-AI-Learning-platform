package com.MOOC.OnlineLearningPlatfrom.announcement;

import com.MOOC.OnlineLearningPlatfrom.Entity.Announcement;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Role;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserRole;
import com.MOOC.OnlineLearningPlatfrom.Repository.AnnouncementRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Story W5-05: Course-scoped announcements
 * Rules: A-17, [E-F13], NFR-03
 *
 * Scenarios:
 * 1. Teacher cannot create global announcement (courseId == null) -> 400 BAD_REQUEST.
 * 2. Teacher can create announcement in their own course -> 200 OK.
 * 3. Teacher cannot create announcement in another teacher's course -> 404 NOT_FOUND.
 * 4. Admin can create global (courseId == null) and course-scoped announcements.
 * 5. Student cannot create announcements -> 403 FORBIDDEN.
 * 6. No cross-course broadcast: Enrolled student in Course A sees Course A and global announcements, but NOT Course B announcements.
 * 7. Unenrolled student access to course announcements returns 404 NOT_FOUND.
 * 8. Expiry: Past expiresAt announcements are excluded from feeds; future expiresAt announcements are included.
 * 9. Pinning: Pinned announcements precede unpinned ones.
 * 10. Cache-Control: private, no-store headers on responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CourseScopedAnnouncementsTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AnnouncementRepository announcementRepository;

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

    private UserAccount teacher1;
    private UserAccount teacher2;
    private UserAccount student1;
    private UserAccount student2;
    private UserAccount admin;

    private Course courseA;
    private Course courseB;

    @BeforeEach
    public void setup() {
        announcementRepository.deleteAll();
        enrollmentRepository.deleteAll();

        teacher1 = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student1 = userAccountRepository.findByEmail("student@learnsphere.com");
        student2 = userAccountRepository.findByEmail("external@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");

        teacher2 = userAccountRepository.findByEmail("foreign.teacher@learnsphere.com");
        if (teacher2 == null) {
            teacher2 = new UserAccount();
            teacher2.setEmail("foreign.teacher@learnsphere.com");
            teacher2.setPassword("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEF");
            teacher2.setFullName("Foreign Teacher");
            teacher2.setStatus(UserAccount.Status.ACTIVE);
            teacher2 = userAccountRepository.save(teacher2);

            Role teacherRole = roleRepository.findByName("TEACHER");
            if (teacherRole == null) {
                teacherRole = new Role();
                teacherRole.setName("TEACHER");
                teacherRole = roleRepository.save(teacherRole);
            }
            UserRole ur = new UserRole();
            ur.setUser(teacher2);
            ur.setRole(teacherRole);
            userRoleRepository.save(ur);
        }

        // Course A taught by teacher1
        courseA = new Course();
        courseA.setTitle("Distributed Systems 101");
        courseA.setDescription("Core distributed algorithms");
        courseA.setTeacher(teacher1);
        courseA.setStatus(Course.Status.LIVE);
        courseA = courseRepository.save(courseA);

        // Course B taught by teacher2
        courseB = new Course();
        courseB.setTitle("Compiler Construction 201");
        courseB.setDescription("Parsers and code generators");
        courseB.setTeacher(teacher2);
        courseB.setStatus(Course.Status.LIVE);
        courseB = courseRepository.save(courseB);

        // Enroll student1 in Course A
        Enrollment enA = new Enrollment();
        enA.setUserId(student1.getUserId());
        enA.setCourse(courseA);
        enA.setRole("STUDENT");
        enrollmentRepository.save(enA);

        // Enroll student2 in Course B
        Enrollment enB = new Enrollment();
        enB.setUserId(student2.getUserId());
        enB.setCourse(courseB);
        enB.setRole("STUDENT");
        enrollmentRepository.save(enB);
    }

    private RequestPostProcessor auth(UserAccount u) {
        return user(userDetailsService.loadUserByUsername(u.getEmail()));
    }

    @Test
    @DisplayName("Scenario 1: Teacher cannot create global announcement (missing courseId) -> 400 Bad Request")
    public void testTeacherCannotCreateGlobalAnnouncement() throws Exception {
        mockMvc.perform(post("/api/announcements")
                        .with(auth(teacher1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Global Broadcast Attempt",
                                "body", "This should fail because teacher announcements must be course-scoped",
                                "category", "General"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsStringIgnoringCase("course")));
    }

    @Test
    @DisplayName("Scenario 2: Teacher can create announcement in their own course -> 200 OK")
    public void testTeacherCanCreateCourseScopedAnnouncement() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/announcements")
                        .with(auth(teacher1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Midterm Exam Announced",
                                "body", "The midterm exam will take place next Monday",
                                "category", "Exam",
                                "courseId", courseA.getCourseId()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Midterm Exam Announced"))
                .andExpect(jsonPath("$.courseId").value(courseA.getCourseId()))
                .andExpect(jsonPath("$.courseTitle").value("Distributed Systems 101"))
                .andReturn();

        Long id = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
        Announcement saved = announcementRepository.findByIdWithDetails(id).orElseThrow();
        assertThat(saved.getCourse()).isNotNull();
        assertThat(saved.getCourse().getCourseId()).isEqualTo(courseA.getCourseId());
    }

    @Test
    @DisplayName("Scenario 3: Teacher cannot create announcement in another teacher's course -> 404 Not Found")
    public void testTeacherCannotCreateInForeignCourse() throws Exception {
        // Teacher 1 tries to post in Course B (owned by Teacher 2)
        mockMvc.perform(post("/api/announcements")
                        .with(auth(teacher1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Rogue Announcement",
                                "body", "Invading another course",
                                "courseId", courseB.getCourseId()
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 4: Admin can create global and course-scoped announcements -> 200 OK")
    public void testAdminCanCreateGlobalAndCourseScopedAnnouncements() throws Exception {
        // Global announcement (courseId null)
        mockMvc.perform(post("/api/announcements")
                        .with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Platform Maintenance",
                                "body", "Scheduled downtime Sunday at 2 AM UTC",
                                "category", "System"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Platform Maintenance"))
                .andExpect(jsonPath("$.courseId").value(nullValue()));

        // Course-scoped announcement by admin
        mockMvc.perform(post("/api/announcements")
                        .with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Admin Course Note",
                                "body", "Special note from administration",
                                "courseId", courseA.getCourseId()
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin Course Note"))
                .andExpect(jsonPath("$.courseId").value(courseA.getCourseId()));
    }

    @Test
    @DisplayName("Scenario 5: Student cannot create announcements -> 403 Forbidden")
    public void testStudentCannotCreateAnnouncements() throws Exception {
        mockMvc.perform(post("/api/announcements")
                        .with(auth(student1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Student Announcement",
                                "body", "Hello world",
                                "courseId", courseA.getCourseId()
                        ))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 6: No cross-course broadcast — student in Course A sees Course A and global announcements, but NOT Course B")
    public void testNoCrossCourseBroadcast() throws Exception {
        // Teacher 1 posts in Course A
        mockMvc.perform(post("/api/announcements")
                        .with(auth(teacher1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Course A Assignment 1",
                                "body", "Assignment 1 is posted",
                                "courseId", courseA.getCourseId()
                        ))))
                .andExpect(status().isOk());

        // Teacher 2 posts in Course B
        mockMvc.perform(post("/api/announcements")
                        .with(auth(teacher2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Course B Project Due",
                                "body", "Project deadline is Friday",
                                "courseId", courseB.getCourseId()
                        ))))
                .andExpect(status().isOk());

        // Admin posts global announcement
        mockMvc.perform(post("/api/announcements")
                        .with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Global Welcome Message",
                                "body", "Welcome to the term"
                        ))))
                .andExpect(status().isOk());

        // Student 1 (enrolled in A only) fetches /api/announcements
        mockMvc.perform(get("/api/announcements")
                        .with(auth(student1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", hasItems("Course A Assignment 1", "Global Welcome Message")))
                .andExpect(jsonPath("$[*].title", not(hasItem("Course B Project Due"))));

        // Student 2 (enrolled in B only) fetches /api/announcements
        mockMvc.perform(get("/api/announcements")
                        .with(auth(student2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", hasItems("Course B Project Due", "Global Welcome Message")))
                .andExpect(jsonPath("$[*].title", not(hasItem("Course A Assignment 1"))));
    }

    @Test
    @DisplayName("Scenario 7: Unenrolled student cannot access course announcements -> 404 Not Found")
    public void testUnenrolledStudentCourseAnnouncementsForbidden() throws Exception {
        // Student 1 is not enrolled in Course B -> 404 (IDOR protection)
        mockMvc.perform(get("/api/courses/{courseId}/announcements", courseB.getCourseId())
                        .with(auth(student1)))
                .andExpect(status().isNotFound());

        // Student 1 is enrolled in Course A -> 200 OK
        mockMvc.perform(get("/api/courses/{courseId}/announcements", courseA.getCourseId())
                        .with(auth(student1)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Scenario 8: Expiry filtering — expired announcements are omitted, future ones included")
    public void testExpirationFiltering() throws Exception {
        // Past announcement (expired 1 day ago)
        mockMvc.perform(post("/api/announcements")
                        .with(auth(teacher1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Old Announcement",
                                "body", "Expired already",
                                "courseId", courseA.getCourseId(),
                                "expiresAt", LocalDateTime.now().minusDays(1).toString()
                        ))))
                .andExpect(status().isOk());

        // Future announcement (expires in 2 days)
        mockMvc.perform(post("/api/announcements")
                        .with(auth(teacher1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Active Future Announcement",
                                "body", "Still active",
                                "courseId", courseA.getCourseId(),
                                "expiresAt", LocalDateTime.now().plusDays(2).toString()
                        ))))
                .andExpect(status().isOk());

        // Fetch announcements for Course A
        mockMvc.perform(get("/api/courses/{courseId}/announcements", courseA.getCourseId())
                        .with(auth(student1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Active Future Announcement"));
    }

    @Test
    @DisplayName("Scenario 9: Pinning precedence — pinned announcements appear before unpinned")
    public void testPinningPrecedence() throws Exception {
        // Unpinned announcement created first
        mockMvc.perform(post("/api/announcements")
                        .with(auth(teacher1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Unpinned Announcement",
                                "body", "Normal announcement",
                                "courseId", courseA.getCourseId(),
                                "pinned", false
                        ))))
                .andExpect(status().isOk());

        // Pinned announcement created second
        mockMvc.perform(post("/api/announcements")
                        .with(auth(teacher1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Pinned Urgent Notice",
                                "body", "Important announcement",
                                "courseId", courseA.getCourseId(),
                                "pinned", true
                        ))))
                .andExpect(status().isOk());

        // Fetch announcements: pinned must appear first
        mockMvc.perform(get("/api/courses/{courseId}/announcements", courseA.getCourseId())
                        .with(auth(student1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Pinned Urgent Notice"))
                .andExpect(jsonPath("$[0].pinned").value(true))
                .andExpect(jsonPath("$[1].title").value("Unpinned Announcement"))
                .andExpect(jsonPath("$[1].pinned").value(false));
    }

    @Test
    @DisplayName("Scenario 10: Cache-Control header — returns private, no-store")
    public void testCacheControlHeaders() throws Exception {
        mockMvc.perform(get("/api/announcements")
                        .with(auth(student1)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }
}
