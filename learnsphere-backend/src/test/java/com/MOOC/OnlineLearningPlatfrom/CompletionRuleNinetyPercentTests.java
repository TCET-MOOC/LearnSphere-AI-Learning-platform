package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W3-04 integration tests:
 * - Completion rule: completedAt at >= 90 %
 * - Un-complete on regression (completedAt reset to null if progress regresses below 90 %)
 * - Preserve completedAt timestamp on subsequent progress advances >= 90 %
 * - POST /api/student/lectures/{id}/complete sets progressPercent to 100.00 and completedAt
 * - POST /api/student/lectures/{id}/uncomplete clears progress to 0.00 and completedAt to null
 * - Course completion count and percentage count items at >= 90 % as completed
 * - Cache-Control: private, no-store headers on student progress endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompletionRuleNinetyPercentTests extends PostgresIntegrationTest {

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
    private LectureRepository lectureRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserProgressRepository userProgressRepository;

    private UserAccount student;
    private UserAccount teacher;
    private Course course;
    private Lecture lecture1;
    private Lecture lecture2;
    private CourseContent content1;
    private CourseContent content2;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");

        Department department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Engineering W304");
            return departmentRepository.save(d);
        });

        course = new Course();
        course.setTitle("Distributed Systems 90 Percent Completion");
        course.setDescription("Course for W3-04 completion rule tests");
        course.setDepartment(department);
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course.setPrice(BigDecimal.ZERO);
        course = courseRepository.save(course);

        // Module
        Module module = new Module();
        module.setCourse(course);
        module.setTitle("Module 1");
        module.setPosition(1);
        module = moduleRepository.save(module);

        // Lecture 1 & Content 1
        lecture1 = new Lecture();
        lecture1.setTitle("Lecture 1 - Consensus");
        lecture1.setCourse(course);
        lecture1.setNumber(1);
        lecture1.setDuration(1200);
        lecture1.setStatus(Lecture.Status.PUBLISHED);
        lecture1 = lectureRepository.save(lecture1);

        content1 = new CourseContent();
        content1.setModule(module);
        content1.setTitle("Lecture 1 - Consensus");
        content1.setContentType("VIDEO");
        content1.setFilePath("https://example.com/v1.mp4");
        content1.setPosition(1);
        content1.setDurationSeconds(1200);
        content1.setLegacyLectureId(lecture1.getId());
        content1 = courseContentRepository.save(content1);

        // Lecture 2 & Content 2
        lecture2 = new Lecture();
        lecture2.setTitle("Lecture 2 - Raft");
        lecture2.setCourse(course);
        lecture2.setNumber(2);
        lecture2.setDuration(1800);
        lecture2.setStatus(Lecture.Status.PUBLISHED);
        lecture2 = lectureRepository.save(lecture2);

        content2 = new CourseContent();
        content2.setModule(module);
        content2.setTitle("Lecture 2 - Raft");
        content2.setContentType("VIDEO");
        content2.setFilePath("https://example.com/v2.mp4");
        content2.setPosition(2);
        content2.setDurationSeconds(1800);
        content2.setLegacyLectureId(lecture2.getId());
        content2 = courseContentRepository.save(content2);

        // Enroll student
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUserId(student.getUserId());
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
    }

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("Progress: progressPercent at 90.00 sets completedAt")
    void shouldSetCompletedAtWhenProgressReachesNinetyPercent() throws Exception {
        String payload = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 90.00,
                    "secondsWatched": 1080
                }
                """, content1.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(90.00))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        UserProgress saved = userProgressRepository
                .findByUserIdAndContent_ContentId(student.getUserId(), content1.getContentId())
                .orElseThrow();
        assertThat(saved.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Progress: progressPercent at 89.99 does not set completedAt")
    void shouldNotSetCompletedAtWhenProgressIsUnderNinetyPercent() throws Exception {
        String payload = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 89.99,
                    "secondsWatched": 1079
                }
                """, content1.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(89.99))
                .andExpect(jsonPath("$.completedAt").isEmpty());

        UserProgress saved = userProgressRepository
                .findByUserIdAndContent_ContentId(student.getUserId(), content1.getContentId())
                .orElseThrow();
        assertThat(saved.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("Progress: regressing below 90 % resets completedAt to null")
    void shouldUncompleteAndClearCompletedAtWhenProgressRegressesBelowNinetyPercent() throws Exception {
        // Step 1: Reach 95 %
        String forwardPayload = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 95.00,
                    "secondsWatched": 1140
                }
                """, content1.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(forwardPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        UserProgress savedInitial = userProgressRepository
                .findByUserIdAndContent_ContentId(student.getUserId(), content1.getContentId())
                .orElseThrow();
        assertThat(savedInitial.getCompletedAt()).isNotNull();

        // Step 2: Regress to 85 %
        String regressedPayload = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 85.00,
                    "secondsWatched": 1020
                }
                """, content1.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regressedPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(85.00))
                .andExpect(jsonPath("$.completedAt").isEmpty());

        UserProgress savedRegressed = userProgressRepository
                .findByUserIdAndContent_ContentId(student.getUserId(), content1.getContentId())
                .orElseThrow();
        assertThat(savedRegressed.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("Progress: advancing further past 90 % preserves original completedAt timestamp")
    void shouldPreserveCompletedAtTimestampWhenProgressAdvancesPastNinetyPercent() throws Exception {
        String p1 = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 90.00,
                    "secondsWatched": 1080
                }
                """, content1.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(p1))
                .andExpect(status().isOk());

        UserProgress first = userProgressRepository
                .findByUserIdAndContent_ContentId(student.getUserId(), content1.getContentId())
                .orElseThrow();
        LocalDateTime firstCompletedAt = first.getCompletedAt();
        assertThat(firstCompletedAt).isNotNull();

        // Subsequent write at 100%
        String p2 = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 100.00,
                    "secondsWatched": 1200
                }
                """, content1.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(p2))
                .andExpect(status().isOk());

        UserProgress second = userProgressRepository
                .findByUserIdAndContent_ContentId(student.getUserId(), content1.getContentId())
                .orElseThrow();
        assertThat(second.getCompletedAt()).isEqualTo(firstCompletedAt);
    }

    @Test
    @DisplayName("Student endpoint: POST /api/student/lectures/{id}/complete sets 100 % and completedAt")
    void shouldMarkLectureCompleteWithPrivateNoStoreHeader() throws Exception {
        mockMvc.perform(post("/api/student/lectures/" + lecture1.getId() + "/complete")
                        .with(as(student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.progressPercent").value(100.00))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        UserProgress saved = userProgressRepository
                .findByUserIdAndLecture_Id(student.getUserId(), lecture1.getId())
                .orElseThrow();
        assertThat(saved.getCompletedAt()).isNotNull();
        assertThat(saved.getProgressPercent()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(saved.getContent()).isNotNull();
        assertThat(saved.getContent().getContentId()).isEqualTo(content1.getContentId());
    }

    @Test
    @DisplayName("Student endpoint: POST /api/student/lectures/{id}/uncomplete clears progress and completedAt")
    void shouldUnmarkLectureCompleteWithPrivateNoStoreHeader() throws Exception {
        // First complete it
        mockMvc.perform(post("/api/student/lectures/" + lecture1.getId() + "/complete")
                        .with(as(student.getEmail())))
                .andExpect(status().isOk());

        // Then uncomplete it
        mockMvc.perform(post("/api/student/lectures/" + lecture1.getId() + "/uncomplete")
                        .with(as(student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.progressPercent").value(0.00))
                .andExpect(jsonPath("$.completedAt").isEmpty());

        UserProgress saved = userProgressRepository
                .findByUserIdAndLecture_Id(student.getUserId(), lecture1.getId())
                .orElseThrow();
        assertThat(saved.getCompletedAt()).isNull();
        assertThat(saved.getProgressPercent()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(saved.getSecondsWatched()).isEqualTo(0);
    }

    @Test
    @DisplayName("Course progress: counts lectures at or above 90 % as completed")
    void shouldCountLecturesAtOrAboveNinetyPercentAsCompletedInCourseDetail() throws Exception {
        // Lecture 1 at 91.00% -> should count as completed
        String p1 = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 91.00,
                    "secondsWatched": 1092
                }
                """, content1.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(p1))
                .andExpect(status().isOk());

        // Lecture 2 at 80.00% -> should not count as completed
        String p2 = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 80.00,
                    "secondsWatched": 1440
                }
                """, content2.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(p2))
                .andExpect(status().isOk());

        // Check Course Detail: 2 total lectures, 1 completed
        mockMvc.perform(get("/api/student/courses/" + course.getId())
                        .with(as(student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedLecturesCount").value(1))
                .andExpect(jsonPath("$.completed").value(false));

        // Advance Lecture 2 to 90.00%
        String p2Complete = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 90.00,
                    "secondsWatched": 1620
                }
                """, content2.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(p2Complete))
                .andExpect(status().isOk());

        // Now both lectures are >= 90 %, course is completed
        mockMvc.perform(get("/api/student/courses/" + course.getId())
                        .with(as(student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedLecturesCount").value(2))
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    @DisplayName("Student progress: GET /api/student/lectures/{id}/progress returns Cache-Control: private, no-store")
    void shouldReturnPrivateNoStoreOnLectureProgress() throws Exception {
        mockMvc.perform(get("/api/student/lectures/" + lecture1.getId() + "/progress")
                        .with(as(student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"));
    }
}
