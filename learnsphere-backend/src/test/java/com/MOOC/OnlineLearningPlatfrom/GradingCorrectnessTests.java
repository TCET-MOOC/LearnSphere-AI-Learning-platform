package com.MOOC.OnlineLearningPlatfrom;

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

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W4-05 Integration tests: Grading correctness (AC-16, AC-17, [D-F3]).
 * - Auto-grade MCQ and TF.
 * - TEXT answers remain null until manual grading (no auto-full-marks).
 * - Attempt score calculated strictly from stored answers (never client-supplied).
 * - Instructor manual grading via PATCH /api/answers/{id}/grade recalculates attempt score.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GradingCorrectnessTests extends PostgresIntegrationTest {

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

    private UserAccount teacherUser;
    private UserAccount studentUser;
    private UserAccount foreignUser;
    private Course course;
    private com.MOOC.OnlineLearningPlatfrom.Entity.Test test;
    private Question mcqQuestion;
    private Question tfQuestion;
    private Question textQuestion;

    @BeforeEach
    void setUp() {
        teacherUser = userAccountRepository.findByEmail("teacher@learnsphere.com");
        studentUser = userAccountRepository.findByEmail("student@learnsphere.com");
        foreignUser = userAccountRepository.findByEmail("admin@learnsphere.com");

        course = new Course();
        course.setTitle("Grading Verification Course");
        course.setDescription("Testing assessment grading rules");
        course.setTeacher(teacherUser);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUserId(studentUser.getUserId());
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        test = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test.setTitle("Comprehensive Exam");
        test.setCourse(course);
        test.setDurationMinutes(60);
        test = testRepository.save(test);

        // MCQ: 5 marks, correct "B"
        mcqQuestion = new Question();
        mcqQuestion.setTest(test);
        mcqQuestion.setBody("Select option B");
        mcqQuestion.setQuestionType("MCQ");
        mcqQuestion.setMarks(5);
        mcqQuestion.setOptions(List.of("A", "B", "C"));
        mcqQuestion.setCorrectAnswer("B");
        mcqQuestion = questionRepository.save(mcqQuestion);

        // TF: 2 marks, correct "True"
        tfQuestion = new Question();
        tfQuestion.setTest(test);
        tfQuestion.setBody("Is Java strongly typed?");
        tfQuestion.setQuestionType("TF");
        tfQuestion.setMarks(2);
        tfQuestion.setOptions(List.of("True", "False"));
        tfQuestion.setCorrectAnswer("True");
        tfQuestion = questionRepository.save(tfQuestion);

        // TEXT: 10 marks, manual grading required
        textQuestion = new Question();
        textQuestion.setTest(test);
        textQuestion.setBody("Explain ACID properties in relational databases.");
        textQuestion.setQuestionType("TEXT");
        textQuestion.setMarks(10);
        textQuestion.setOptions(List.of());
        textQuestion.setCorrectAnswer("Atomicity, Consistency, Isolation, Durability");
        textQuestion = questionRepository.save(textQuestion);

        entityManager.flush();
    }

    private RequestPostProcessor studentAuth() {
        return user(userDetailsService.loadUserByUsername(studentUser.getEmail()));
    }

    private RequestPostProcessor teacherAuth() {
        return user(userDetailsService.loadUserByUsername(teacherUser.getEmail()));
    }

    @Test
    @DisplayName("Auto-grade MCQ and TF questions accurately upon finalize/submit")
    void shouldAutoGradeMcqAndTfCorrectly() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt = testAttemptRepository.save(attempt);

        // MCQ: correct answer "B"
        StudentAnswer mcqAnswer = new StudentAnswer();
        mcqAnswer.setAttempt(attempt);
        mcqAnswer.setQuestion(mcqQuestion);
        mcqAnswer.setAnswerText("B");
        studentAnswerRepository.save(mcqAnswer);

        // TF: incorrect answer "False"
        StudentAnswer tfAnswer = new StudentAnswer();
        tfAnswer.setAttempt(attempt);
        tfAnswer.setQuestion(tfQuestion);
        tfAnswer.setAnswerText("False");
        studentAnswerRepository.save(tfAnswer);

        entityManager.flush();

        // Finalize
        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/finalize", attempt.getAttemptId())
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(5))) // 5 (MCQ) + 0 (TF)
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        StudentAnswer evaluatedMcq = studentAnswerRepository.findById(mcqAnswer.getStudentAnswerId()).orElseThrow();
        assertThat(evaluatedMcq.getMarksAwarded()).isEqualTo(5);

        StudentAnswer evaluatedTf = studentAnswerRepository.findById(tfAnswer.getStudentAnswerId()).orElseThrow();
        assertThat(evaluatedTf.getMarksAwarded()).isEqualTo(0);
    }

    @Test
    @DisplayName("TEXT questions remain ungraded (null marksAwarded) upon finalize; no auto-full-marks ([D-F3])")
    void shouldLeaveTextQuestionsUngradedOnFinalize() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt = testAttemptRepository.save(attempt);

        // MCQ: correct answer "B" (5 marks)
        StudentAnswer mcqAnswer = new StudentAnswer();
        mcqAnswer.setAttempt(attempt);
        mcqAnswer.setQuestion(mcqQuestion);
        mcqAnswer.setAnswerText("B");
        studentAnswerRepository.save(mcqAnswer);

        // TEXT: answered, but must NOT be auto-awarded full 10 marks!
        StudentAnswer textAnswer = new StudentAnswer();
        textAnswer.setAttempt(attempt);
        textAnswer.setQuestion(textQuestion);
        textAnswer.setAnswerText("ACID stands for Atomicity, Consistency, Isolation, and Durability.");
        studentAnswerRepository.save(textAnswer);

        entityManager.flush();

        // Finalize
        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/finalize", attempt.getAttemptId())
                        .with(studentAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score", is(5))) // ONLY MCQ 5 marks, TEXT is NOT awarded!
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        StudentAnswer evaluatedText = studentAnswerRepository.findById(textAnswer.getStudentAnswerId()).orElseThrow();
        assertThat(evaluatedText.getMarksAwarded())
                .as("TEXT answer marksAwarded must remain null until instructor manual grade")
                .isNull();

        TestAttempt evaluatedAttempt = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow();
        assertThat(evaluatedAttempt.getScore()).isEqualTo(5);
    }

    @Test
    @DisplayName("Teacher can manually grade a TEXT answer via PATCH /api/answers/{id}/grade and recalculate attempt score")
    void shouldAllowTeacherToManuallyGradeTextAnswerAndRecalculateAttemptScore() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("COMPLETED");
        attempt.setScore(5); // MCQ was 5
        attempt.setAttemptedAt(LocalDateTime.now().minusMinutes(10));
        attempt = testAttemptRepository.save(attempt);

        StudentAnswer mcqAnswer = new StudentAnswer();
        mcqAnswer.setAttempt(attempt);
        mcqAnswer.setQuestion(mcqQuestion);
        mcqAnswer.setAnswerText("B");
        mcqAnswer.setMarksAwarded(5);
        studentAnswerRepository.save(mcqAnswer);

        StudentAnswer textAnswer = new StudentAnswer();
        textAnswer.setAttempt(attempt);
        textAnswer.setQuestion(textQuestion);
        textAnswer.setAnswerText("Comprehensive explanation of database transactions.");
        textAnswer.setMarksAwarded(null);
        textAnswer = studentAnswerRepository.save(textAnswer);

        entityManager.flush();

        // Teacher grades TEXT answer: awards 8 out of 10
        String gradePayload = """
                {
                    "marksAwarded": 8
                }
                """;

        mockMvc.perform(patch("/api/answers/{id}/grade", textAnswer.getStudentAnswerId())
                        .with(teacherAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marksAwarded", is(8)));

        // Verify StudentAnswer marksAwarded updated
        StudentAnswer gradedText = studentAnswerRepository.findById(textAnswer.getStudentAnswerId()).orElseThrow();
        assertThat(gradedText.getMarksAwarded()).isEqualTo(8);

        // Verify parent TestAttempt score was recalculated: 5 (MCQ) + 8 (TEXT) = 13
        TestAttempt updatedAttempt = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow();
        assertThat(updatedAttempt.getScore())
                .as("Attempt score must be recalculated to sum of all marksAwarded")
                .isEqualTo(13);
    }

    @Test
    @DisplayName("Reject manual grading when marksAwarded exceeds question max marks or is negative")
    void shouldRejectInvalidManualGrade() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("COMPLETED");
        attempt = testAttemptRepository.save(attempt);

        StudentAnswer textAnswer = new StudentAnswer();
        textAnswer.setAttempt(attempt);
        textAnswer.setQuestion(textQuestion); // max marks = 10
        textAnswer.setAnswerText("Short response");
        textAnswer = studentAnswerRepository.save(textAnswer);
        entityManager.flush();

        // Exceeds max marks: 15 > 10
        mockMvc.perform(patch("/api/answers/{id}/grade", textAnswer.getStudentAnswerId())
                        .with(teacherAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marksAwarded\": 15}"))
                .andExpect(status().isBadRequest());

        // Negative marks: -2 < 0
        mockMvc.perform(patch("/api/answers/{id}/grade", textAnswer.getStudentAnswerId())
                        .with(teacherAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marksAwarded\": -2}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Reject manual grading by student (403/404)")
    void shouldRejectManualGradingByUnauthorizedStudent() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("COMPLETED");
        attempt = testAttemptRepository.save(attempt);

        StudentAnswer textAnswer = new StudentAnswer();
        textAnswer.setAttempt(attempt);
        textAnswer.setQuestion(textQuestion);
        textAnswer.setAnswerText("Self evaluation attempt");
        textAnswer = studentAnswerRepository.save(textAnswer);
        entityManager.flush();

        mockMvc.perform(patch("/api/answers/{id}/grade", textAnswer.getStudentAnswerId())
                        .with(studentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marksAwarded\": 10}"))
                .andExpect(status().isNotFound()); // Stealth IDOR 404 for foreign student
    }
}

