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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W5-01: Canonical Discussion Thread & Post Tests.
 *
 * Rules: A-13, [E-F1], LMS Entity Reference Section 10.
 *
 * Scenarios:
 * 1. Create thread under course via POST /api/courses/{courseId}/threads.
 * 2. List course threads via GET /api/courses/{courseId}/threads (ordered createdAt DESC).
 * 3. Get thread by ID via GET /api/threads/{id} (includes author and posts).
 * 4. Update thread via PUT /api/threads/{id} (author/admin allowed, others rejected).
 * 5. Delete thread via DELETE /api/threads/{id} (cascades to posts).
 * 6. Create post/reply via POST /api/threads/{id}/posts.
 * 7. List posts for thread via GET /api/threads/{id}/posts (ordered createdAt ASC).
 * 8. Edit and delete post via PUT/DELETE /api/posts/{id}.
 * 9. Legacy endpoints (/api/courses/{id}/discussion, /api/discussion) are retired (return 404).
 * 10. Private, no-store cache headers on all discussion responses.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CanonicalDiscussionThreadTests extends PostgresIntegrationTest {

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
    private UserAccount student;
    private UserAccount otherStudent;
    private UserAccount admin;
    private Course course;

    @BeforeEach
    public void setup() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        otherStudent = userAccountRepository.findByEmail("external@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");

        course = new Course();
        course.setTitle("Distributed Systems Architecture");
        course.setDescription("Design and implementation of distributed storage and consensus");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now().minusDays(1));
        enrollmentRepository.save(enrollment);

        entityManager.flush();
        entityManager.clear();
    }

    private RequestPostProcessor auth(UserAccount u) {
        return user(userDetailsService.loadUserByUsername(u.getEmail()));
    }

    @Test
    @DisplayName("Scenario 1: Create thread under course with title, content and author attribution")
    public void testCreateThread() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "title", "Raft Consensus Log Compaction",
                "content", "How does snapshotting interact with slow followers?"
        ));

        mockMvc.perform(post("/api/courses/" + course.getId() + "/threads")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.threadId").isNumber())
                .andExpect(jsonPath("$.title").value("Raft Consensus Log Compaction"))
                .andExpect(jsonPath("$.content").value("How does snapshotting interact with slow followers?"))
                .andExpect(jsonPath("$.userId").value(student.getUserId()))
                .andExpect(jsonPath("$.authorName").value(student.getFullName()));
    }

    @Test
    @DisplayName("Scenario 2: List threads for course ordered by createdAt DESC")
    public void testListCourseThreads() throws Exception {
        DiscussionThread t1 = new DiscussionThread();
        t1.setCourse(course);
        t1.setUser(student);
        t1.setTitle("First Question");
        t1.setContent("Details of first question");
        t1.setCreatedAt(LocalDateTime.now().minusHours(2));
        t1 = discussionThreadRepository.save(t1);

        DiscussionThread t2 = new DiscussionThread();
        t2.setCourse(course);
        t2.setUser(teacher);
        t2.setTitle("Second Question");
        t2.setContent("Details of second question");
        t2.setCreatedAt(LocalDateTime.now().minusHours(1));
        t2 = discussionThreadRepository.save(t2);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/courses/" + course.getId() + "/threads")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[0].title").value("Second Question"))
                .andExpect(jsonPath("$[1].title").value("First Question"));
    }

    @Test
    @DisplayName("Scenario 3: Get thread by ID returns thread details and posts")
    public void testGetThreadById() throws Exception {
        DiscussionThread t = new DiscussionThread();
        t.setCourse(course);
        t.setUser(student);
        t.setTitle("Gossip Protocols");
        t.setContent("Epidemic dissemination convergence rates.");
        t = discussionThreadRepository.save(t);

        Post p = new Post();
        p.setThread(t);
        p.setUser(teacher);
        p.setBody("Check the SWAN and SWIM protocol papers.");
        postRepository.save(p);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/threads/" + t.getThreadId())
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.threadId").value(t.getThreadId()))
                .andExpect(jsonPath("$.title").value("Gossip Protocols"))
                .andExpect(jsonPath("$.posts", hasSize(1)))
                .andExpect(jsonPath("$.posts[0].body").value("Check the SWAN and SWIM protocol papers."));
    }

    @Test
    @DisplayName("Scenario 4: Author can update thread; non-author non-admin is rejected")
    public void testUpdateThreadAuthorization() throws Exception {
        DiscussionThread t = new DiscussionThread();
        t.setCourse(course);
        t.setUser(student);
        t.setTitle("Original Title");
        t.setContent("Original Content");
        t = discussionThreadRepository.save(t);
        entityManager.flush();
        entityManager.clear();

        // Non-author tries to update -> rejected (400 or 403)
        mockMvc.perform(put("/api/threads/" + t.getThreadId())
                        .with(auth(otherStudent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Hacked Title"))))
                .andExpect(status().is4xxClientError());

        // Author updates -> 200 OK
        mockMvc.perform(put("/api/threads/" + t.getThreadId())
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Updated Title"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));

        // Admin updates -> 200 OK
        mockMvc.perform(put("/api/threads/" + t.getThreadId())
                        .with(auth(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("content", "Moderated Content"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Moderated Content"));
    }

    @Test
    @DisplayName("Scenario 5: Thread deletion cascades to posts; unauthorized delete rejected")
    public void testDeleteThreadCascade() throws Exception {
        DiscussionThread t = new DiscussionThread();
        t.setCourse(course);
        t.setUser(student);
        t.setTitle("Thread to be deleted");
        t.setContent("Will be removed.");
        t = discussionThreadRepository.save(t);

        Post p1 = new Post();
        p1.setThread(t);
        p1.setUser(teacher);
        p1.setBody("Post 1");
        p1 = postRepository.save(p1);

        Post p2 = new Post();
        p2.setThread(t);
        p2.setUser(student);
        p2.setBody("Post 2");
        p2 = postRepository.save(p2);

        entityManager.flush();
        entityManager.clear();

        // Other student cannot delete
        mockMvc.perform(delete("/api/threads/" + t.getThreadId())
                        .with(auth(otherStudent)))
                .andExpect(status().is4xxClientError());

        // Author deletes
        mockMvc.perform(delete("/api/threads/" + t.getThreadId())
                        .with(auth(student)))
                .andExpect(status().isNoContent());

        assertThat(discussionThreadRepository.findById(t.getThreadId())).isEmpty();
        assertThat(postRepository.findById(p1.getPostId())).isEmpty();
        assertThat(postRepository.findById(p2.getPostId())).isEmpty();
    }

    @Test
    @DisplayName("Scenario 6: Create post and nested replies in thread")
    public void testCreatePostAndReplies() throws Exception {
        DiscussionThread t = new DiscussionThread();
        t.setCourse(course);
        t.setUser(student);
        t.setTitle("Vector Clocks vs Lamport Timestamps");
        t.setContent("When do we strictly need Vector Clocks?");
        t = discussionThreadRepository.save(t);
        entityManager.flush();
        entityManager.clear();

        // 1. Root post in thread
        MvcResult postRes = mockMvc.perform(post("/api/threads/" + t.getThreadId() + "/posts")
                        .with(auth(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Vector clocks detect concurrent, causally independent events."
                        ))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.postId").isNumber())
                .andExpect(jsonPath("$.threadId").value(t.getThreadId()))
                .andExpect(jsonPath("$.body").value("Vector clocks detect concurrent, causally independent events."))
                .andReturn();

        Long parentPostId = objectMapper.readTree(postRes.getResponse().getContentAsString()).get("postId").asLong();

        // 2. Reply to that post
        mockMvc.perform(post("/api/threads/" + t.getThreadId() + "/posts")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Understood, thank you Professor!",
                                "parentPostId", parentPostId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentPostId").value(parentPostId));
    }

    @Test
    @DisplayName("Scenario 7: List posts for thread ordered by createdAt ASC")
    public void testListThreadPosts() throws Exception {
        DiscussionThread t = new DiscussionThread();
        t.setCourse(course);
        t.setUser(student);
        t.setTitle("Two Phase Commit");
        t.setContent("Coordinator failure scenarios.");
        t = discussionThreadRepository.save(t);

        Post p1 = new Post();
        p1.setThread(t);
        p1.setUser(student);
        p1.setBody("First message");
        p1.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        postRepository.save(p1);

        Post p2 = new Post();
        p2.setThread(t);
        p2.setUser(teacher);
        p2.setBody("Second message");
        p2.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        postRepository.save(p2);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/threads/" + t.getThreadId() + "/posts")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].body").value("First message"))
                .andExpect(jsonPath("$[1].body").value("Second message"));
    }

    @Test
    @DisplayName("Scenario 8: Edit and delete post authorization")
    public void testEditAndDeletePost() throws Exception {
        DiscussionThread t = new DiscussionThread();
        t.setCourse(course);
        t.setUser(student);
        t.setTitle("Chandy-Lamport Snapshotting");
        t.setContent("How are marker messages handled?");
        t = discussionThreadRepository.save(t);

        Post p = new Post();
        p.setThread(t);
        p.setUser(student);
        p.setBody("Original post text");
        p = postRepository.save(p);
        entityManager.flush();
        entityManager.clear();

        // Non-author cannot edit
        mockMvc.perform(put("/api/posts/" + p.getPostId())
                        .with(auth(otherStudent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Hacked"))))
                .andExpect(status().is4xxClientError());

        // Author edits
        mockMvc.perform(put("/api/posts/" + p.getPostId())
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Edited text"))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.body").value("Edited text"));

        // Author deletes
        mockMvc.perform(delete("/api/posts/" + p.getPostId())
                        .with(auth(student)))
                .andExpect(status().isNoContent());

        assertThat(postRepository.findById(p.getPostId())).isEmpty();
    }

    @Test
    @DisplayName("Scenario 9: Retired legacy discussion endpoints return 404 NOT_FOUND")
    public void testLegacyDiscussionEndpointsRetired() throws Exception {
        mockMvc.perform(get("/api/courses/" + course.getId() + "/discussion")
                        .with(auth(student)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/lectures/1/discussion")
                        .with(auth(student)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/discussion")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":1,\"body\":\"Legacy post\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/discussion/1")
                        .with(auth(student)))
                .andExpect(status().isNotFound());
    }
}

