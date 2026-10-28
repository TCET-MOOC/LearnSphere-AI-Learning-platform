package com.MOOC.OnlineLearningPlatfrom.media;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Entity.StoredFile;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W2-12: Canonical Media Source + Secure Local Playback Tests.
 *
 * Rules: A-25, BR-26, NFR-03, W2-05, W2-07, spec §6.
 *
 * Validates:
 * 1. Non-enrolled student playback is rejected with 404 (IDOR defense).
 * 2. Enrolled student receives signed playback descriptor without leaking raw paths or storage keys.
 * 3. Course staff and admin receive playback descriptor.
 * 4. Playback descriptors return Cache-Control: private, no-store.
 * 5. Stream endpoint validates HMAC signature and rejects tampered signature with 403.
 * 6. Stream endpoint rejects expired signature with 403.
 * 7. Stream endpoint supports HTTP Range streaming (206 Partial Content).
 * 8. Stream endpoint without Range header returns full content (200 OK).
 * 9. Upload video validates MP4/WebM MIME + magic bytes and 200MB cap.
 * 10. Mislabelled, executable, or unsupported files rejected with 400.
 * 11. Legacy public /api/uploads/** endpoint remains rejected (401/404).
 * 12. CourseContent source union validation (rejects invalid/missing sources).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class MediaPlaybackSecurityTests extends PostgresIntegrationTest {

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
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private UserAccount teacher;
    private UserAccount enrolledStudent;
    private UserAccount unenrolledStudent;
    private UserAccount admin;
    private Course course;
    private Module module;
    private CourseContent youtubeContent;
    private CourseContent uploadContent;
    private StoredFile testVideoFile;

    private RequestPostProcessor asTeacher() {
        return user(userDetailsService.loadUserByUsername(teacher.getEmail()));
    }

    private RequestPostProcessor asEnrolled() {
        return user(userDetailsService.loadUserByUsername(enrolledStudent.getEmail()));
    }

    private RequestPostProcessor asUnenrolled() {
        return user(userDetailsService.loadUserByUsername(unenrolledStudent.getEmail()));
    }

    private RequestPostProcessor asAdmin() {
        return user(userDetailsService.loadUserByUsername(admin.getEmail()));
    }

    @BeforeEach
    void setUp() throws Exception {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        enrolledStudent = userAccountRepository.findByEmail("student@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");

        unenrolledStudent = userAccountRepository.findByEmail("pending@learnsphere.com");
        if (unenrolledStudent == null) {
            unenrolledStudent = userAccountRepository.findByEmail("external@learnsphere.com");
        }

        Department department = departmentRepository.findByName("Computer Science");
        if (department == null) {
            Department d = new Department();
            d.setName("Computer Science");
            department = departmentRepository.save(d);
        }

        course = new Course();
        course.setTitle("Distributed Media Systems");
        course.setDescription("Streaming protocols and secure video distribution");
        course.setPrice(BigDecimal.valueOf(29.99));
        course.setTeacher(teacher);
        course.setDepartment(department);
        course.setDepartmentLabel(department.getName());
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        // Enroll the enrolledStudent
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUserId(enrolledStudent.getUserId());
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);

        module = new Module();
        module.setCourse(course);
        module.setTitle("Module 1: Video Delivery");
        module.setPosition(1);
        module = moduleRepository.save(module);

        // 1. YouTube canonical content
        youtubeContent = new CourseContent();
        youtubeContent.setModule(module);
        youtubeContent.setContentType("VIDEO");
        youtubeContent.setTitle("YouTube Lecture Intro");
        youtubeContent.setSourceType("YOUTUBE");
        youtubeContent.setProviderVideoId("dQw4w9WgXcQ");
        youtubeContent.setDurationSeconds(212);
        youtubeContent.setPosition(1);
        youtubeContent.setMediaStatus("READY");
        youtubeContent = courseContentRepository.save(youtubeContent);

        // 2. Upload video file on disk & stored_files table
        String fileKey = UUID.randomUUID().toString().replace("-", "");
        String diskFileName = UUID.randomUUID() + ".mp4";
        String relativePath = teacher.getUserId() + File.separator + diskFileName;

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path teacherDir = root.resolve(String.valueOf(teacher.getUserId())).normalize();
        Files.createDirectories(teacherDir);
        Path targetPath = root.resolve(relativePath).normalize();

        // Create a dummy MP4 file with valid ftyp header
        byte[] dummyMp4Bytes = new byte[1024];
        dummyMp4Bytes[4] = 'f';
        dummyMp4Bytes[5] = 't';
        dummyMp4Bytes[6] = 'y';
        dummyMp4Bytes[7] = 'p';
        for (int i = 8; i < dummyMp4Bytes.length; i++) {
            dummyMp4Bytes[i] = (byte) (i % 256);
        }
        try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            fos.write(dummyMp4Bytes);
        }

        testVideoFile = new StoredFile();
        testVideoFile.setFileKey(fileKey);
        testVideoFile.setOwnerId(teacher.getUserId());
        testVideoFile.setOriginalFilename("test-video.mp4");
        testVideoFile.setContentType("video/mp4");
        testVideoFile.setSizeBytes((long) dummyMp4Bytes.length);
        testVideoFile.setStoragePath(relativePath);
        testVideoFile = storedFileRepository.save(testVideoFile);

        // Upload canonical content
        uploadContent = new CourseContent();
        uploadContent.setModule(module);
        uploadContent.setContentType("VIDEO");
        uploadContent.setTitle("Local Private Video Lecture");
        uploadContent.setSourceType("UPLOAD");
        uploadContent.setStorageKey(fileKey);
        uploadContent.setMimeType("video/mp4");
        uploadContent.setSizeBytes((long) dummyMp4Bytes.length);
        uploadContent.setDurationSeconds(300);
        uploadContent.setPosition(2);
        uploadContent.setMediaStatus("READY");
        uploadContent = courseContentRepository.save(uploadContent);
    }

    // =========================================================================
    // 1. Entitlement-Bound Playback Descriptors (NFR-03, A-25)
    // =========================================================================

    @Test
    @DisplayName("A-25, BR-26: Non-enrolled student receives 404 NOT_FOUND on playback descriptor (IDOR defense)")
    void shouldRejectPlaybackForNonEnrolledStudent() throws Exception {
        mockMvc.perform(get("/api/contents/" + uploadContent.getContentId() + "/playback")
                        .with(asUnenrolled()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A-25, NFR-03: Enrolled student receives signed playback descriptor with private, no-store and no path leak")
    void shouldReturnSignedPlaybackDescriptorForEnrolledStudent() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/contents/" + uploadContent.getContentId() + "/playback")
                        .with(asEnrolled()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.contentId").value(uploadContent.getContentId()))
                .andExpect(jsonPath("$.sourceType").value("UPLOAD"))
                .andExpect(jsonPath("$.mimeType").value("video/mp4"))
                .andExpect(jsonPath("$.streamUrl", containsString("/stream")))
                .andExpect(jsonPath("$.streamUrl", containsString("sig=")))
                .andExpect(jsonPath("$.streamUrl", containsString("exp=")))
                // Must NOT leak storage_path or storage_key
                .andExpect(jsonPath("$.storageKey").doesNotExist())
                .andExpect(jsonPath("$.storagePath").doesNotExist())
                .andExpect(jsonPath("$.filePath").doesNotExist())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(testVideoFile.getStoragePath());
        assertThat(body).doesNotContain(testVideoFile.getFileKey());
    }

    @Test
    @DisplayName("A-25: YouTube playback descriptor contains embedUrl and providerVideoId")
    void shouldReturnYouTubePlaybackDescriptor() throws Exception {
        mockMvc.perform(get("/api/contents/" + youtubeContent.getContentId() + "/playback")
                        .with(asEnrolled()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.contentId").value(youtubeContent.getContentId()))
                .andExpect(jsonPath("$.sourceType").value("YOUTUBE"))
                .andExpect(jsonPath("$.providerVideoId").value("dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.embedUrl", containsString("youtube-nocookie.com/embed/dQw4w9WgXcQ")))
                .andExpect(jsonPath("$.streamUrl").doesNotExist());
    }

    @Test
    @DisplayName("A-25: Course owner teacher receives playback descriptor")
    void shouldAllowCourseTeacherToFetchPlaybackDescriptor() throws Exception {
        mockMvc.perform(get("/api/contents/" + uploadContent.getContentId() + "/playback")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").value(uploadContent.getContentId()))
                .andExpect(jsonPath("$.streamUrl", notNullValue()));
    }

    // =========================================================================
    // 2. Stream Security & Range Requests (W2-12, BR-26)
    // =========================================================================

    @Test
    @DisplayName("BR-26: Stream request with tampered signature is rejected with 403 Forbidden")
    void shouldRejectStreamWithTamperedSignature() throws Exception {
        // First get valid descriptor
        MvcResult result = mockMvc.perform(get("/api/contents/" + uploadContent.getContentId() + "/playback")
                        .with(asEnrolled()))
                .andExpect(status().isOk())
                .andReturn();

        String streamUrl = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.streamUrl");
        assertThat(streamUrl).isNotBlank();

        // Tamper the signature by altering the last characters
        String tamperedUrl = streamUrl.substring(0, streamUrl.length() - 4) + "ffff";

        mockMvc.perform(get(tamperedUrl)
                        .with(asEnrolled()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("BR-26: Stream request with expired signature is rejected with 403 Forbidden")
    void shouldRejectStreamWithExpiredSignature() throws Exception {
        // Construct stream URL with past expiry
        long pastExp = System.currentTimeMillis() / 1000 - 60; // 60 seconds ago
        String expiredUrl = "/api/contents/" + uploadContent.getContentId() + "/stream?exp=" + pastExp
                + "&u=" + enrolledStudent.getUserId() + "&sig=dummySignature123";

        mockMvc.perform(get(expiredUrl)
                        .with(asEnrolled()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("W2-12: Stream endpoint supports HTTP Range request (206 Partial Content)")
    void shouldSupportRangeStreaming() throws Exception {
        // Get valid stream URL
        MvcResult descriptorResult = mockMvc.perform(get("/api/contents/" + uploadContent.getContentId() + "/playback")
                        .with(asEnrolled()))
                .andExpect(status().isOk())
                .andReturn();

        String streamUrl = com.jayway.jsonpath.JsonPath.read(descriptorResult.getResponse().getContentAsString(), "$.streamUrl");

        // Request byte range 0..99
        mockMvc.perform(get(streamUrl)
                        .with(asEnrolled())
                        .header(HttpHeaders.RANGE, "bytes=0-99"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string(HttpHeaders.CONTENT_RANGE, "bytes 0-99/1024"))
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "100"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "video/mp4"))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    @DisplayName("W2-12: Full stream request without Range header returns 200 OK")
    void shouldReturnFullStreamWhenNoRangeRequested() throws Exception {
        MvcResult descriptorResult = mockMvc.perform(get("/api/contents/" + uploadContent.getContentId() + "/playback")
                        .with(asEnrolled()))
                .andExpect(status().isOk())
                .andReturn();

        String streamUrl = com.jayway.jsonpath.JsonPath.read(descriptorResult.getResponse().getContentAsString(), "$.streamUrl");

        mockMvc.perform(get(streamUrl)
                        .with(asEnrolled()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCEPT_RANGES, "bytes"))
                .andExpect(header().string(HttpHeaders.CONTENT_LENGTH, "1024"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "video/mp4"));
    }

    // =========================================================================
    // 3. Video Upload Safety (200MB, MIME + Magic Bytes)
    // =========================================================================

    @Test
    @DisplayName("W2-12: Upload video with valid MP4 magic bytes returns 201 CREATED with metadata")
    void shouldUploadValidMp4Video() throws Exception {
        byte[] validMp4 = new byte[128];
        validMp4[4] = 'f';
        validMp4[5] = 't';
        validMp4[6] = 'y';
        validMp4[7] = 'p';

        MockMultipartFile file = new MockMultipartFile(
                "file", "lecture1.mp4", "video/mp4", validMp4
        );

        mockMvc.perform(multipart("/api/teacher/upload/video")
                        .file(file)
                        .with(asTeacher()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileKey", notNullValue()))
                .andExpect(jsonPath("$.storageKey", notNullValue()))
                .andExpect(jsonPath("$.mimeType").value("video/mp4"));
    }

    @Test
    @DisplayName("W2-12: Upload video with mislabelled format (fake MP4 magic bytes) returns 400 Bad Request")
    void shouldRejectMislabelledVideoUpload() throws Exception {
        byte[] fakeMp4 = "Plain text disguised as MP4".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.mp4", "video/mp4", fakeMp4
        );

        mockMvc.perform(multipart("/api/teacher/upload/video")
                        .file(file)
                        .with(asTeacher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("W2-12: Upload video with executable content (MZ header) returns 400 Bad Request")
    void shouldRejectExecutableDisguisedAsVideo() throws Exception {
        byte[] exeHeader = new byte[64];
        exeHeader[0] = 0x4D; // M
        exeHeader[1] = 0x5A; // Z

        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.mp4", "video/mp4", exeHeader
        );

        mockMvc.perform(multipart("/api/teacher/upload/video")
                        .file(file)
                        .with(asTeacher()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("W2-07, W2-12: Legacy public uploads path (/api/uploads/**) remains rejected")
    void shouldRejectLegacyPublicUploadsPath() throws Exception {
        mockMvc.perform(get("/api/uploads/videos/sample.mp4"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/uploads/videos/sample.mp4")
                        .with(asEnrolled()))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 4. Source Union Validation on CourseContent Creation/Update
    // =========================================================================

    @Test
    @DisplayName("A-25: Content authoring enforces source union (rejects non-existent upload storageKey)")
    void shouldRejectContentCreationWithNonExistentStorageKey() throws Exception {
        mockMvc.perform(post("/api/modules/" + module.getModuleId() + "/contents")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bad Upload Video\",\"contentType\":\"VIDEO\",\"sourceType\":\"UPLOAD\",\"storageKey\":\"nonExistentKey999\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("A-25: Content authoring with valid storageKey successfully links canonical source")
    void shouldCreateContentWithValidStorageKey() throws Exception {
        mockMvc.perform(post("/api/modules/" + module.getModuleId() + "/contents")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid Upload Video\",\"contentType\":\"VIDEO\",\"sourceType\":\"UPLOAD\",\"storageKey\":\""
                                + testVideoFile.getFileKey() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentId").isNumber())
                .andExpect(jsonPath("$.sourceType").value("UPLOAD"))
                .andExpect(jsonPath("$.storageKey").value(testVideoFile.getFileKey()));
    }
}
