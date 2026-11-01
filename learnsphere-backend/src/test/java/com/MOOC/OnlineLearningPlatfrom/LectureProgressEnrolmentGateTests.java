package com.MOOC.OnlineLearningPlatfrom;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Post-W6 audit fix (W3-03 / W4-11 regression): lecture completion and progress endpoints
 * must be enrolment-gated like their siblings. A student who is not enrolled gets a stealth
 * 404 and no progress row is written (IDOR rule, PLAN §6.4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class LectureProgressEnrolmentGateTests extends PostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private CourseRepository courseRepository;
    @Autowired private LectureRepository lectureRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private UserProgressRepository userProgressRepository;
    @Autowired private UserDetailsService userDetailsService;

    private UserAccount enrolled;
    private UserAccount outsider;
    private Lecture lecture;

    @BeforeEach
    void setUp() {
        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        enrolled = userAccountRepository.findByEmail("student@learnsphere.com");
        outsider = userAccountRepository.findByEmail("external@learnsphere.com");

        Course course = new Course();
        course.setTitle("Gated Progress Course");
        course.setDescription("Completion must be enrolment-gated");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        lecture = new Lecture();
        lecture.setCourse(course);
        lecture.setTitle("Gated lecture");
        lecture.setNumber(1);
        lecture.setDuration(600);
        lecture = lectureRepository.save(lecture);

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(enrolled.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now().minusDays(1));
        enrollmentRepository.save(enrollment);
        entityManager.flush();
    }

    private RequestPostProcessor auth(UserAccount u) {
        return user(userDetailsService.loadUserByUsername(u.getEmail()));
    }

    @Test
    @DisplayName("An unenrolled student cannot mark a lecture complete (404, no progress row)")
    void outsiderCannotComplete() throws Exception {
        mockMvc.perform(post("/api/student/lectures/{id}/complete", lecture.getId()).with(auth(outsider)))
                .andExpect(status().isNotFound());
        assertThat(userProgressRepository.findByUserIdAndLecture_Id(outsider.getUserId(), lecture.getId())).isEmpty();
    }

    @Test
    @DisplayName("An unenrolled student cannot reset a lecture's progress (404, no progress row)")
    void outsiderCannotUncomplete() throws Exception {
        mockMvc.perform(post("/api/student/lectures/{id}/uncomplete", lecture.getId()).with(auth(outsider)))
                .andExpect(status().isNotFound());
        assertThat(userProgressRepository.findByUserIdAndLecture_Id(outsider.getUserId(), lecture.getId())).isEmpty();
    }

    @Test
    @DisplayName("An unenrolled student cannot read a lecture's progress (404)")
    void outsiderCannotReadProgress() throws Exception {
        mockMvc.perform(get("/api/student/lectures/{id}/progress", lecture.getId()).with(auth(outsider)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("An enrolled student can still read and reset their own progress")
    void enrolledStudentStillWorks() throws Exception {
        mockMvc.perform(get("/api/student/lectures/{id}/progress", lecture.getId()).with(auth(enrolled)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/student/lectures/{id}/uncomplete", lecture.getId()).with(auth(enrolled)))
                .andExpect(status().isOk());
    }
}
