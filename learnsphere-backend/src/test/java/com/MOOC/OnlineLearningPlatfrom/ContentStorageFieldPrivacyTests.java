package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Post-W6 audit fix (W2-12, "no raw video URLs"): curriculum reads stay public for course
 * detail, but storage internals (file path, storage key, external source URL) are never
 * serialised. Playback goes only through the signed descriptor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ContentStorageFieldPrivacyTests extends PostgresIntegrationTest {

    private static final String[] SECRETS = {"uploads/private/lecture-01.mp4", "media/2026/abc123.mp4", "https://cdn.example.test/raw/lecture.m3u8"};

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private CourseContentRepository courseContentRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private UserDetailsService userDetailsService;

    private Course course;
    private Module module;
    private CourseContent content;

    @BeforeEach
    void setUp() {
        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        course = new Course();
        course.setTitle("Private Storage Course");
        course.setDescription("Storage fields must not leak");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        module = new Module();
        module.setCourse(course);
        module.setTitle("Module 1");
        module.setPosition(1);
        module = moduleRepository.save(module);

        content = new CourseContent();
        content.setModule(module);
        content.setTitle("Lecture with private storage");
        content.setContentType("VIDEO");
        content.setPosition(1);
        content.setDurationSeconds(600);
        content.setSourceType("UPLOAD");
        content.setFilePath(SECRETS[0]);
        content.setStorageKey(SECRETS[1]);
        content.setExternalUrl(SECRETS[2]);
        content = courseContentRepository.save(content);
        module.getContents().add(content);
        entityManager.flush();
    }

    private void assertNoStorageFields(String body) {
        assertThat(body).contains("Lecture with private storage"); // the curriculum itself is still served
        for (String secret : SECRETS) assertThat(body).doesNotContain(secret);
        assertThat(body).doesNotContain("\"filePath\"").doesNotContain("\"storageKey\"").doesNotContain("\"externalUrl\"");
    }

    @Test
    @DisplayName("GET /api/contents/{id} serves the content without storage fields")
    void singleContent() throws Exception {
        String body = mockMvc.perform(get("/api/contents/{id}", content.getContentId())
                        .with(user(userDetailsService.loadUserByUsername("external@learnsphere.com"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertNoStorageFields(body);
    }

    @Test
    @DisplayName("GET /api/modules/{id}/contents serves contents without storage fields")
    void moduleContents() throws Exception {
        String body = mockMvc.perform(get("/api/modules/{id}/contents", module.getModuleId())
                        .with(user(userDetailsService.loadUserByUsername("external@learnsphere.com"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertNoStorageFields(body);
    }

    @Test
    @DisplayName("GET /api/courses/{id}/modules never nests storage fields")
    void courseModules() throws Exception {
        String body = mockMvc.perform(get("/api/courses/{id}/modules", course.getId())
                        .with(user(userDetailsService.loadUserByUsername("external@learnsphere.com"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        for (String secret : SECRETS) assertThat(body).doesNotContain(secret);
    }
}
