package com.MOOC.OnlineLearningPlatfrom.moderation;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Story W5-04: Moderation on canonical posts
 * Rules: [E-F7, E-F8], NFR-03
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CanonicalPostModerationTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private DiscussionThreadRepository discussionThreadRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private FlaggedContentRepository flaggedContentRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private EntityManager entityManager;

    private UserAccount student;
    private UserAccount unenrolledStudent;
    private UserAccount teacher;
    private UserAccount admin;
    private Course course;
    private DiscussionThread cleanThread;

    @BeforeEach
    public void setup() {
        flaggedContentRepository.deleteAll();
        postRepository.deleteAll();
        discussionThreadRepository.deleteAll();

        // Retrieve seeded users or create
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");

        unenrolledStudent = userAccountRepository.findByEmail("external@learnsphere.com");

        // Create published course
        course = new Course();
        course.setTitle("Distributed Systems Architecture");
        course.setDescription("Comprehensive microservices and distributed data architecture");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        // Enroll student
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);

        // Create a clean discussion thread
        cleanThread = new DiscussionThread();
        cleanThread.setCourse(course);
        cleanThread.setUser(teacher);
        cleanThread.setTitle("Welcome to Distributed Systems");
        cleanThread.setContent("Feel free to ask questions here.");
        cleanThread.setFlagged(false);
        cleanThread = discussionThreadRepository.save(cleanThread);
    }

    private RequestPostProcessor auth(UserAccount u) {
        return user(userDetailsService.loadUserByUsername(u.getEmail()));
    }

    @Test
    @DisplayName("Scenario 1: Auto-flagging on post creation with bullying keywords")
    public void testAutoFlagPostCreation() throws Exception {
        // Enrolled student posts bullying comment: "You are an idiot and stupid, shut up"
        MvcResult result = mockMvc.perform(post("/api/threads/" + cleanThread.getThreadId() + "/posts")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "You are an idiot and stupid, shut up"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(true))
                .andReturn();

        Long postId = objectMapper.readTree(result.getResponse().getContentAsString()).get("postId").asLong();

        Post savedPost = postRepository.findById(postId).orElseThrow();
        assertThat(savedPost.getFlagged()).isTrue();

        // Verify FlaggedContent record exists in PENDING state with reason BULLYING
        List<FlaggedContent> flags = flaggedContentRepository.findAll();
        assertThat(flags).anyMatch(f ->
                f.getContentId().equals(postId) &&
                f.getContentType() == FlaggedContent.ContentType.DISCUSSION_POST &&
                f.getReason() == FlaggedContent.Reason.BULLYING &&
                f.getStatus() == FlaggedContent.Status.PENDING
        );
    }

    @Test
    @DisplayName("Scenario 2: Auto-flagging on thread creation with spam keywords")
    public void testAutoFlagThreadCreation() throws Exception {
        // Enrolled student creates thread with spam: "Free money click here buy now"
        MvcResult result = mockMvc.perform(post("/api/courses/" + course.getCourseId() + "/threads")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Special deal",
                                "content", "Free money click here buy now on this site"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(true))
                .andReturn();

        Long threadId = objectMapper.readTree(result.getResponse().getContentAsString()).get("threadId").asLong();

        DiscussionThread savedThread = discussionThreadRepository.findById(threadId).orElseThrow();
        assertThat(savedThread.getFlagged()).isTrue();

        // Verify FlaggedContent record exists in PENDING state with reason SPAM
        List<FlaggedContent> flags = flaggedContentRepository.findAll();
        assertThat(flags).anyMatch(f ->
                f.getContentId().equals(threadId) &&
                (f.getContentType() == FlaggedContent.ContentType.DISCUSSION_THREAD || f.getContentType() == FlaggedContent.ContentType.DISCUSSION_POST) &&
                f.getReason() == FlaggedContent.Reason.SPAM &&
                f.getStatus() == FlaggedContent.Status.PENDING
        );
    }

    @Test
    @DisplayName("Scenario 3: Admin GET /api/admin/flagged lists queue with metadata and category filter")
    public void testAdminGetFlaggedQueue() throws Exception {
        // Create an offending post
        mockMvc.perform(post("/api/threads/" + cleanThread.getThreadId() + "/posts")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "This instructor is an idiot"
                        ))))
                .andExpect(status().isOk());

        // Admin queries queue
        mockMvc.perform(get("/api/admin/flagged")
                        .with(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].authorName").value(student.getFullName()))
                .andExpect(jsonPath("$[0].courseTitle").value("Distributed Systems Architecture"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].severity").value("high"));

        // Filter by category bullying
        mockMvc.perform(get("/api/admin/flagged?category=bullying")
                        .with(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].reason").value("BULLYING"));

        // Filter by category spam -> empty
        mockMvc.perform(get("/api/admin/flagged?category=spam")
                        .with(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Scenario 4: Non-admin users rejected from /api/admin/flagged/**")
    public void testModerationAuthorization() throws Exception {
        // Unauthenticated -> 401
        mockMvc.perform(get("/api/admin/flagged"))
                .andExpect(status().isUnauthorized());

        // Student -> 403
        mockMvc.perform(get("/api/admin/flagged")
                        .with(auth(student)))
                .andExpect(status().isForbidden());

        // Teacher -> 403
        mockMvc.perform(get("/api/admin/flagged")
                        .with(auth(teacher)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Scenario 5: Admin resolve masks content with [Content removed by moderator] and clears post flag")
    public void testAdminResolveMasksContent() throws Exception {
        // 1. Student creates offensive post
        MvcResult postRes = mockMvc.perform(post("/api/threads/" + cleanThread.getThreadId() + "/posts")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "You are an idiot and worthless"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        Long postId = objectMapper.readTree(postRes.getResponse().getContentAsString()).get("postId").asLong();

        // 2. Locate the FlaggedContent id
        List<FlaggedContent> flags = flaggedContentRepository.findAll();
        FlaggedContent flag = flags.stream()
                .filter(f -> f.getContentId().equals(postId))
                .findFirst()
                .orElseThrow();

        // 3. Admin resolves the flagged content
        mockMvc.perform(put("/api/admin/flagged/" + flag.getId() + "/resolve")
                        .with(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        // 4. Verify post body is masked and flag is cleared
        Post updatedPost = postRepository.findById(postId).orElseThrow();
        assertThat(updatedPost.getBody()).isEqualTo("[Content removed by moderator]");
        assertThat(updatedPost.getFlagged()).isFalse();

        // 5. Subsequent GET on the thread shows masked content
        mockMvc.perform(get("/api/threads/" + cleanThread.getThreadId() + "/posts")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("[Content removed by moderator]"))
                .andExpect(jsonPath("$[0].flagged").value(false));
    }

    @Test
    @DisplayName("Scenario 6: Admin dismiss preserves original content and clears post flag")
    public void testAdminDismissClearsFlagPreservesContent() throws Exception {
        // 1. Student creates post
        MvcResult postRes = mockMvc.perform(post("/api/threads/" + cleanThread.getThreadId() + "/posts")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "I hate you"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        Long postId = objectMapper.readTree(postRes.getResponse().getContentAsString()).get("postId").asLong();

        // 2. Locate FlaggedContent
        FlaggedContent flag = flaggedContentRepository.findAll().stream()
                .filter(f -> f.getContentId().equals(postId))
                .findFirst()
                .orElseThrow();

        // 3. Admin dismisses flag (false positive)
        mockMvc.perform(put("/api/admin/flagged/" + flag.getId() + "/dismiss")
                        .with(auth(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISMISSED"));

        // 4. Verify post body is preserved and flag is cleared
        Post updatedPost = postRepository.findById(postId).orElseThrow();
        assertThat(updatedPost.getBody()).isEqualTo("I hate you");
        assertThat(updatedPost.getFlagged()).isFalse();
    }

    @Test
    @DisplayName("Scenario 7: Enrolled user manual reporting of post creates PENDING flag with reporterId")
    public void testManualReportPost() throws Exception {
        // 1. Teacher creates a normal post
        MvcResult postRes = mockMvc.perform(post("/api/threads/" + cleanThread.getThreadId() + "/posts")
                        .with(auth(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Question about exam schedule"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        Long postId = objectMapper.readTree(postRes.getResponse().getContentAsString()).get("postId").asLong();

        // 2. Enrolled student reports the post
        mockMvc.perform(post("/api/posts/" + postId + "/flag")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "reason", "SPAM"
                        ))))
                .andExpect(status().isOk());

        // 3. Verify flag exists with student's reporterId
        List<FlaggedContent> flags = flaggedContentRepository.findAll();
        assertThat(flags).anyMatch(f ->
                f.getContentId().equals(postId) &&
                f.getReporterId().equals(student.getUserId()) &&
                f.getReason() == FlaggedContent.Reason.SPAM &&
                f.getStatus() == FlaggedContent.Status.PENDING
        );

        // 4. Unenrolled student reporting post is rejected with 404 (IDOR protection)
        mockMvc.perform(post("/api/posts/" + postId + "/flag")
                        .with(auth(unenrolledStudent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("reason", "SPAM"))))
                .andExpect(status().isNotFound());
    }
}
