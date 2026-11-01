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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Post-W6 audit fix (AC-12, BR-10, BR-25 "the player cannot complete content client-side"):
 * watch progress for content with a known length is derived on the server from seconds
 * watched, never taken from the client's percentage, and a video lecture can only be marked
 * complete once the server has recorded at least 90 % of it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ServerDerivedProgressTests extends PostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private CourseContentRepository courseContentRepository;
    @Autowired private LectureRepository lectureRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private UserProgressRepository userProgressRepository;
    @Autowired private UserDetailsService userDetailsService;

    private UserAccount student;
    private Lecture lecture;
    private CourseContent content;
    private Course course;

    @BeforeEach
    void setUp() {
        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");

        course = new Course();
        course.setTitle("Server Progress Course");
        course.setDescription("Progress is derived on the server");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        Module module = new Module();
        module.setCourse(course);
        module.setTitle("Module 1");
        module.setPosition(1);
        module = moduleRepository.save(module);

        lecture = new Lecture();
        lecture.setCourse(course);
        lecture.setTitle("A 20-minute video");
        lecture.setNumber(1);
        lecture.setDuration(1200);
        lecture = lectureRepository.save(lecture);

        content = new CourseContent();
        content.setModule(module);
        content.setTitle("A 20-minute video");
        content.setContentType("VIDEO");
        content.setPosition(1);
        content.setDurationSeconds(1200);
        content.setLegacyLectureId(lecture.getId());
        content.setSourceType("UPLOAD");
        content = courseContentRepository.save(content);

        Enrollment e = new Enrollment();
        e.setUserId(student.getUserId());
        e.setCourse(course);
        e.setRole("STUDENT");
        e.setEnrolledAt(LocalDateTime.now().minusDays(1));
        enrollmentRepository.save(e);
        entityManager.flush();
    }

    private RequestPostProcessor me() {
        return user(userDetailsService.loadUserByUsername(student.getEmail()));
    }

    private String progress(double percent, int seconds) {
        return String.format("{\"content\":{\"contentId\":%d},\"progressPercent\":%s,\"secondsWatched\":%d}",
                content.getContentId(), percent, seconds);
    }

    @Test
    @DisplayName("A client claiming 100 % after 60 s of a 20-minute video gets 5 %, not completion")
    void clientPercentIsIgnoredWhenTheLengthIsKnown() throws Exception {
        mockMvc.perform(post("/api/progress").with(me()).contentType(MediaType.APPLICATION_JSON).content(progress(100, 60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(5.00))
                .andExpect(jsonPath("$.completedAt").doesNotExist())
                // the response must not drag in the lecture's course and teacher (e-mail, royalty balance)
                .andExpect(jsonPath("$.lecture").doesNotExist())
                .andExpect(jsonPath("$.lectureId").value(lecture.getId()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("royaltyBalance"))));
    }

    @Test
    @DisplayName("Seconds watched beyond the length are clamped to 100 %")
    void secondsBeyondLengthClampTo100() throws Exception {
        mockMvc.perform(post("/api/progress").with(me()).contentType(MediaType.APPLICATION_JSON).content(progress(0, 5000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(100.00))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Marking an unwatched video lecture complete is refused (400) and records nothing")
    void completeWithoutWatchingIsRefused() throws Exception {
        mockMvc.perform(post("/api/student/lectures/{id}/complete", lecture.getId()).with(me()))
                .andExpect(status().isBadRequest());
        userProgressRepository.findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId())
                .ifPresent(p -> assertThat(p.getCompletedAt()).isNull());
    }

    @Test
    @DisplayName("After 90 % is recorded on the server, marking the lecture complete succeeds")
    void completeAfterNinetyPercentSucceeds() throws Exception {
        mockMvc.perform(post("/api/progress").with(me()).contentType(MediaType.APPLICATION_JSON).content(progress(90, 1080)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/student/lectures/{id}/complete", lecture.getId()).with(me()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    @DisplayName("A lecture with no known length can still be marked complete manually")
    void lectureWithoutLengthCanBeMarkedComplete() throws Exception {
        Lecture reading = new Lecture();
        reading.setCourse(course);
        reading.setTitle("A reading");
        reading.setNumber(2);
        reading = lectureRepository.save(reading);
        entityManager.flush();
        mockMvc.perform(post("/api/student/lectures/{id}/complete", reading.getId()).with(me()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(100.00));
        assertThat(userProgressRepository.findByUserIdAndLecture_Id(student.getUserId(), reading.getId()).orElseThrow()
                .getProgressPercent()).isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
