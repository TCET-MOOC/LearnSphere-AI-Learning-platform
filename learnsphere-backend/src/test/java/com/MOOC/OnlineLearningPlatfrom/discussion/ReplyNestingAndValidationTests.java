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
 * W5-03: Reply nesting and cross-thread validation tests.
 *
 * Rules: A-13, [E-F5, E-F6].
 *
 * Scenarios:
 * 1. Two-level nesting: Root post -> Reply (allowed).
 * 2. Three-level nesting rejected: Reply to a Reply -> 400 Bad Request.
 * 3. Cross-thread reply rejected: Reply to a post belonging to another thread -> 400 Bad Request.
 * 4. Non-existent parent post -> 404 Not Found.
 * 5. Cascade delete: Deleting parent post cascade deletes its replies.
 * 6. Ordering: Posts and replies ordered by createdAt ASC.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ReplyNestingAndValidationTests extends PostgresIntegrationTest {

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
    private Course course;
    private DiscussionThread threadA;
    private DiscussionThread threadB;

    @BeforeEach
    public void setup() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");

        course = new Course();
        course.setTitle("Advanced Algorithms");
        course.setDescription("Graph theory, network flow and randomized algorithms");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);

        threadA = new DiscussionThread();
        threadA.setCourse(course);
        threadA.setUser(student);
        threadA.setTitle("Ford-Fulkerson vs Edmonds-Karp");
        threadA.setContent("What is the exact asymptotic bound for Edmonds-Karp?");
        threadA = discussionThreadRepository.save(threadA);

        threadB = new DiscussionThread();
        threadB.setCourse(course);
        threadB.setUser(student);
        threadB.setTitle("Push-Relabel Algorithm");
        threadB.setContent("How does the gap heuristic optimize push-relabel?");
        threadB = discussionThreadRepository.save(threadB);

        entityManager.flush();
        entityManager.clear();
    }

    private RequestPostProcessor auth(UserAccount u) {
        return user(userDetailsService.loadUserByUsername(u.getEmail()));
    }

    @Test
    @DisplayName("Scenario 1: Two-level nesting — create root post and a valid reply")
    public void testTwoLevelNestingAllowed() throws Exception {
        // 1. Create root post (Level 1)
        MvcResult rootRes = mockMvc.perform(post("/api/threads/" + threadA.getThreadId() + "/posts")
                        .with(auth(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Edmonds-Karp runs in O(V * E^2) time using BFS for shortest augmenting paths."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").isNumber())
                .andExpect(jsonPath("$.parentPostId").value(nullValue()))
                .andReturn();

        Long rootPostId = objectMapper.readTree(rootRes.getResponse().getContentAsString()).get("postId").asLong();

        // 2. Create reply (Level 2)
        mockMvc.perform(post("/api/threads/" + threadA.getThreadId() + "/posts")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Does using Dinic's algorithm improve this to O(V^2 * E)?",
                                "parentPostId", rootPostId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentPostId").value(rootPostId));
    }

    @Test
    @DisplayName("Scenario 2: Three-level nesting rejected — replying to a reply returns 400 Bad Request")
    public void testThreeLevelNestingRejected() throws Exception {
        // 1. Root post
        MvcResult rootRes = mockMvc.perform(post("/api/threads/" + threadA.getThreadId() + "/posts")
                        .with(auth(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Level 1 Root"))))
                .andExpect(status().isOk())
                .andReturn();
        Long rootPostId = objectMapper.readTree(rootRes.getResponse().getContentAsString()).get("postId").asLong();

        // 2. Reply (Level 2)
        MvcResult replyRes = mockMvc.perform(post("/api/threads/" + threadA.getThreadId() + "/posts")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Level 2 Reply",
                                "parentPostId", rootPostId
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        Long replyPostId = objectMapper.readTree(replyRes.getResponse().getContentAsString()).get("postId").asLong();

        // 3. Reply to reply (Level 3) -> MUST BE REJECTED with 400
        mockMvc.perform(post("/api/threads/" + threadA.getThreadId() + "/posts")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Level 3 Nested Reply",
                                "parentPostId", replyPostId
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsStringIgnoringCase("two levels")));
    }

    @Test
    @DisplayName("Scenario 3: Cross-thread reply rejected — parent post from another thread returns 400")
    public void testCrossThreadReplyRejected() throws Exception {
        // Create root post in Thread B
        MvcResult postInB = mockMvc.perform(post("/api/threads/" + threadB.getThreadId() + "/posts")
                        .with(auth(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Post in Thread B"))))
                .andExpect(status().isOk())
                .andReturn();
        Long postInBId = objectMapper.readTree(postInB.getResponse().getContentAsString()).get("postId").asLong();

        // Attempt to reply in Thread A referencing post from Thread B -> MUST BE REJECTED with 400
        mockMvc.perform(post("/api/threads/" + threadA.getThreadId() + "/posts")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Cross-thread reply attempt",
                                "parentPostId", postInBId
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsStringIgnoringCase("thread")));
    }

    @Test
    @DisplayName("Scenario 4: Non-existent parent post returns 404 Not Found")
    public void testParentPostNotFound() throws Exception {
        mockMvc.perform(post("/api/threads/" + threadA.getThreadId() + "/posts")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Orphan reply",
                                "parentPostId", 999999L
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 5: Cascade delete — deleting parent post deletes its replies")
    public void testCascadeDeleteParentPost() throws Exception {
        // 1. Root post
        MvcResult rootRes = mockMvc.perform(post("/api/threads/" + threadA.getThreadId() + "/posts")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Parent post to delete"))))
                .andExpect(status().isOk())
                .andReturn();
        Long rootPostId = objectMapper.readTree(rootRes.getResponse().getContentAsString()).get("postId").asLong();

        // 2. Child reply
        MvcResult replyRes = mockMvc.perform(post("/api/threads/" + threadA.getThreadId() + "/posts")
                        .with(auth(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Child reply to be cascaded",
                                "parentPostId", rootPostId
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        Long replyPostId = objectMapper.readTree(replyRes.getResponse().getContentAsString()).get("postId").asLong();

        // 3. Delete parent post
        mockMvc.perform(delete("/api/posts/" + rootPostId)
                        .with(auth(student)))
                .andExpect(status().isNoContent());

        entityManager.clear();

        // 4. Verify both parent and reply are gone from database
        assertThat(postRepository.findById(rootPostId)).isEmpty();
        assertThat(postRepository.findById(replyPostId)).isEmpty();
    }

    @Test
    @DisplayName("Scenario 6: Ordering — posts and replies ordered by createdAt ASC")
    public void testPostsAndRepliesOrdering() throws Exception {
        Post p1 = new Post();
        p1.setThread(threadA);
        p1.setUser(student);
        p1.setBody("First root post");
        p1.setCreatedAt(LocalDateTime.now().minusMinutes(20));
        p1 = postRepository.save(p1);

        Post p2 = new Post();
        p2.setThread(threadA);
        p2.setUser(teacher);
        p2.setBody("Second root post");
        p2.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        p2 = postRepository.save(p2);

        Post r1 = new Post();
        r1.setThread(threadA);
        r1.setUser(teacher);
        r1.setParentPost(p1);
        r1.setBody("First reply to p1");
        r1.setCreatedAt(LocalDateTime.now().minusMinutes(15));
        postRepository.save(r1);

        Post r2 = new Post();
        r2.setThread(threadA);
        r2.setUser(student);
        r2.setParentPost(p1);
        r2.setBody("Second reply to p1");
        r2.setCreatedAt(LocalDateTime.now().minusMinutes(12));
        postRepository.save(r2);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/threads/" + threadA.getThreadId() + "/posts")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].body").value("First root post"))
                .andExpect(jsonPath("$[1].body").value("First reply to p1"))
                .andExpect(jsonPath("$[2].body").value("Second reply to p1"))
                .andExpect(jsonPath("$[3].body").value("Second root post"));
    }
}
