package com.MOOC.OnlineLearningPlatfrom.discussion;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.DiscussionThread;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Post;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DiscussionThreadRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PostRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W5-02: Enrolment-Gated Community Reads and Writes Tests.
 *
 * Rules: A-14, [E-F3], LMS Entity Reference Section 10.
 *
 * Scenarios:
 * 1. Unauthenticated requests cannot read or write discussions (catalogue hides discussions).
 * 2. Unenrolled student calling GET/POST /api/courses/{id}/threads receives 404 (IDOR defense).
 * 3. Unenrolled student calling GET /api/threads/{id} or /posts receives 404.
 * 4. Unenrolled student calling POST /api/threads/{id}/posts receives 404.
 * 5. Enrolled student can read threads, create threads, read posts, and post replies.
 * 6. Course staff (teacher) has full community read/write access without student enrollment.
 * 7. Admin has full community read/write access across all courses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class EnrolmentGatedCommunityTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private DiscussionThreadRepository discussionThreadRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private UserAccount teacher;
    private UserAccount enrolledStudent;
    private UserAccount unenrolledStudent;
    private UserAccount admin;
    private Course course;
    private DiscussionThread thread;

    @BeforeEach
    public void setup() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        enrolledStudent = userAccountRepository.findByEmail("student@learnsphere.com");
        unenrolledStudent = userAccountRepository.findByEmail("external@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");

        course = new Course();
        course.setTitle("Cloud Native Microservices");
        course.setDescription("Building resilient microservices with Spring Cloud and Kubernetes");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(enrolledStudent.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now().minusDays(2));
        enrollmentRepository.save(enrollment);

        thread = new DiscussionThread();
        thread.setCourse(course);
        thread.setUser(enrolledStudent);
        thread.setTitle("Service Mesh with Istio");
        thread.setContent("How does mTLS handshake perform at high QPS?");
        thread = discussionThreadRepository.save(thread);

        Post p = new Post();
        p.setThread(thread);
        p.setUser(teacher);
        p.setBody("Envoy proxies handle TLS termination with negligible overhead.");
        postRepository.save(p);

        entityManager.flush();
        entityManager.clear();
    }

    private RequestPostProcessor auth(UserAccount u) {
        return user(userDetailsService.loadUserByUsername(u.getEmail()));
    }

    @Test
    @DisplayName("Scenario 1: Public catalogue hides discussions — unauthenticated access rejected")
    public void testPublicCatalogueHidesDiscussions() throws Exception {
        mockMvc.perform(get("/api/courses/" + course.getId() + "/threads"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/threads/" + thread.getThreadId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/threads/" + thread.getThreadId() + "/posts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/courses/" + course.getId() + "/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Topic\",\"content\":\"Body\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario 2: Unenrolled student cannot read or create course threads (returns 404)")
    public void testUnenrolledStudentCourseThreadAccess() throws Exception {
        // Read course threads -> 404 Not Found
        mockMvc.perform(get("/api/courses/" + course.getId() + "/threads")
                        .with(auth(unenrolledStudent)))
                .andExpect(status().isNotFound());

        // Create thread -> 404 Not Found
        mockMvc.perform(post("/api/courses/" + course.getId() + "/threads")
                        .with(auth(unenrolledStudent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Unauthorized Thread",
                                "content", "Should be rejected"
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 3: Unenrolled student cannot access thread or its posts by ID (returns 404)")
    public void testUnenrolledStudentThreadDetailsAccess() throws Exception {
        // Get thread by ID -> 404 Not Found
        mockMvc.perform(get("/api/threads/" + thread.getThreadId())
                        .with(auth(unenrolledStudent)))
                .andExpect(status().isNotFound());

        // Get thread posts -> 404 Not Found
        mockMvc.perform(get("/api/threads/" + thread.getThreadId() + "/posts")
                        .with(auth(unenrolledStudent)))
                .andExpect(status().isNotFound());

        // Post reply -> 404 Not Found
        mockMvc.perform(post("/api/threads/" + thread.getThreadId() + "/posts")
                        .with(auth(unenrolledStudent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Unauthorized Reply"
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 4: Enrolled student can read and write course threads and replies")
    public void testEnrolledStudentCanReadAndWrite() throws Exception {
        // 1. Read course threads
        mockMvc.perform(get("/api/courses/" + course.getId() + "/threads")
                        .with(auth(enrolledStudent)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // 2. Read thread details
        mockMvc.perform(get("/api/threads/" + thread.getThreadId())
                        .with(auth(enrolledStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Service Mesh with Istio"));

        // 3. Read posts
        mockMvc.perform(get("/api/threads/" + thread.getThreadId() + "/posts")
                        .with(auth(enrolledStudent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // 4. Post reply
        mockMvc.perform(post("/api/threads/" + thread.getThreadId() + "/posts")
                        .with(auth(enrolledStudent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Enrolled reply verified"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Enrolled reply verified"));
    }

    @Test
    @DisplayName("Scenario 5: Course staff (teacher) can read and write without student enrollment")
    public void testCourseStaffCanReadAndWrite() throws Exception {
        // Teacher can list course threads
        mockMvc.perform(get("/api/courses/" + course.getId() + "/threads")
                        .with(auth(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // Teacher can create thread
        mockMvc.perform(post("/api/courses/" + course.getId() + "/threads")
                        .with(auth(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Teacher Office Hours",
                                "content", "Office hours every Wednesday 3pm UTC"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Teacher Office Hours"));

        // Teacher can reply to thread
        mockMvc.perform(post("/api/threads/" + thread.getThreadId() + "/posts")
                        .with(auth(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Official staff guidance"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Official staff guidance"));
    }

    @Test
    @DisplayName("Scenario 6: Admin can read and write community across all courses")
    public void testAdminCanReadAndWriteAnyCourse() throws Exception {
        mockMvc.perform(get("/api/courses/" + course.getId() + "/threads")
                        .with(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/api/threads/" + thread.getThreadId())
                        .with(auth(admin)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/threads/" + thread.getThreadId() + "/posts")
                        .with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "System administrative announcement"
                        ))))
                .andExpect(status().isOk());
    }
}
