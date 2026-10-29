package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Dto.AssessmentDtos.AttemptReviewDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W4-08 Integration tests: Numeric score/marks migration to BigDecimal(8,2) (A-10, [D-F9]).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NumericScoreMarksMigrationTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @Autowired
    private EntityManager entityManager;

    private UserAccount studentUser;
    private UserAccount teacherUser;
    private Course course;
    private com.MOOC.OnlineLearningPlatfrom.Entity.Test test;

    @BeforeEach
    void setUp() {
        studentUser = userAccountRepository.findByEmail("student@learnsphere.com");
        teacherUser = userAccountRepository.findByEmail("teacher@learnsphere.com");

        course = new Course();
        course.setTitle("Numeric Migration Test Course " + System.currentTimeMillis());
        course.setDescription("Testing BigDecimal(8,2) scores and marks");
        course.setTeacher(teacherUser);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(studentUser.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        test = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test.setCourse(course);
        test.setTitle("Fractional Scoring Test");
        test.setDurationMinutes(30);
        test = testRepository.save(test);

        entityManager.flush();
    }

    private RequestPostProcessor studentAuth() {
        return user(userDetailsService.loadUserByUsername(studentUser.getEmail()));
    }

    private RequestPostProcessor teacherAuth() {
        return user(userDetailsService.loadUserByUsername(teacherUser.getEmail()));
    }

    @Test
    @DisplayName("Should persist and retrieve fractional marks (BigDecimal) for questions")
    void shouldPersistAndRetrieveFractionalMarksForQuestion() {
        Question q = new Question();
        q.setTest(test);
        q.setQuestionType("MCQ");
        q.setBody("Fractional question 1");
        q.setOptions(List.of("A", "B"));
        q.setCorrectAnswer("A");
        q.setMarks(new BigDecimal("2.50"));
        q = questionRepository.save(q);
        entityManager.flush();
        entityManager.clear();

        Question loaded = questionRepository.findById(q.getQuestionId()).orElseThrow();
        assertThat(loaded.getMarks()).isNotNull();
        assertThat(loaded.getMarks()).isEqualByComparingTo(new BigDecimal("2.50"));
    }

    @Test
    @DisplayName("Should auto-grade fractional marks for MCQ and compute BigDecimal attempt score")
    void shouldAutoGradeFractionalMarksForMcq() throws Exception {
        Question q1 = new Question();
        q1.setTest(test);
        q1.setQuestionType("MCQ");
        q1.setBody("Q1");
        q1.setOptions(List.of("A", "B"));
        q1.setCorrectAnswer("A");
        q1.setMarks(new BigDecimal("2.25"));
        questionRepository.save(q1);

        Question q2 = new Question();
        q2.setTest(test);
        q2.setQuestionType("MCQ");
        q2.setBody("Q2");
        q2.setOptions(List.of("X", "Y"));
        q2.setCorrectAnswer("Y");
        q2.setMarks(new BigDecimal("3.50"));
        questionRepository.save(q2);
        entityManager.flush();

        // Student starts attempt
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
        attempt = testAttemptRepository.save(attempt);

        // Submit answers: both correct
        StudentAnswer ans1 = new StudentAnswer();
        ans1.setAttempt(attempt);
        ans1.setQuestion(q1);
        ans1.setAnswerText("A");
        studentAnswerRepository.save(ans1);

        StudentAnswer ans2 = new StudentAnswer();
        ans2.setAttempt(attempt);
        ans2.setQuestion(q2);
        ans2.setAnswerText("Y");
        studentAnswerRepository.save(ans2);
        entityManager.flush();

        // Finalize
        mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/finalize")
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.score").value(5.75));

        TestAttempt refreshed = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow();
        assertThat(refreshed.getScore()).isEqualByComparingTo(new BigDecimal("5.75"));
    }

    @Test
    @DisplayName("Should allow instructor to manually grade with fractional marks (BigDecimal)")
    void shouldAllowTeacherToManuallyGradeWithFractionalMarks() throws Exception {
        Question essay = new Question();
        essay.setTest(test);
        essay.setQuestionType("TEXT");
        essay.setBody("Write an essay");
        essay.setMarks(new BigDecimal("10.00"));
        essay = questionRepository.save(essay);

        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("COMPLETED");
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(10));
        attempt.setScore(BigDecimal.ZERO);
        attempt = testAttemptRepository.save(attempt);

        StudentAnswer answer = new StudentAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(essay);
        answer.setAnswerText("An insightful essay");
        answer = studentAnswerRepository.save(answer);
        entityManager.flush();

        // Teacher grades 7.50 out of 10.00
        mockMvc.perform(patch("/api/answers/" + answer.getStudentAnswerId() + "/grade")
                        .with(teacherAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marksAwarded\": 7.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marksAwarded").value(7.5));

        TestAttempt refreshed = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow();
        assertThat(refreshed.getScore()).isEqualByComparingTo(new BigDecimal("7.50"));
    }
}

