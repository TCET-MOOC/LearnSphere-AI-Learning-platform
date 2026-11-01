package com.MOOC.OnlineLearningPlatfrom.livesession;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Post-W6 audit fix (W5-10, api-catalogue "List only; no playback descriptor"): students see
 * the stream only through the playback descriptor of a LIVE session's detail. The list never
 * carries it, and the raw source fields are never sent to students. Course staff keep them.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LiveSessionSourcePrivacyTests extends PostgresIntegrationTest {

    private static final String STREAM = "https://stream.example.test/live/abc/master.m3u8";

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private CourseRepository courseRepository;
    @Autowired private LiveSessionRepository liveSessionRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private UserDetailsService userDetailsService;

    private LiveSession session;

    @BeforeEach
    void setUp() {
        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        UserAccount student = userAccountRepository.findByEmail("student@learnsphere.com");

        Course course = new Course();
        course.setTitle("Live Privacy Course");
        course.setDescription("Stream URLs stay inside the descriptor");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        Enrollment e = new Enrollment();
        e.setUserId(student.getUserId());
        e.setCourse(course);
        e.setRole("STUDENT");
        e.setEnrolledAt(LocalDateTime.now().minusDays(1));
        enrollmentRepository.save(e);

        session = new LiveSession();
        session.setCourse(course);
        session.setTeacher(teacher);
        session.setTitle("Live revision");
        session.setScheduledAt(LocalDateTime.now().plusHours(1));
        session.setStatus(LiveSession.Status.SCHEDULED);
        session.setSourceType(LiveSession.SourceType.HLS);
        session.setSourceUrl(STREAM);
        session = liveSessionRepository.save(session);
        entityManager.flush();
    }

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    private void goLive() {
        session.setStatus(LiveSession.Status.LIVE);
        session.setStartedAt(LocalDateTime.now());
        liveSessionRepository.save(session);
        entityManager.flush();
    }

    @Test
    @DisplayName("The student list never carries the stream, even while LIVE")
    void studentListHasNoStream() throws Exception {
        goLive();
        String body = mockMvc.perform(get("/api/student/live-sessions").with(as("student@learnsphere.com")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).contains("Live revision").doesNotContain(STREAM);
    }

    @Test
    @DisplayName("A scheduled session's detail does not reveal the stream to students")
    void scheduledDetailHasNoStream() throws Exception {
        String body = mockMvc.perform(get("/api/student/live-sessions/{id}", session.getId()).with(as("student@learnsphere.com")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(STREAM);
    }

    @Test
    @DisplayName("A LIVE session's detail gives students the stream only inside the playback descriptor")
    void liveDetailOnlyInDescriptor() throws Exception {
        goLive();
        mockMvc.perform(get("/api/student/live-sessions/{id}", session.getId()).with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playbackDescriptor.playbackUrl").value(STREAM))
                .andExpect(jsonPath("$.sourceUrl").doesNotExist())
                .andExpect(jsonPath("$.providerVideoId").doesNotExist());
    }

    @Test
    @DisplayName("The teacher still sees the configured source")
    void teacherKeepsTheSource() throws Exception {
        String body = mockMvc.perform(get("/api/teacher/live-sessions").with(as("teacher@learnsphere.com")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body).contains(STREAM);
    }
}
