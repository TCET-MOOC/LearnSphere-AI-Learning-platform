package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LectureRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.ModuleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserProgressRepository;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W2-05 integration tests: Lecture -> CourseContent 1:1 migration.
 * Legacy lecture API becomes read-only (A-05, [B-F1]).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LectureContentMigrationTests extends PostgresIntegrationTest {

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
    private LectureRepository lectureRepository;

    @Autowired
    private CourseContentRepository courseContentRepository;

    @Autowired
    private UserProgressRepository userProgressRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private UserAccount teacher;
    private UserAccount student;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");

        Department department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Computer Science & Engineering");
            return departmentRepository.save(d);
        });

        testCourse = new Course();
        testCourse.setTitle("Legacy Lecture Test Course");
        testCourse.setDescription("Testing lecture to content migration");
        testCourse.setTeacher(teacher);
        testCourse.setDepartment(department);
        testCourse.setStatus(Course.Status.LIVE);
        testCourse.setPrice(BigDecimal.ZERO);
        testCourse = courseRepository.save(testCourse);

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(testCourse);
        enrollment.setUserId(student.getUserId());
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);
    }

    private RequestPostProcessor asUser(UserAccount account) {
        return user(userDetailsService.loadUserByUsername(account.getEmail()));
    }

    @Test
    @DisplayName("Legacy lecture authoring (POST) is retired and returns 400 Bad Request (A-05)")
    void legacyLectureCreationIsRetired() throws Exception {
        mockMvc.perform(post("/api/teacher/courses/" + testCourse.getId() + "/lectures")
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Old Lecture\",\"videoUrl\":\"https://example.com/video.mp4\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Legacy bulk lecture creation (POST) is retired and returns 400 Bad Request (A-05)")
    void legacyBulkLectureCreationIsRetired() throws Exception {
        mockMvc.perform(post("/api/teacher/courses/" + testCourse.getId() + "/bulk-lectures")
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"title\":\"Old Lecture 1\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Legacy lecture mutation (PUT) is retired and returns 400 Bad Request (A-05)")
    void legacyLectureUpdateIsRetired() throws Exception {
        Lecture lecture = new Lecture();
        lecture.setCourse(testCourse);
        lecture.setTitle("Existing Lecture");
        lecture.setNumber(1);
        lecture = lectureRepository.save(lecture);

        mockMvc.perform(put("/api/teacher/lectures/" + lecture.getId())
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Legacy lecture deletion (DELETE) is retired and returns 400 Bad Request (A-05)")
    void legacyLectureDeleteIsRetired() throws Exception {
        Lecture lecture = new Lecture();
        lecture.setCourse(testCourse);
        lecture.setTitle("Existing Lecture");
        lecture.setNumber(1);
        lecture = lectureRepository.save(lecture);

        mockMvc.perform(delete("/api/teacher/lectures/" + lecture.getId())
                        .with(asUser(teacher)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("GET /api/courses/{id}/lectures remains functional as a read-only surface")
    void legacyLectureReadEndpointRemainsFunctional() throws Exception {
        Lecture lecture = new Lecture();
        lecture.setCourse(testCourse);
        lecture.setTitle("Read Only Lecture");
        lecture.setNumber(1);
        lecture.setDuration(600);
        lecture.setVideoUrl("https://example.com/stream.mp4");
        lecture.setStatus(Lecture.Status.PUBLISHED);
        lectureRepository.save(lecture);

        mockMvc.perform(get("/api/courses/" + testCourse.getId() + "/lectures")
                        .with(asUser(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Read Only Lecture"));
    }

    @Test
    @DisplayName("Marking lecture complete also populates canonical content_id in user_progress")
    void markingLectureCompletePopulatesCanonicalContent() throws Exception {
        Module module = new Module();
        module.setCourse(testCourse);
        module.setTitle("Auto Module");
        module.setPosition(1);
        module = moduleRepository.save(module);

        Lecture lecture = new Lecture();
        lecture.setCourse(testCourse);
        lecture.setTitle("Video Lecture");
        lecture.setNumber(1);
        lecture.setDuration(900);
        lecture.setVideoUrl("https://example.com/v1.mp4");
        lecture.setStatus(Lecture.Status.PUBLISHED);
        lecture = lectureRepository.save(lecture);

        CourseContent content = new CourseContent();
        content.setModule(module);
        content.setContentType("VIDEO");
        content.setTitle("Video Lecture");
        content.setFilePath("https://example.com/v1.mp4");
        content.setDurationSeconds(900);
        content.setPosition(1);
        content.setLegacyLectureId(lecture.getId());
        courseContentRepository.save(content);

        mockMvc.perform(post("/api/student/lectures/" + lecture.getId() + "/complete")
                        .with(asUser(student)))
                .andExpect(status().isOk());

        UserProgress progress = userProgressRepository.findByUserIdAndLecture_Id(student.getUserId(), lecture.getId())
                .orElse(null);
        assertThat(progress).isNotNull();
        assertThat(progress.getContent()).isNotNull();
        assertThat(progress.getContent().getContentId()).isEqualTo(content.getContentId());
    }
}
