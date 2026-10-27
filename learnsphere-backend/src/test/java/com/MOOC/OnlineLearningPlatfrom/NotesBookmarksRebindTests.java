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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W2-08 integration tests: Notes/bookmarks rebind to CourseContent and duration bounds.
 * Rules: A-15, AC-14, BR-11, [E-F10].
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotesBookmarksRebindTests extends PostgresIntegrationTest {

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
    private UserAccount otherStudent;
    private Course course;
    private Module module;
    private CourseContent content;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        otherStudent = userAccountRepository.findByEmail("pending@learnsphere.com");

        Department dept = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Engineering");
            return departmentRepository.save(d);
        });

        course = new Course();
        course.setTitle("Operating Systems");
        course.setTeacher(teacher);
        course.setDepartment(dept);
        course.setPrice(BigDecimal.ZERO);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        module = new Module();
        module.setCourse(course);
        module.setTitle("Kernel Architecture");
        module.setPosition(1);
        module = moduleRepository.save(module);

        content = new CourseContent();
        content.setModule(module);
        content.setTitle("System Calls Deep Dive");
        content.setContentType("VIDEO");
        content.setDurationSeconds(300); // 5 minutes
        content.setPosition(1);
        content = courseContentRepository.save(content);
    }

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("Create note with contentId binds to CourseContent and auto-resolves course")
    void createNote_withContentId_bindsToCourseContentAndCourse() throws Exception {
        String body = """
            {
              "contentId": %d,
              "timestampSeconds": 120,
              "title": "Syscall Table",
              "content": "eax register holds the syscall number",
              "tags": ["kernel", "x86"]
            }
            """.formatted(content.getContentId());

        mockMvc.perform(post("/api/notes")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.contentId").value(content.getContentId()))
                .andExpect(jsonPath("$.courseId").value(course.getId()))
                .andExpect(jsonPath("$.timestampSeconds").value(120))
                .andExpect(jsonPath("$.title").value("Syscall Table"));
    }

    @Test
    @DisplayName("Create note with negative timestampSeconds returns 400 Bad Request")
    void createNote_withNegativeTimestamp_returns400() throws Exception {
        String body = """
            {
              "contentId": %d,
              "timestampSeconds": -15,
              "title": "Invalid Note"
            }
            """.formatted(content.getContentId());

        mockMvc.perform(post("/api/notes")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Create note with timestamp exceeding content duration returns 400 Bad Request")
    void createNote_withTimestampExceedingDuration_returns400() throws Exception {
        String body = """
            {
              "contentId": %d,
              "timestampSeconds": 450,
              "title": "Out of Bounds Note"
            }
            """.formatted(content.getContentId());

        mockMvc.perform(post("/api/notes")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Create bookmark with contentId binds to CourseContent")
    void createBookmark_withContentId_bindsToCourseContent() throws Exception {
        String body = """
            {
              "contentId": %d,
              "timestampSeconds": 240,
              "label": "Context switch explanation"
            }
            """.formatted(content.getContentId());

        mockMvc.perform(post("/api/bookmarks")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.contentId").value(content.getContentId()))
                .andExpect(jsonPath("$.timestampSeconds").value(240))
                .andExpect(jsonPath("$.label").value("Context switch explanation"));
    }

    @Test
    @DisplayName("Create bookmark with timestamp exceeding duration returns 400 Bad Request")
    void createBookmark_withTimestampExceedingDuration_returns400() throws Exception {
        String body = """
            {
              "contentId": %d,
              "timestampSeconds": 350,
              "label": "Beyond duration"
            }
            """.formatted(content.getContentId());

        mockMvc.perform(post("/api/bookmarks")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Query bookmarks by content returns student bookmarks")
    void getBookmarksByContent_returnsStudentBookmarks() throws Exception {
        String body = """
            {
              "contentId": %d,
              "timestampSeconds": 50,
              "label": "Intro to traps"
            }
            """.formatted(content.getContentId());

        mockMvc.perform(post("/api/bookmarks")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/bookmarks/content/{contentId}", content.getContentId())
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].contentId").value(content.getContentId()))
                .andExpect(jsonPath("$[0].label").value("Intro to traps"));
    }

    @Test
    @DisplayName("Foreign student cannot update or delete note (returns 404)")
    void foreignStudentCannotMutateNote_returns404() throws Exception {
        String body = """
            {
              "contentId": %d,
              "timestampSeconds": 100,
              "title": "Private Note",
              "content": "Secret text"
            }
            """.formatted(content.getContentId());

        String json = mockMvc.perform(post("/api/notes")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long noteId = ((Number) com.jayway.jsonpath.JsonPath.read(json, "$.id")).longValue();

        // Foreign student tries to update -> 404
        mockMvc.perform(put("/api/notes/{id}", noteId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hijacked\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Foreign student tries to delete -> 404
        mockMvc.perform(delete("/api/notes/{id}", noteId)
                        .with(as("pending@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("Foreign student cannot delete bookmark (returns 404)")
    void foreignStudentCannotDeleteBookmark_returns404() throws Exception {
        String body = """
            {
              "contentId": %d,
              "timestampSeconds": 100,
              "label": "Private bookmark"
            }
            """.formatted(content.getContentId());

        String json = mockMvc.perform(post("/api/bookmarks")
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long bookmarkId = ((Number) com.jayway.jsonpath.JsonPath.read(json, "$.id")).longValue();

        // Foreign student tries to delete -> 404
        mockMvc.perform(delete("/api/bookmarks/{id}", bookmarkId)
                        .with(as("pending@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
