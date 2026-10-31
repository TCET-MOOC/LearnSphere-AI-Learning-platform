package com.MOOC.OnlineLearningPlatfrom.livesession;

import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Story W5-10: Real live media provider + player integration
 * Rules: A-26, BR-27, A-18, W5-07, NFR-03
 *
 * Scenarios:
 * 1. PUT /api/teacher/live-sessions/{id}/source accepts valid YOUTUBE_LIVE source -> 200 OK.
 * 2. PUT /api/teacher/live-sessions/{id}/source accepts valid HTTPS HLS manifest URL -> 200 OK.
 * 3. RTMP or encoder stream keys are rejected with 400 BAD_REQUEST.
 * 4. Start session without a source is blocked -> 400 BAD_REQUEST.
 * 5. Start session with validated source succeeds -> 200 OK, status LIVE, startedAt recorded.
 * 6. Enrolled student GET /api/student/live-sessions/{id} returns descriptor ONLY when LIVE.
 * 7. When SCHEDULED or ENDED, playback descriptor is null/absent.
 * 8. Unenrolled student GET /api/student/live-sessions/{id} returns 404 NOT_FOUND.
 * 9. Teacher ending session sets status ENDED, endedAt recorded, hides playback descriptor.
 * 10. Playback descriptor endpoint enforces Cache-Control: private, no-store.
 * 11. Foreign teacher cannot configure source or start session -> 404 NOT_FOUND.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class LiveMediaProviderAndPlayerTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LiveSessionRepository liveSessionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private UserAccount teacher;
    private UserAccount foreignTeacher;
    private UserAccount studentEnrolled;
    private UserAccount studentUnenrolled;

    private Course course;
    private LiveSession session;

    @BeforeEach
    void setUp() {
        liveSessionRepository.deleteAll();
        enrollmentRepository.deleteAll();

        teacher = findOrCreateUser("media.teacher@learnsphere.com", "Media Teacher", "TEACHER");
        foreignTeacher = findOrCreateUser("media.foreign@learnsphere.com", "Foreign Teacher", "TEACHER");
        studentEnrolled = findOrCreateUser("media.enrolled@learnsphere.com", "Enrolled Student", "STUDENT");
        studentUnenrolled = findOrCreateUser("media.unenrolled@learnsphere.com", "Unenrolled Student", "STUDENT");

        course = new Course();
        course.setTitle("Real-Time Systems");
        course.setTeacher(teacher);
        course.setIsPublished(true);
        course = courseRepository.save(course);

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUserId(studentEnrolled.getUserId());
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);

        session = new LiveSession();
        session.setCourse(course);
        session.setTeacher(teacher);
        session.setTitle("Live Architecture Review");
        session.setScheduledAt(LocalDateTime.now().plusDays(1));
        session.setStatus(LiveSession.Status.SCHEDULED);
        session = liveSessionRepository.save(session);
    }

    private RequestPostProcessor asUser(UserAccount account) {
        var userDetails = userDetailsService.loadUserByUsername(account.getEmail());
        return user(userDetails);
    }

    private UserAccount findOrCreateUser(String email, String name, String roleName) {
        UserAccount existing = userAccountRepository.findByEmail(email);
        if (existing != null) {
            return existing;
        }
        UserAccount u = new UserAccount();
        u.setEmail(email);
        u.setFullName(name);
        u.setPassword("hashedpassword");
        u.setStatus(UserAccount.Status.ACTIVE);
        u = userAccountRepository.save(u);

        Role role = roleRepository.findByName(roleName);
        if (role == null) {
            role = new Role();
            role.setName(roleName);
            role = roleRepository.save(role);
        }

        UserRole ur = new UserRole();
        ur.setUser(u);
        ur.setRole(role);
        userRoleRepository.save(ur);
        return u;
    }

    @Test
    @DisplayName("Scenario 1: Teacher can configure YOUTUBE_LIVE source -> 200 OK")
    void configureYouTubeLiveSource() throws Exception {
        Map<String, Object> body = Map.of(
                "sourceType", "YOUTUBE_LIVE",
                "providerVideoId", "dQw4w9WgXcQ"
        );

        mockMvc.perform(put("/api/teacher/live-sessions/{id}/source", session.getId())
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("YOUTUBE_LIVE"))
                .andExpect(jsonPath("$.providerVideoId").value("dQw4w9WgXcQ"))
                .andExpect(jsonPath("$.playbackMode").value("YOUTUBE"));
    }

    @Test
    @DisplayName("Scenario 2: Teacher can configure HTTPS HLS source -> 200 OK")
    void configureHlsSource() throws Exception {
        Map<String, Object> body = Map.of(
                "sourceType", "HLS",
                "sourceUrl", "https://stream.learnsphere.com/live/stream.m3u8"
        );

        mockMvc.perform(put("/api/teacher/live-sessions/{id}/source", session.getId())
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceType").value("HLS"))
                .andExpect(jsonPath("$.sourceUrl").value("https://stream.learnsphere.com/live/stream.m3u8"))
                .andExpect(jsonPath("$.playbackMode").value("HLS"));
    }

    @Test
    @DisplayName("Scenario 3: RTMP URLs or encoder stream keys rejected -> 400 BAD_REQUEST")
    void rtmpAndStreamKeysRejected() throws Exception {
        Map<String, Object> rtmp = Map.of(
                "sourceType", "HLS",
                "sourceUrl", "rtmp://live.twitch.tv/app/stream_key_secret_123"
        );

        mockMvc.perform(put("/api/teacher/live-sessions/{id}/source", session.getId())
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rtmp)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("RTMP")));

        Map<String, Object> streamKey = Map.of(
                "sourceType", "HLS",
                "sourceUrl", "https://stream.example.com/live?key=secret_encoder_key_456"
        );

        mockMvc.perform(put("/api/teacher/live-sessions/{id}/source", session.getId())
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(streamKey)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Scenario 4: Start session without source is blocked -> 400 BAD_REQUEST")
    void startWithoutSourceBlocked() throws Exception {
        mockMvc.perform(put("/api/teacher/live-sessions/{id}/start", session.getId())
                        .with(asUser(teacher)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("source")));
    }

    @Test
    @DisplayName("Scenario 5: Start session with validated source succeeds -> 200 OK, status LIVE")
    void startWithSourceSucceeds() throws Exception {
        // Set source
        mockMvc.perform(put("/api/teacher/live-sessions/{id}/source", session.getId())
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sourceType", "YOUTUBE_LIVE",
                                "providerVideoId", "jfKfPfyJRdk"
                        ))))
                .andExpect(status().isOk());

        // Start session
        mockMvc.perform(put("/api/teacher/live-sessions/{id}/start", session.getId())
                        .with(asUser(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Scenario 6 & 7: Playback descriptor returned ONLY when LIVE; absent when SCHEDULED or ENDED")
    void playbackDescriptorOnlyReturnedWhenLive() throws Exception {
        // Configure source
        mockMvc.perform(put("/api/teacher/live-sessions/{id}/source", session.getId())
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sourceType", "HLS",
                                "sourceUrl", "https://stream.learnsphere.com/live/master.m3u8"
                        ))))
                .andExpect(status().isOk());

        // 1. When SCHEDULED: student sees session, but playback descriptor is null/absent
        mockMvc.perform(get("/api/student/live-sessions/{id}", session.getId())
                        .with(asUser(studentEnrolled)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andExpect(jsonPath("$.playbackDescriptor").doesNotExist());

        // 2. Start session -> LIVE
        mockMvc.perform(put("/api/teacher/live-sessions/{id}/start", session.getId())
                        .with(asUser(teacher)))
                .andExpect(status().isOk());

        // When LIVE: student receives playback descriptor with Cache-Control: private, no-store
        mockMvc.perform(get("/api/student/live-sessions/{id}", session.getId())
                        .with(asUser(studentEnrolled)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.status").value("LIVE"))
                .andExpect(jsonPath("$.playbackDescriptor").exists())
                .andExpect(jsonPath("$.playbackDescriptor.sourceType").value("HLS"))
                .andExpect(jsonPath("$.playbackDescriptor.playbackUrl").value("https://stream.learnsphere.com/live/master.m3u8"));

        // 3. End session -> ENDED
        mockMvc.perform(put("/api/teacher/live-sessions/{id}/end", session.getId())
                        .with(asUser(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"));

        // When ENDED: playback descriptor is null/absent again
        mockMvc.perform(get("/api/student/live-sessions/{id}", session.getId())
                        .with(asUser(studentEnrolled)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENDED"))
                .andExpect(jsonPath("$.playbackDescriptor").doesNotExist());
    }

    @Test
    @DisplayName("Scenario 8: Unenrolled student cannot get session -> 404 NOT_FOUND")
    void unenrolledStudentGets404() throws Exception {
        mockMvc.perform(get("/api/student/live-sessions/{id}", session.getId())
                        .with(asUser(studentUnenrolled)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 9: Foreign teacher cannot configure source or start session -> 404 NOT_FOUND")
    void foreignTeacherCannotConfigureOrStart() throws Exception {
        mockMvc.perform(put("/api/teacher/live-sessions/{id}/source", session.getId())
                        .with(asUser(foreignTeacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "sourceType", "HLS",
                                "sourceUrl", "https://stream.learnsphere.com/live/master.m3u8"
                        ))))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/teacher/live-sessions/{id}/start", session.getId())
                        .with(asUser(foreignTeacher)))
                .andExpect(status().isNotFound());
    }
}
