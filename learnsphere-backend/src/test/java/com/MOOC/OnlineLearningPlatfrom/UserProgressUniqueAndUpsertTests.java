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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W3-03 integration tests:
 * - Unique (user_id, content_id) on user_progress (DB constraint & Flyway migration)
 * - Progress upsert semantics: update existing record if present, insert if absent
 * - BigDecimal(5,2) precision for progress_percent
 * - Clamp progress_percent to [0.00, 100.00]
 * - Cache-Control: private, no-store headers on progress endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserProgressUniqueAndUpsertTests extends PostgresIntegrationTest {

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
    private UserProgressRepository userProgressRepository;

    private UserAccount student;
    private UserAccount teacher;
    private Course course;
    private CourseContent content;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");

        Department department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Engineering W303");
            return departmentRepository.save(d);
        });

        course = new Course();
        course.setTitle("Cloud Microservices");
        course.setDescription("Microservices architecture course");
        course.setStatus(Course.Status.LIVE);
        course.setTeacher(teacher);
        course.setDepartment(department);
        course.setPrice(BigDecimal.valueOf(149.99));
        course = courseRepository.save(course);

        Module module = new Module();
        module.setCourse(course);
        module.setTitle("Module 1: Service Discovery");
        module.setPosition(1);
        module = moduleRepository.save(module);

        content = new CourseContent();
        content.setModule(module);
        content.setTitle("Eureka Service Registry");
        content.setContentType("VIDEO");
        content.setDurationSeconds(1200);
        content.setPosition(1);
        content = courseContentRepository.save(content);

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);
    }

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("Progress: Insert new progress with BigDecimal precision (e.g. 45.75)")
    void shouldInsertNewProgressWithBigDecimalPrecision() throws Exception {
        String payload = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 45.75,
                    "secondsWatched": 549
                }
                """, content.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.progressPercent").value(45.75))
                .andExpect(jsonPath("$.secondsWatched").value(549));

        UserProgress saved = userProgressRepository
                .findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId())
                .orElseThrow();
        assertThat(saved.getProgressPercent()).isEqualByComparingTo(new BigDecimal("45.75"));
        assertThat(saved.getSecondsWatched()).isEqualTo(549);
    }

    @Test
    @DisplayName("Progress: Upsert semantics updates existing record instead of inserting duplicate")
    void shouldUpsertExistingProgressInsteadOfCreatingDuplicate() throws Exception {
        // Initial log: 30.00%
        String payload1 = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 30.00,
                    "secondsWatched": 360
                }
                """, content.getContentId());

        String response1 = mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(30.0))
                .andReturn().getResponse().getContentAsString();

        UserProgress firstRecord = userProgressRepository
                .findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId())
                .orElseThrow();
        Long firstProgressId = firstRecord.getProgressId();

        // Second log: 75.50%
        String payload2 = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 75.50,
                    "secondsWatched": 906
                }
                """, content.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressId").value(firstProgressId))
                .andExpect(jsonPath("$.progressPercent").value(75.5))
                .andExpect(jsonPath("$.secondsWatched").value(906));

        // Verify only 1 progress record exists for this user and content
        long count = userProgressRepository.findByUserId(student.getUserId()).stream()
                .filter(p -> p.getContent() != null && content.getContentId().equals(p.getContent().getContentId()))
                .count();
        assertThat(count).isEqualTo(1);

        UserProgress updated = userProgressRepository.findById(firstProgressId).orElseThrow();
        assertThat(updated.getProgressPercent()).isEqualByComparingTo(new BigDecimal("75.50"));
        assertThat(updated.getSecondsWatched()).isEqualTo(906);
    }

    @Test
    @DisplayName("Progress: Clamp progress_percent below 0 to 0.00")
    void shouldClampProgressPercentBelowZeroToZero() throws Exception {
        String payload = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": -25.50,
                    "secondsWatched": 0
                }
                """, content.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(0.0));

        UserProgress saved = userProgressRepository
                .findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId())
                .orElseThrow();
        assertThat(saved.getProgressPercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Progress: Clamp progress_percent above 100 to 100.00")
    void shouldClampProgressPercentAbove100To100() throws Exception {
        String payload = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 142.85,
                    "secondsWatched": 1200
                }
                """, content.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(100.0));

        UserProgress saved = userProgressRepository
                .findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId())
                .orElseThrow();
        assertThat(saved.getProgressPercent()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Progress: Setting progressPercent to 100 sets completedAt")
    void shouldSetCompletedAtWhenProgressReaches100() throws Exception {
        String payload = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 100.00,
                    "secondsWatched": 1200
                }
                """, content.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        UserProgress saved = userProgressRepository
                .findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId())
                .orElseThrow();
        assertThat(saved.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Progress: Cache-Control: private, no-store on read endpoints")
    void shouldReturnPrivateNoStoreHeadersOnReadEndpoints() throws Exception {
        // First log some progress
        String payload = String.format("""
                {
                    "content": { "contentId": %d },
                    "progressPercent": 50.00,
                    "secondsWatched": 600
                }
                """, content.getContentId());

        mockMvc.perform(post("/api/progress")
                        .with(as(student.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // GET /api/progress/me
        mockMvc.perform(get("/api/progress/me")
                        .with(as(student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"));

        // GET /api/progress/content/{contentId}
        mockMvc.perform(get("/api/progress/content/{contentId}", content.getContentId())
                        .with(as(student.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"));
    }

    @Test
    @DisplayName("Progress: Database unique constraint on (user_id, content_id) prevents duplicates")
    void shouldEnforceDatabaseUniqueConstraintOnUserAndContent() {
        UserProgress p1 = new UserProgress();
        p1.setUserId(student.getUserId());
        p1.setContent(content);
        p1.setProgressPercent(new BigDecimal("50.00"));
        userProgressRepository.saveAndFlush(p1);

        UserProgress p2 = new UserProgress();
        p2.setUserId(student.getUserId());
        p2.setContent(content);
        p2.setProgressPercent(new BigDecimal("80.00"));

        assertThatThrownBy(() -> userProgressRepository.saveAndFlush(p2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
