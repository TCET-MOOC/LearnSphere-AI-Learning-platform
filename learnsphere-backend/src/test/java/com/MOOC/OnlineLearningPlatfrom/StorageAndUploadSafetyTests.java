package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.StoredFile;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.StoredFileRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W2-07 integration tests: Storage port + upload safety.
 * Rules: spec §6, [B-F9], BR-26.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StorageAndUploadSafetyTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private StoredFileRepository storedFileRepository;

    private UserAccount teacher;
    private UserAccount otherTeacher;
    private UserAccount student;
    private UserAccount admin;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        otherTeacher = userAccountRepository.findByEmail("pending@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");
    }

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("Teacher uploads valid PDF -> 201 Created with opaque fileKey and /api/files/ url")
    void uploadResource_asTeacher_withValidPdf_returns201WithFileKeyAndUrl() throws Exception {
        byte[] pdfContent = "%PDF-1.5 \n%Valid test PDF content for learnsphere".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "lecture-notes.pdf", "application/pdf", pdfContent);

        String responseJson = mockMvc.perform(multipart("/api/teacher/upload/resource")
                        .file(file)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileKey").isString())
                .andExpect(jsonPath("$.url").value(startsWith("/api/files/")))
                .andReturn().getResponse().getContentAsString();

        assertThat(responseJson).doesNotContain("/api/uploads/");

        StoredFile stored = storedFileRepository.findAll().stream()
                .filter(s -> s.getOriginalFilename().equals("lecture-notes.pdf"))
                .findFirst().orElseThrow();
        assertThat(stored.getOwnerId()).isEqualTo(teacher.getUserId());
        assertThat(stored.getContentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Teacher uploads file with spoofed MIME but invalid magic bytes -> 400 Bad Request")
    void uploadResource_withMismatchedMagicBytes_returns400BadRequest() throws Exception {
        // Declared as PDF, but contains Windows PE executable (MZ header)
        byte[] fakePdf = new byte[]{'M', 'Z', 0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        MockMultipartFile file = new MockMultipartFile("file", "exploit.pdf", "application/pdf", fakePdf);

        mockMvc.perform(multipart("/api/teacher/upload/resource")
                        .file(file)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Teacher uploads shell script -> 400 Bad Request")
    void uploadResource_withExecutableScript_returns400BadRequest() throws Exception {
        byte[] script = "#!/bin/bash\necho hello".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "script.txt", "text/plain", script);

        mockMvc.perform(multipart("/api/teacher/upload/resource")
                        .file(file)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Teacher uploads valid MP4 video -> 201 Created with opaque fileKey and url")
    void uploadVideo_withValidMp4_returns201WithFileKeyAndUrl() throws Exception {
        // Valid MP4 header: 4 bytes length, then 'ftyp'
        byte[] mp4Header = new byte[]{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2'};
        MockMultipartFile file = new MockMultipartFile("file", "lecture1.mp4", "video/mp4", mp4Header);

        mockMvc.perform(multipart("/api/teacher/upload/video")
                        .file(file)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileKey").isString())
                .andExpect(jsonPath("$.url").value(startsWith("/api/files/")));
    }

    @Test
    @DisplayName("Teacher uploads non-video declared as video/mp4 -> 400 Bad Request")
    void uploadVideo_withNonVideoContent_returns400BadRequest() throws Exception {
        byte[] plainText = "This is not an mp4 video file at all".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "fake.mp4", "video/mp4", plainText);

        mockMvc.perform(multipart("/api/teacher/upload/video")
                        .file(file)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Owner can download file via GET /api/files/{key} with Cache-Control: private, no-store")
    void getFile_asOwner_returns200AndPrivateNoStore() throws Exception {
        byte[] pdfContent = "%PDF-1.4 sample PDF body".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "guide.pdf", "application/pdf", pdfContent);

        String json = mockMvc.perform(multipart("/api/teacher/upload/resource")
                        .file(file)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String fileKey = com.jayway.jsonpath.JsonPath.read(json, "$.fileKey");

        mockMvc.perform(get("/api/files/{key}", fileKey)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    @DisplayName("Admin can download any file via GET /api/files/{key}")
    void getFile_asAdmin_returns200() throws Exception {
        byte[] pdfContent = "%PDF-1.4 admin sample PDF".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "admin-read.pdf", "application/pdf", pdfContent);

        String json = mockMvc.perform(multipart("/api/teacher/upload/resource")
                        .file(file)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String fileKey = com.jayway.jsonpath.JsonPath.read(json, "$.fileKey");

        mockMvc.perform(get("/api/files/{key}", fileKey)
                        .with(as("admin@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    @DisplayName("Foreign teacher receives 404 Not Found on GET /api/files/{key} (IDOR protection)")
    void getFile_asForeignTeacher_returns404NotFound() throws Exception {
        byte[] pdfContent = "%PDF-1.4 private teacher document".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "secret.pdf", "application/pdf", pdfContent);

        String json = mockMvc.perform(multipart("/api/teacher/upload/resource")
                        .file(file)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String fileKey = com.jayway.jsonpath.JsonPath.read(json, "$.fileKey");

        mockMvc.perform(get("/api/files/{key}", fileKey)
                        .with(as("pending@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Path traversal key is rejected with 404 Not Found")
    void getFile_withPathTraversalKey_returns404NotFound() throws Exception {
        mockMvc.perform(get("/api/files/{key}", "..secret")
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Legacy public /api/uploads/** endpoint is removed: 401 unauthenticated, 404 authenticated")
    void legacyPublicUploadsEndpoint_isRemoved_returns404() throws Exception {
        // Unauthenticated request is no longer permitted (was permitAll)
        mockMvc.perform(get("/api/uploads/nonexistent-file.pdf"))
                .andExpect(status().isUnauthorized());

        // Authenticated request has no static handler mapping
        mockMvc.perform(get("/api/uploads/nonexistent-file.pdf")
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Student cannot upload files to /api/teacher/upload -> 403 Forbidden")
    void student_cannotUploadFiles_returns403Forbidden() throws Exception {
        byte[] pdfContent = "%PDF-1.4 student file".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "student.pdf", "application/pdf", pdfContent);

        mockMvc.perform(multipart("/api/teacher/upload/resource")
                        .file(file)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
