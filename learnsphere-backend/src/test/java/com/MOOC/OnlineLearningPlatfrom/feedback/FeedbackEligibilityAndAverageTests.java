package com.MOOC.OnlineLearningPlatfrom.feedback;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Story W5-09: Feedback eligibility + average rating cache and invalidation
 * Rules: AC-22, AC-23, NFR-03, [E-F12]
 *
 * Scenarios:
 * 1. Enrolled student can submit feedback (rating 1-5) -> 200 OK + Cache-Control: private, no-store.
 * 2. Unenrolled student submitting feedback returns 404 NOT_FOUND (enrolment gating).
 * 3. Unauthenticated request to submit feedback returns 401 UNAUTHORIZED.
 * 4. Submitting invalid rating (< 1 or > 5) returns 400 BAD_REQUEST.
 * 5. Exactly 1 feedback per user per course: submitting again updates the existing record (upsert).
 * 6. Feedback author can delete feedback -> 204 NO_CONTENT.
 * 7. Admin can delete any feedback -> 204 NO_CONTENT.
 * 8. Non-author student attempting to delete feedback returns 404 NOT_FOUND (IDOR defense).
 * 9. GET /api/courses/{courseId}/feedback/avg returns average rating and is cached (hitCount increments).
 * 10. Submitting or deleting feedback invalidates cached average rating.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FeedbackEligibilityAndAverageTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FeedbackRepository feedbackRepository;

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

    @Autowired
    private PublicCacheService publicCacheService;

    private UserAccount teacher;
    private UserAccount studentEnrolled;
    private UserAccount studentUnenrolled;
    private UserAccount admin;

    private Course course;

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll();
        enrollmentRepository.deleteAll();

        teacher = findOrCreateUser("fb.teacher@learnsphere.com", "Feedback Teacher", "TEACHER");
        studentEnrolled = findOrCreateUser("fb.enrolled@learnsphere.com", "Enrolled Student", "STUDENT");
        studentUnenrolled = findOrCreateUser("fb.unenrolled@learnsphere.com", "Unenrolled Student", "STUDENT");
        admin = findOrCreateUser("fb.admin@learnsphere.com", "Feedback Admin", "ADMIN");

        course = new Course();
        course.setTitle("Distributed Systems 101");
        course.setTeacher(teacher);
        course.setIsPublished(true);
        course = courseRepository.save(course);

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUserId(studentEnrolled.getUserId());
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);

        publicCacheService.invalidateCourseFeedbackAvg(course.getId());
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
    @DisplayName("Scenario 1: Enrolled student can submit feedback -> 200 OK + Cache-Control: private, no-store")
    void enrolledStudentCanSubmitFeedback() throws Exception {
        Map<String, Object> body = Map.of(
                "rating", 5,
                "message", "Excellent course structure!"
        );

        mockMvc.perform(post("/api/courses/{courseId}/feedback", course.getId())
                        .with(asUser(studentEnrolled))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.message").value("Excellent course structure!"))
                .andExpect(jsonPath("$.userId").value(studentEnrolled.getUserId()))
                .andExpect(jsonPath("$.courseId").value(course.getId()));
    }

    @Test
    @DisplayName("Scenario 2: Unenrolled student submitting feedback returns 404 NOT_FOUND (enrolment gating)")
    void unenrolledStudentSubmittingFeedbackReturns404() throws Exception {
        Map<String, Object> body = Map.of(
                "rating", 4,
                "message", "I am not enrolled"
        );

        mockMvc.perform(post("/api/courses/{courseId}/feedback", course.getId())
                        .with(asUser(studentUnenrolled))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 3: Unauthenticated request to submit feedback returns 401 UNAUTHORIZED")
    void unauthenticatedRequestToSubmitFeedbackReturns401() throws Exception {
        Map<String, Object> body = Map.of(
                "rating", 5,
                "message", "Anonymous"
        );

        mockMvc.perform(post("/api/courses/{courseId}/feedback", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Scenario 4: Submitting invalid rating (< 1 or > 5) returns 400 BAD_REQUEST")
    void invalidRatingReturns400() throws Exception {
        Map<String, Object> tooHigh = Map.of("rating", 6, "message", "Way too high");
        mockMvc.perform(post("/api/courses/{courseId}/feedback", course.getId())
                        .with(asUser(studentEnrolled))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooHigh)))
                .andExpect(status().isBadRequest());

        Map<String, Object> tooLow = Map.of("rating", 0, "message", "Zero");
        mockMvc.perform(post("/api/courses/{courseId}/feedback", course.getId())
                        .with(asUser(studentEnrolled))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tooLow)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Scenario 5: Exactly 1 feedback per user per course (submitting again updates the record)")
    void exactlyOneFeedbackPerCourseUpdatesExisting() throws Exception {
        Map<String, Object> initial = Map.of("rating", 3, "message", "Average");
        mockMvc.perform(post("/api/courses/{courseId}/feedback", course.getId())
                        .with(asUser(studentEnrolled))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(3));

        Map<String, Object> updated = Map.of("rating", 5, "message", "Upgraded to 5 stars!");
        mockMvc.perform(post("/api/courses/{courseId}/feedback", course.getId())
                        .with(asUser(studentEnrolled))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.message").value("Upgraded to 5 stars!"));

        assertThat(feedbackRepository.countByCourseId(course.getId())).isEqualTo(1L);
    }

    @Test
    @DisplayName("Scenario 6: Feedback author can delete feedback -> 204 NO_CONTENT")
    void authorCanDeleteFeedback() throws Exception {
        Map<String, Object> body = Map.of("rating", 4, "message", "Good");
        MvcResult res = mockMvc.perform(post("/api/courses/{courseId}/feedback", course.getId())
                        .with(asUser(studentEnrolled))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        Feedback saved = objectMapper.readValue(res.getResponse().getContentAsString(), Feedback.class);

        mockMvc.perform(delete("/api/feedback/{id}", saved.getFeedbackId())
                        .with(asUser(studentEnrolled)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")));

        assertThat(feedbackRepository.findById(saved.getFeedbackId())).isEmpty();
    }

    @Test
    @DisplayName("Scenario 7: Admin can delete any feedback -> 204 NO_CONTENT")
    void adminCanDeleteFeedback() throws Exception {
        Map<String, Object> body = Map.of("rating", 4, "message", "Good");
        MvcResult res = mockMvc.perform(post("/api/courses/{courseId}/feedback", course.getId())
                        .with(asUser(studentEnrolled))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        Feedback saved = objectMapper.readValue(res.getResponse().getContentAsString(), Feedback.class);

        mockMvc.perform(delete("/api/feedback/{id}", saved.getFeedbackId())
                        .with(asUser(admin)))
                .andExpect(status().isNoContent());

        assertThat(feedbackRepository.findById(saved.getFeedbackId())).isEmpty();
    }

    @Test
    @DisplayName("Scenario 8: Non-author student attempting to delete feedback returns 404 NOT_FOUND (IDOR defense)")
    void nonAuthorStudentCannotDeleteFeedback() throws Exception {
        Map<String, Object> body = Map.of("rating", 4, "message", "Good");
        MvcResult res = mockMvc.perform(post("/api/courses/{courseId}/feedback", course.getId())
                        .with(asUser(studentEnrolled))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();

        Feedback saved = objectMapper.readValue(res.getResponse().getContentAsString(), Feedback.class);

        mockMvc.perform(delete("/api/feedback/{id}", saved.getFeedbackId())
                        .with(asUser(studentUnenrolled)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Scenario 9 & 10: GET /api/courses/{courseId}/feedback/avg is cached and invalidated on feedback write")
    void averageRatingIsCachedAndInvalidatedOnWrite() throws Exception {
        // Initially 0 reviews
        mockMvc.perform(get("/api/courses/{courseId}/feedback/avg", course.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(header().string("Cache-Control", containsString("max-age=60")))
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.averageRating").value(0.0))
                .andExpect(jsonPath("$.totalReviews").value(0));

        // Submit rating 4
        mockMvc.perform(post("/api/courses/{courseId}/feedback", course.getId())
                        .with(asUser(studentEnrolled))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("rating", 4))))
                .andExpect(status().isOk());

        // Cache was invalidated on write, fresh query returns 4.0
        mockMvc.perform(get("/api/courses/{courseId}/feedback/avg", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.totalReviews").value(1));

        long hitBefore = publicCacheService.getHitCount();

        // Second call within 60s should hit the cache
        mockMvc.perform(get("/api/courses/{courseId}/feedback/avg", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.0));

        assertThat(publicCacheService.getHitCount()).isGreaterThan(hitBefore);
    }
}
