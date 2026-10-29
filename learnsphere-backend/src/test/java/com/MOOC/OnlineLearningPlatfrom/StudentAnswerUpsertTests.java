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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W4-04 Integration tests: Unique (attempt_id, question_id), answer upsert,
 * and rejection of answers after finalize / timeout (AC-16, [D-F4]).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StudentAnswerUpsertTests extends PostgresIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private UserAccount studentUser;
    private UserAccount teacherUser;
    private Course liveCourse;
    private com.MOOC.OnlineLearningPlatfrom.Entity.Test assessmentTest;
    private Question mcqQuestion;
    private Question tfQuestion;

    @BeforeEach
    void setUp() {
        teacherUser = userAccountRepository.findByEmail("teacher@learnsphere.com");
        studentUser = userAccountRepository.findByEmail("student@learnsphere.com");

        liveCourse = new Course();
        liveCourse.setTitle("W404 Course Integrity");
        liveCourse.setDescription("Course for testing W4-04 answer upsert & constraints");
        liveCourse.setTeacher(teacherUser);
        liveCourse.setStatus(Course.Status.LIVE);
        liveCourse = courseRepository.save(liveCourse);

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(liveCourse);
        enrollment.setUserId(studentUser.getUserId());
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        assessmentTest = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        assessmentTest.setTitle("Integrity Quiz");
        assessmentTest.setCourse(liveCourse);
        assessmentTest.setDurationMinutes(30);
        assessmentTest = testRepository.save(assessmentTest);

        mcqQuestion = new Question();
        mcqQuestion.setTest(assessmentTest);
        mcqQuestion.setBody("What is 2 + 2?");
        mcqQuestion.setQuestionType("MCQ");
        mcqQuestion.setMarks(5);
        mcqQuestion.setOptions(List.of("3", "4", "5"));
        mcqQuestion.setCorrectAnswer("4");
        mcqQuestion = questionRepository.save(mcqQuestion);

        tfQuestion = new Question();
        tfQuestion.setTest(assessmentTest);
        tfQuestion.setBody("The Earth is round.");
        tfQuestion.setQuestionType("TF");
        tfQuestion.setMarks(2);
        tfQuestion.setOptions(List.of("True", "False"));
        tfQuestion.setCorrectAnswer("True");
        tfQuestion = questionRepository.save(tfQuestion);

        entityManager.flush();
    }

    private RequestPostProcessor studentAuth() {
        return user(userDetailsService.loadUserByUsername(studentUser.getEmail()));
    }

    @Test
    @DisplayName("DB enforces unique constraint uq_student_answers_attempt_question on (attempt_id, question_id)")
    void shouldEnforceDatabaseUniqueConstraintOnAttemptAndQuestion() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.table_constraints " +
                "WHERE table_name = 'student_answers' AND constraint_name = 'uq_student_answers_attempt_question'",
                Integer.class
        );
        assertThat(count).as("uq_student_answers_attempt_question must exist in public.student_answers").isEqualTo(1);

        TestAttempt attempt = new TestAttempt();
        attempt.setTest(assessmentTest);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt = testAttemptRepository.save(attempt);
        entityManager.flush();

        StudentAnswer answer1 = new StudentAnswer();
        answer1.setAttempt(attempt);
        answer1.setQuestion(mcqQuestion);
        answer1.setAnswerText("4");
        studentAnswerRepository.save(answer1);
        entityManager.flush();

        StudentAnswer answer2 = new StudentAnswer();
        answer2.setAttempt(attempt);
        answer2.setQuestion(mcqQuestion);
        answer2.setAnswerText("3");

        assertThatThrownBy(() -> {
            studentAnswerRepository.save(answer2);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Submitting an answer for the same question twice in an active attempt updates (upserts) the existing record")
    void shouldUpsertAnswerForSameQuestionInActiveAttempt() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(assessmentTest);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt = testAttemptRepository.save(attempt);

        // First submission: answer "3"
        String payload1 = """
                {
                    "questionId": %d,
                    "answerText": "3"
                }
                """.formatted(mcqQuestion.getQuestionId());

        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/answers", attempt.getAttemptId())
                        .with(studentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId", is(mcqQuestion.getQuestionId().intValue())))
                .andExpect(jsonPath("$.answerText", is("3")))
                .andExpect(jsonPath("$.saved", is(true)));

        List<StudentAnswer> answersAfterFirst = studentAnswerRepository.findByAttempt_AttemptId(attempt.getAttemptId());
        assertThat(answersAfterFirst).hasSize(1);
        Long answerId = answersAfterFirst.get(0).getStudentAnswerId();
        assertThat(answersAfterFirst.get(0).getAnswerText()).isEqualTo("3");

        // Second submission for SAME question: answer changed to "4"
        String payload2 = """
                {
                    "questionId": %d,
                    "answerText": "4"
                }
                """.formatted(mcqQuestion.getQuestionId());

        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/answers", attempt.getAttemptId())
                        .with(studentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentAnswerId", is(answerId.intValue())))
                .andExpect(jsonPath("$.questionId", is(mcqQuestion.getQuestionId().intValue())))
                .andExpect(jsonPath("$.answerText", is("4")))
                .andExpect(jsonPath("$.saved", is(true)));

        List<StudentAnswer> answersAfterSecond = studentAnswerRepository.findByAttempt_AttemptId(attempt.getAttemptId());
        assertThat(answersAfterSecond).hasSize(1);
        assertThat(answersAfterSecond.get(0).getStudentAnswerId()).isEqualTo(answerId);
        assertThat(answersAfterSecond.get(0).getAnswerText()).isEqualTo("4");
    }

    @Test
    @DisplayName("Reject answer submission when attempt is already COMPLETED (finalized)")
    void shouldRejectAnswerSubmissionWhenAttemptIsCompleted() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(assessmentTest);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("COMPLETED");
        attempt.setScore(5);
        attempt.setAttemptedAt(LocalDateTime.now().minusMinutes(10));
        attempt = testAttemptRepository.save(attempt);

        String payload = """
                {
                    "questionId": %d,
                    "answerText": "4"
                }
                """.formatted(mcqQuestion.getQuestionId());

        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/answers", attempt.getAttemptId())
                        .with(studentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("finalized")));
    }

    @Test
    @DisplayName("Reject answer submission when attempt is ABANDONED")
    void shouldRejectAnswerSubmissionWhenAttemptIsAbandoned() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(assessmentTest);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("ABANDONED");
        attempt.setAttemptedAt(LocalDateTime.now().minusMinutes(40));
        attempt = testAttemptRepository.save(attempt);

        String payload = """
                {
                    "questionId": %d,
                    "answerText": "4"
                }
                """.formatted(mcqQuestion.getQuestionId());

        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/answers", attempt.getAttemptId())
                        .with(studentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("finalized")));
    }

    @Test
    @DisplayName("Reject answer submission and mark attempt ABANDONED when attempt has timed out")
    void shouldRejectAnswerSubmissionWhenAttemptHasTimedOut() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(assessmentTest); // durationMinutes = 30
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setAttemptedAt(LocalDateTime.now().minusMinutes(35)); // 35 mins ago > 30 mins
        attempt = testAttemptRepository.save(attempt);

        String payload = """
                {
                    "questionId": %d,
                    "answerText": "4"
                }
                """.formatted(mcqQuestion.getQuestionId());

        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/answers", attempt.getAttemptId())
                        .with(studentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("timed out")));

        TestAttempt updatedAttempt = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow();
        assertThat(updatedAttempt.getStatus()).isEqualTo("ABANDONED");
    }

    @Test
    @DisplayName("Reject answer submission when question does not belong to the attempted test")
    void shouldRejectAnswerSubmissionWhenQuestionBelongsToDifferentTest() throws Exception {
        com.MOOC.OnlineLearningPlatfrom.Entity.Test otherTest = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        otherTest.setTitle("Different Test");
        otherTest.setCourse(liveCourse);
        otherTest = testRepository.save(otherTest);

        Question otherQuestion = new Question();
        otherQuestion.setTest(otherTest);
        otherQuestion.setBody("Foreign question?");
        otherQuestion.setQuestionType("MCQ");
        otherQuestion.setMarks(2);
        otherQuestion.setOptions(List.of("Yes", "No"));
        otherQuestion.setCorrectAnswer("Yes");
        otherQuestion = questionRepository.save(otherQuestion);

        TestAttempt attempt = new TestAttempt();
        attempt.setTest(assessmentTest);
        attempt.setUserId(studentUser.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt = testAttemptRepository.save(attempt);

        String payload = """
                {
                    "questionId": %d,
                    "answerText": "Yes"
                }
                """.formatted(otherQuestion.getQuestionId());

        mockMvc.perform(post("/api/assessments/attempts/{attemptId}/answers", attempt.getAttemptId())
                        .with(studentAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("does not belong to this test")));
    }
}
