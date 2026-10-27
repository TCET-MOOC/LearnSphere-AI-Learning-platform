package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.ModuleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W2-09 integration tests: YouTube import into canonical content.
 * Rules: A-05; spec §6.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class YouTubeImportCanonicalContentTests extends PostgresIntegrationTest {

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

    private UserAccount teacher;
    private UserAccount student;
    private UserAccount otherTeacher;
    private Course teacherCourse;
    private Course otherTeacherCourse;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        otherTeacher = userAccountRepository.findByEmail("pending@learnsphere.com");

        Department dept = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Engineering");
            return departmentRepository.save(d);
        });

        teacherCourse = new Course();
        teacherCourse.setTitle("Distributed Systems");
        teacherCourse.setTeacher(teacher);
        teacherCourse.setDepartment(dept);
        teacherCourse.setPrice(BigDecimal.ZERO);
        teacherCourse.setStatus(Course.Status.LIVE);
        teacherCourse = courseRepository.save(teacherCourse);

        otherTeacherCourse = new Course();
        otherTeacherCourse.setTitle("Foreign Course");
        otherTeacherCourse.setTeacher(otherTeacher);
        otherTeacherCourse.setDepartment(dept);
        otherTeacherCourse.setPrice(BigDecimal.ZERO);
        otherTeacherCourse.setStatus(Course.Status.LIVE);
        otherTeacherCourse = courseRepository.save(otherTeacherCourse);
    }

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("Unauthenticated request to import-playlist returns 401 Unauthorized")
    void importPlaylist_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/teacher/youtube/import-playlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playlistUrl\":\"https://www.youtube.com/playlist?list=PL123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Student request to import-playlist returns 403 Forbidden")
    void importPlaylist_asStudent_returns403() throws Exception {
        mockMvc.perform(post("/api/teacher/youtube/import-playlist")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playlistUrl\":\"https://www.youtube.com/playlist?list=PL123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("Blank playlistUrl returns 400 Bad Request")
    void importPlaylist_blankUrl_returns400() throws Exception {
        mockMvc.perform(post("/api/teacher/youtube/import-playlist")
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playlistUrl\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Preview import without courseId returns playlist metadata and lectures without mutating DB")
    void importPlaylist_previewWithoutCourse_returnsPlaylistItems() throws Exception {
        long initialContentCount = courseContentRepository.count();

        mockMvc.perform(post("/api/teacher/youtube/import-playlist")
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playlistUrl\":\"https://www.youtube.com/watch?v=ukzFI9rgwfU\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playlistTitle").isNotEmpty())
                .andExpect(jsonPath("$.lectures").isArray())
                .andExpect(jsonPath("$.totalLectures").value(1));

        assertThat(courseContentRepository.count()).isEqualTo(initialContentCount);
    }

    @Test
    @DisplayName("Importing playlist with owned courseId creates Module and canonical CourseContent items")
    void importPlaylist_withOwnedCourseId_importsIntoCanonicalCourseContent() throws Exception {
        String body = """
            {
              "playlistUrl": "https://www.youtube.com/playlist?list=PL_DEMO_AI_PLAYLIST",
              "courseId": %d
            }
            """.formatted(teacherCourse.getId());

        mockMvc.perform(post("/api/teacher/youtube/import-playlist")
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playlistTitle").isNotEmpty())
                .andExpect(jsonPath("$.totalLectures").isNumber())
                .andExpect(jsonPath("$.importedContents").isArray())
                .andExpect(jsonPath("$.importedContents.length()").value(org.hamcrest.Matchers.greaterThan(0)));

        List<Module> modules = moduleRepository.findByCourse_IdOrderByPositionAsc(teacherCourse.getId());
        assertThat(modules).isNotEmpty();
        Module createdModule = modules.get(0);

        List<CourseContent> contents = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(createdModule.getModuleId());
        assertThat(contents).isNotEmpty();
        assertThat(contents).allMatch(c -> "VIDEO".equalsIgnoreCase(c.getContentType()));
        assertThat(contents).allMatch(c -> c.getFilePath() != null && !c.getFilePath().isBlank());

        // Contiguous 1..n ordering check
        for (int i = 0; i < contents.size(); i++) {
            assertThat(contents.get(i).getPosition()).isEqualTo(i + 1);
        }
    }

    @Test
    @DisplayName("Importing playlist with foreign courseId returns 404 Not Found (IDOR defense)")
    void importPlaylist_withForeignCourseId_returns404() throws Exception {
        String body = """
            {
              "playlistUrl": "https://www.youtube.com/watch?v=ukzFI9rgwfU",
              "courseId": %d
            }
            """.formatted(otherTeacherCourse.getId());

        mockMvc.perform(post("/api/teacher/youtube/import-playlist")
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Importing playlist into an existing specific moduleId appends contiguously")
    void importPlaylist_intoSpecificModule_appendsContiguously() throws Exception {
        Module existingModule = new Module();
        existingModule.setCourse(teacherCourse);
        existingModule.setTitle("Existing Foundation Module");
        existingModule.setPosition(1);
        existingModule = moduleRepository.save(existingModule);

        CourseContent preExistingContent = new CourseContent();
        preExistingContent.setModule(existingModule);
        preExistingContent.setContentType("VIDEO");
        preExistingContent.setTitle("Lecture 0: Introduction");
        preExistingContent.setFilePath("https://youtube.com/v0");
        preExistingContent.setDurationSeconds(600);
        preExistingContent.setPosition(1);
        courseContentRepository.save(preExistingContent);

        String body = """
            {
              "playlistUrl": "https://www.youtube.com/watch?v=ukzFI9rgwfU",
              "courseId": %d,
              "moduleId": %d
            }
            """.formatted(teacherCourse.getId(), existingModule.getModuleId());

        mockMvc.perform(post("/api/teacher/youtube/import-playlist")
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedContents").isArray());

        List<CourseContent> allContents = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(existingModule.getModuleId());
        assertThat(allContents.size()).isGreaterThanOrEqualTo(2);
        for (int i = 0; i < allContents.size(); i++) {
            assertThat(allContents.get(i).getPosition()).isEqualTo(i + 1);
        }
    }

    @Test
    @DisplayName("Foreign moduleId under different course returns 404 Not Found")
    void importPlaylist_withMismatchedModuleId_returns404() throws Exception {
        Module foreignModule = new Module();
        foreignModule.setCourse(otherTeacherCourse);
        foreignModule.setTitle("Foreign Module");
        foreignModule.setPosition(1);
        foreignModule = moduleRepository.save(foreignModule);

        String body = """
            {
              "playlistUrl": "https://www.youtube.com/watch?v=ukzFI9rgwfU",
              "courseId": %d,
              "moduleId": %d
            }
            """.formatted(teacherCourse.getId(), foreignModule.getModuleId());

        mockMvc.perform(post("/api/teacher/youtube/import-playlist")
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("getTranscript returns transcript text and caches across multiple invocations")
    void getTranscript_cachesAcrossMultipleCalls() throws Exception {
        String videoId = "testVideo123";

        String firstCall = mockMvc.perform(get("/api/teacher/youtube/transcript/{videoId}", videoId)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoId").value(videoId))
                .andExpect(jsonPath("$.transcriptText").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String secondCall = mockMvc.perform(get("/api/teacher/youtube/transcript/{videoId}", videoId)
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoId").value(videoId))
                .andExpect(jsonPath("$.transcriptText").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        assertThat(firstCall).isEqualTo(secondCall);
    }

    @Test
    @DisplayName("getTranscript with blank videoId returns 400 Bad Request")
    void getTranscript_blankVideoId_returns400() throws Exception {
        mockMvc.perform(get("/api/teacher/youtube/transcript/{videoId}", " ")
                        .with(as("teacher@learnsphere.com")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
