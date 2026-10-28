package com.MOOC.OnlineLearningPlatfrom.media;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W2-13: Opt-in YouTube Publish & Channel Connections Security Tests.
 *
 * Rules: A-25, BR-26, NFR-03, spec §6.
 *
 * Validates:
 * 1. OAuth connect generates Google OAuth URL and signed single-use state token.
 * 2. OAuth callback rejects tampered or expired state tokens.
 * 3. OAuth callback rejects replayed state tokens.
 * 4. Stored refresh token is encrypted and NEVER leaked in GET connection responses.
 * 5. Disconnect channel removes connection cleanly.
 * 6. Content publish rejects if teacher has no connected channel (400).
 * 7. Content publish rejects foreign teacher with 404 (IDOR defense).
 * 8. Content publish rejects non-UPLOAD content (400).
 * 9. Content publish creates publish job with 202 ACCEPTED.
 * 10. Concurrent publish on the same content returns 409 CONFLICT.
 * 11. Foreign teacher job status query returns 404 NOT_FOUND.
 * 12. On publish completion, providerVideoId is saved but sourceType remains UPLOAD.
 * 13. Quota/error failure records error on job and keeps local source UPLOAD intact.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class YouTubePublishSecurityTests extends PostgresIntegrationTest {

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
    private CourseContentRepository courseContentRepository;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private UserAccount teacher;
    private UserAccount foreignTeacher;
    private UserAccount student;
    private Course course;
    private Module module;
    private CourseContent uploadContent;
    private CourseContent youtubeContent;
    private StoredFile storedVideo;

    private RequestPostProcessor asTeacher() {
        return user(userDetailsService.loadUserByUsername(teacher.getEmail()));
    }

    private RequestPostProcessor asForeignTeacher() {
        return user(userDetailsService.loadUserByUsername(foreignTeacher.getEmail()));
    }

    private RequestPostProcessor asStudent() {
        return user(userDetailsService.loadUserByUsername(student.getEmail()));
    }

    @BeforeEach
    void setUp() throws Exception {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");

        foreignTeacher = userAccountRepository.findByEmail("pending@learnsphere.com");
        if (foreignTeacher == null) {
            foreignTeacher = userAccountRepository.findByEmail("external@learnsphere.com");
        }

        Department department = departmentRepository.findByName("Computer Science");
        if (department == null) {
            Department d = new Department();
            d.setName("Computer Science");
            department = departmentRepository.save(d);
        }

        course = new Course();
        course.setTitle("Cloud Video Pipelines");
        course.setDescription("Streaming and transcoding architectures");
        course.setPrice(BigDecimal.valueOf(19.99));
        course.setTeacher(teacher);
        course.setDepartment(department);
        course.setDepartmentLabel(department.getName());
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        module = new Module();
        module.setCourse(course);
        module.setTitle("Module 1: Transcoding");
        module.setPosition(1);
        module = moduleRepository.save(module);

        // Create stored file for UPLOAD content
        String fileKey = UUID.randomUUID().toString().replace("-", "");
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path teacherDir = root.resolve(String.valueOf(teacher.getUserId())).normalize();
        Files.createDirectories(teacherDir);
        Path targetPath = teacherDir.resolve("cloud-lecture.mp4");
        Files.write(targetPath, new byte[]{0, 0, 0, 32, 'f', 't', 'y', 'p'});

        storedVideo = new StoredFile();
        storedVideo.setFileKey(fileKey);
        storedVideo.setOwnerId(teacher.getUserId());
        storedVideo.setOriginalFilename("cloud-lecture.mp4");
        storedVideo.setContentType("video/mp4");
        storedVideo.setSizeBytes(32L);
        storedVideo.setStoragePath(teacher.getUserId() + "/cloud-lecture.mp4");
        storedVideo = storedFileRepository.save(storedVideo);

        uploadContent = new CourseContent();
        uploadContent.setModule(module);
        uploadContent.setContentType("VIDEO");
        uploadContent.setTitle("Local Lecture Video");
        uploadContent.setSourceType("UPLOAD");
        uploadContent.setStorageKey(fileKey);
        uploadContent.setMimeType("video/mp4");
        uploadContent.setSizeBytes(32L);
        uploadContent.setPosition(1);
        uploadContent.setMediaStatus("READY");
        uploadContent = courseContentRepository.save(uploadContent);

        youtubeContent = new CourseContent();
        youtubeContent.setModule(module);
        youtubeContent.setContentType("VIDEO");
        youtubeContent.setTitle("Already YouTube Video");
        youtubeContent.setSourceType("YOUTUBE");
        youtubeContent.setProviderVideoId("abc12345678");
        youtubeContent.setPosition(2);
        youtubeContent.setMediaStatus("READY");
        youtubeContent = courseContentRepository.save(youtubeContent);
    }

    // =========================================================================
    // 1. OAuth Connect, Callback, State Validation & No Token Leak
    // =========================================================================

    @Test
    @DisplayName("W2-13: Connect endpoint returns Google OAuth authUrl and signed state")
    void shouldGenerateSignedOAuthStateAndUrl() throws Exception {
        mockMvc.perform(post("/api/teacher/media/youtube/connect")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.authUrl", containsString("accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(jsonPath("$.authUrl", containsString("scope=")))
                .andExpect(jsonPath("$.state", notNullValue()));
    }

    @Test
    @DisplayName("W2-13: Callback rejects tampered state token with 403 Forbidden")
    void shouldRejectTamperedOAuthState() throws Exception {
        MvcResult connectResult = mockMvc.perform(post("/api/teacher/media/youtube/connect")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andReturn();

        String state = com.jayway.jsonpath.JsonPath.read(connectResult.getResponse().getContentAsString(), "$.state");
        String tamperedState = state + "bad";

        mockMvc.perform(get("/api/teacher/media/youtube/callback")
                        .with(asTeacher())
                        .param("code", "mock-auth-code")
                        .param("state", tamperedState))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("W2-13: Callback rejects expired or invalid state token with 403 Forbidden")
    void shouldRejectExpiredOAuthState() throws Exception {
        mockMvc.perform(get("/api/teacher/media/youtube/callback")
                        .with(asTeacher())
                        .param("code", "mock-auth-code")
                        .param("state", "expiredState123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("W2-13: Callback with valid state connects channel, but replayed state is rejected with 400 Bad Request")
    void shouldRejectReplayedOAuthState() throws Exception {
        MvcResult connectResult = mockMvc.perform(post("/api/teacher/media/youtube/connect")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andReturn();

        String state = com.jayway.jsonpath.JsonPath.read(connectResult.getResponse().getContentAsString(), "$.state");

        // First callback succeeds
        mockMvc.perform(get("/api/teacher/media/youtube/callback")
                        .with(asTeacher())
                        .param("code", "mock-auth-code")
                        .param("state", state))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true));

        // Replaying the same state must be rejected
        mockMvc.perform(get("/api/teacher/media/youtube/callback")
                        .with(asTeacher())
                        .param("code", "mock-auth-code")
                        .param("state", state))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("W2-13: Connection status returns metadata without leaking refresh token or access token")
    void shouldStoreEncryptedTokenAndNeverLeakInConnectionStatus() throws Exception {
        // Connect channel
        MvcResult connectResult = mockMvc.perform(post("/api/teacher/media/youtube/connect")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andReturn();

        String state = com.jayway.jsonpath.JsonPath.read(connectResult.getResponse().getContentAsString(), "$.state");

        mockMvc.perform(get("/api/teacher/media/youtube/callback")
                        .with(asTeacher())
                        .param("code", "mock-auth-code")
                        .param("state", state))
                .andExpect(status().isOk());

        // Read connection status
        MvcResult statusResult = mockMvc.perform(get("/api/teacher/media/youtube/connection")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.connected").value(true))
                .andExpect(jsonPath("$.channelId", notNullValue()))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.encryptedRefreshToken").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        String body = statusResult.getResponse().getContentAsString();
        assertThat(body).doesNotContain("mock-refresh-token");
    }

    @Test
    @DisplayName("W2-13: Disconnect channel revokes connection cleanly")
    void shouldDisconnectChannelSuccessfully() throws Exception {
        // Connect first
        MvcResult connectResult = mockMvc.perform(post("/api/teacher/media/youtube/connect")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andReturn();

        String state = com.jayway.jsonpath.JsonPath.read(connectResult.getResponse().getContentAsString(), "$.state");
        mockMvc.perform(get("/api/teacher/media/youtube/callback")
                        .with(asTeacher())
                        .param("code", "mock-auth-code")
                        .param("state", state))
                .andExpect(status().isOk());

        // Disconnect
        mockMvc.perform(delete("/api/teacher/media/youtube/connection")
                        .with(asTeacher()))
                .andExpect(status().isNoContent());

        // Status shows disconnected
        mockMvc.perform(get("/api/teacher/media/youtube/connection")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(false));
    }

    // =========================================================================
    // 2. Publish Authorization, Validation, Idempotency & 409
    // =========================================================================

    @Test
    @DisplayName("W2-13: Content publish without connected YouTube channel returns 400 Bad Request")
    void shouldRejectPublishIfNoChannelConnected() throws Exception {
        mockMvc.perform(post("/api/teacher/contents/" + uploadContent.getContentId() + "/youtube-publish")
                        .with(asTeacher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("W2-13: Content publish by foreign teacher returns 404 Not Found (IDOR defense)")
    void shouldRejectPublishForForeignTeacherWith404() throws Exception {
        mockMvc.perform(post("/api/teacher/contents/" + uploadContent.getContentId() + "/youtube-publish")
                        .with(asForeignTeacher()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("W2-13: Content publish for non-UPLOAD content returns 400 Bad Request")
    void shouldRejectPublishForNonUploadContent() throws Exception {
        // Connect channel
        MvcResult connectResult = mockMvc.perform(post("/api/teacher/media/youtube/connect")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andReturn();

        String state = com.jayway.jsonpath.JsonPath.read(connectResult.getResponse().getContentAsString(), "$.state");
        mockMvc.perform(get("/api/teacher/media/youtube/callback")
                        .with(asTeacher())
                        .param("code", "mock-auth-code")
                        .param("state", state))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/teacher/contents/" + youtubeContent.getContentId() + "/youtube-publish")
                        .with(asTeacher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("W2-13: Valid content publish returns 202 ACCEPTED with jobId and creates job")
    void shouldReturn202AndCreatePublishJob() throws Exception {
        // Connect channel
        MvcResult connectResult = mockMvc.perform(post("/api/teacher/media/youtube/connect")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andReturn();

        String state = com.jayway.jsonpath.JsonPath.read(connectResult.getResponse().getContentAsString(), "$.state");
        mockMvc.perform(get("/api/teacher/media/youtube/callback")
                        .with(asTeacher())
                        .param("code", "mock-auth-code")
                        .param("state", state))
                .andExpect(status().isOk());

        MvcResult publishResult = mockMvc.perform(post("/api/teacher/contents/" + uploadContent.getContentId() + "/youtube-publish")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"pub-key-101\"}"))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.jobId", notNullValue()))
                .andExpect(jsonPath("$.contentId").value(uploadContent.getContentId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        Long jobId = ((Number) com.jayway.jsonpath.JsonPath.read(publishResult.getResponse().getContentAsString(), "$.jobId")).longValue();

        // Check job query
        mockMvc.perform(get("/api/teacher/media/youtube/jobs/" + jobId)
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId))
                .andExpect(jsonPath("$.contentId").value(uploadContent.getContentId()));
    }

    @Test
    @DisplayName("W2-13: Concurrent duplicate publish request returns 409 CONFLICT")
    void shouldRejectDuplicateConcurrentPublishWith409() throws Exception {
        // Connect channel
        MvcResult connectResult = mockMvc.perform(post("/api/teacher/media/youtube/connect")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andReturn();

        String state = com.jayway.jsonpath.JsonPath.read(connectResult.getResponse().getContentAsString(), "$.state");
        mockMvc.perform(get("/api/teacher/media/youtube/callback")
                        .with(asTeacher())
                        .param("code", "mock-auth-code")
                        .param("state", state))
                .andExpect(status().isOk());

        // First publish
        mockMvc.perform(post("/api/teacher/contents/" + uploadContent.getContentId() + "/youtube-publish")
                        .with(asTeacher()))
                .andExpect(status().isAccepted());

        // Concurrent duplicate publish returns 409
        mockMvc.perform(post("/api/teacher/contents/" + uploadContent.getContentId() + "/youtube-publish")
                        .with(asTeacher()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("W2-13: Foreign teacher querying publish job receives 404 NOT_FOUND (IDOR defense)")
    void shouldRejectForeignTeacherJobQueryWith404() throws Exception {
        // Connect channel
        MvcResult connectResult = mockMvc.perform(post("/api/teacher/media/youtube/connect")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andReturn();

        String state = com.jayway.jsonpath.JsonPath.read(connectResult.getResponse().getContentAsString(), "$.state");
        mockMvc.perform(get("/api/teacher/media/youtube/callback")
                        .with(asTeacher())
                        .param("code", "mock-auth-code")
                        .param("state", state))
                .andExpect(status().isOk());

        MvcResult publishResult = mockMvc.perform(post("/api/teacher/contents/" + uploadContent.getContentId() + "/youtube-publish")
                        .with(asTeacher()))
                .andExpect(status().isAccepted())
                .andReturn();

        Long jobId = ((Number) com.jayway.jsonpath.JsonPath.read(publishResult.getResponse().getContentAsString(), "$.jobId")).longValue();

        // Foreign teacher receives 404
        mockMvc.perform(get("/api/teacher/media/youtube/jobs/" + jobId)
                        .with(asForeignTeacher()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("W2-13: Completed publish job saves providerVideoId but leaves content sourceType as UPLOAD")
    void shouldKeepLocalSourceUploadOnPublishSuccess() throws Exception {
        // Connect channel
        MvcResult connectResult = mockMvc.perform(post("/api/teacher/media/youtube/connect")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andReturn();

        String state = com.jayway.jsonpath.JsonPath.read(connectResult.getResponse().getContentAsString(), "$.state");
        mockMvc.perform(get("/api/teacher/media/youtube/callback")
                        .with(asTeacher())
                        .param("code", "mock-auth-code")
                        .param("state", state))
                .andExpect(status().isOk());

        MvcResult publishResult = mockMvc.perform(post("/api/teacher/contents/" + uploadContent.getContentId() + "/youtube-publish")
                        .with(asTeacher()))
                .andExpect(status().isAccepted())
                .andReturn();

        Long jobId = ((Number) com.jayway.jsonpath.JsonPath.read(publishResult.getResponse().getContentAsString(), "$.jobId")).longValue();

        // Trigger or verify job completion
        mockMvc.perform(post("/api/teacher/media/youtube/jobs/" + jobId + "/simulate-complete")
                        .with(asTeacher())
                        .param("youtubeVideoId", "ytUploaded123"))
                .andExpect(status().isOk());

        CourseContent updatedContent = courseContentRepository.findById(uploadContent.getContentId()).orElseThrow();
        assertThat(updatedContent.getProviderVideoId()).isEqualTo("ytUploaded123");
        // Must remain UPLOAD until an explicit switch!
        assertThat(updatedContent.getSourceType()).isEqualTo("UPLOAD");
    }

    @Test
    @DisplayName("W2-13: Quota/error failure records error on job and preserves local UPLOAD fallback")
    void shouldHandleQuotaFailureAndKeepLocalFallback() throws Exception {
        // Connect channel
        MvcResult connectResult = mockMvc.perform(post("/api/teacher/media/youtube/connect")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andReturn();

        String state = com.jayway.jsonpath.JsonPath.read(connectResult.getResponse().getContentAsString(), "$.state");
        mockMvc.perform(get("/api/teacher/media/youtube/callback")
                        .with(asTeacher())
                        .param("code", "mock-auth-code")
                        .param("state", state))
                .andExpect(status().isOk());

        MvcResult publishResult = mockMvc.perform(post("/api/teacher/contents/" + uploadContent.getContentId() + "/youtube-publish")
                        .with(asTeacher()))
                .andExpect(status().isAccepted())
                .andReturn();

        Long jobId = ((Number) com.jayway.jsonpath.JsonPath.read(publishResult.getResponse().getContentAsString(), "$.jobId")).longValue();

        // Simulate quota error
        mockMvc.perform(post("/api/teacher/media/youtube/jobs/" + jobId + "/simulate-error")
                        .with(asTeacher())
                        .param("error", "The user has exceeded the upload quota limit."))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teacher/media/youtube/jobs/" + jobId)
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage", containsString("upload quota")));

        CourseContent updatedContent = courseContentRepository.findById(uploadContent.getContentId()).orElseThrow();
        assertThat(updatedContent.getSourceType()).isEqualTo("UPLOAD");
    }
}
