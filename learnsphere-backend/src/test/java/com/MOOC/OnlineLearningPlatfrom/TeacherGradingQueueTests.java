package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.StudentAnswer;
import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.QuestionRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.StudentAnswerRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestAttemptRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W7-16 (SCR-35; W4-05, AC-17): the teacher assessments screen reads its tests and the TEXT
 * answers waiting for a grade from teacher-scoped DTO endpoints, grades through
 * {@code PATCH /api/answers/{id}/grade}, and never sees another teacher's work or a correct answer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TeacherGradingQueueTests extends PostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private CustomUserDetailsService userDetailsService;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private TestAttemptRepository testAttemptRepository;
    @Autowired private StudentAnswerRepository studentAnswerRepository;

    private UserAccount student;
    private com.MOOC.OnlineLearningPlatfrom.Entity.Test test;
    private StudentAnswer pendingText;
    private StudentAnswer inProgressText;
    private TestAttempt completedAttempt;

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @BeforeEach
    void setUp() {
        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");

        Course course = new Course();
        course.setTitle("Grading Course W7-16");
        course.setDescription("Fixture");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        test = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test.setTitle("Essay check");
        test.setCourse(course);
        test.setDurationMinutes(30);
        test = testRepository.save(test);

        Question mcq = new Question();
        mcq.setTest(test);
        mcq.setBody("Pick B");
        mcq.setQuestionType("MCQ");
        mcq.setMarks(new BigDecimal("5.00"));
        mcq.setOptions(List.of("A", "B"));
        mcq.setCorrectAnswer("B");
        mcq = questionRepository.save(mcq);

        Question text = new Question();
        text.setTest(test);
        text.setBody("Explain eventual consistency.");
        text.setQuestionType("TEXT");
        text.setMarks(new BigDecimal("10.00"));
        text.setCorrectAnswer("Replicas converge over time.");
        text = questionRepository.save(text);

        completedAttempt = new TestAttempt();
        completedAttempt.setTest(test);
        completedAttempt.setUserId(student.getUserId());
        completedAttempt.setStatus("COMPLETED");
        completedAttempt.setStartedAt(LocalDateTime.of(2026, 11, 1, 10, 0));
        completedAttempt.setCompletedAt(LocalDateTime.of(2026, 11, 1, 10, 20));
        completedAttempt.setScore(new BigDecimal("5.00"));
        completedAttempt = testAttemptRepository.save(completedAttempt);

        StudentAnswer mcqAnswer = new StudentAnswer();
        mcqAnswer.setAttempt(completedAttempt);
        mcqAnswer.setQuestion(mcq);
        mcqAnswer.setAnswerText("B");
        mcqAnswer.setMarksAwarded(new BigDecimal("5.00"));
        studentAnswerRepository.save(mcqAnswer);

        pendingText = new StudentAnswer();
        pendingText.setAttempt(completedAttempt);
        pendingText.setQuestion(text);
        pendingText.setAnswerText("They agree eventually.");
        pendingText = studentAnswerRepository.save(pendingText);

        TestAttempt running = new TestAttempt();
        running.setTest(test);
        running.setUserId(userAccountRepository.findByEmail("external@learnsphere.com").getUserId());
        running.setStatus("IN_PROGRESS");
        running.setStartedAt(LocalDateTime.of(2026, 11, 2, 10, 0));
        running = testAttemptRepository.save(running);

        inProgressText = new StudentAnswer();
        inProgressText.setAttempt(running);
        inProgressText.setQuestion(text);
        inProgressText.setAnswerText("draft");
        inProgressText = studentAnswerRepository.save(inProgressText);

        entityManager.flush();
    }

    @Test
    void testsListShowsQuestionAndPendingCountsForOwnCoursesOnly() throws Exception {
        mockMvc.perform(get("/api/teacher/assessments").with(as("teacher@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(containsString("no-store"), containsString("private"))))
                .andExpect(jsonPath("$[?(@.testId == " + test.getTestId() + ")].title").value(hasItem("Essay check")))
                .andExpect(jsonPath("$[?(@.testId == " + test.getTestId() + ")].courseTitle").value(hasItem("Grading Course W7-16")))
                .andExpect(jsonPath("$[?(@.testId == " + test.getTestId() + ")].questionCount").value(hasItem(2)))
                .andExpect(jsonPath("$[?(@.testId == " + test.getTestId() + ")].pendingCount").value(hasItem(1)))
                .andExpect(jsonPath("$..correctAnswer").doesNotExist());

        mockMvc.perform(get("/api/teacher/assessments").with(as("pending@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].testId", not(hasItem(test.getTestId().intValue()))));

        mockMvc.perform(get("/api/teacher/assessments").with(as("student@learnsphere.com")))
                .andExpect(status().isForbidden());
    }

    @Test
    void pendingListsOnlyUngradedTextAnswersOfFinishedAttemptsWithoutTheKey() throws Exception {
        mockMvc.perform(get("/api/teacher/assessments/pending").with(as("teacher@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", allOf(containsString("no-store"), containsString("private"))))
                .andExpect(jsonPath("$[?(@.testId == " + test.getTestId() + ")]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.answerId == " + pendingText.getStudentAnswerId() + ")].studentName").value(hasItem(student.getFullName())))
                .andExpect(jsonPath("$[?(@.answerId == " + pendingText.getStudentAnswerId() + ")].questionText").value(hasItem("Explain eventual consistency.")))
                .andExpect(jsonPath("$[?(@.answerId == " + pendingText.getStudentAnswerId() + ")].answerText").value(hasItem("They agree eventually.")))
                .andExpect(jsonPath("$[?(@.answerId == " + pendingText.getStudentAnswerId() + ")].maxMarks").value(hasItem(10.0)))
                .andExpect(jsonPath("$[*].answerId", not(hasItem(inProgressText.getStudentAnswerId().intValue()))))
                .andExpect(jsonPath("$..correctAnswer").doesNotExist())
                .andExpect(content().string(not(containsString("Replicas converge"))));

        mockMvc.perform(get("/api/teacher/assessments/pending").param("testId", test.getTestId().toString())
                        .with(as("pending@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void gradingRemovesTheAnswerFromTheQueueAndRecomputesTheAttempt() throws Exception {
        mockMvc.perform(patch("/api/answers/{id}/grade", pendingText.getStudentAnswerId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marksAwarded\": 11}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/answers/{id}/grade", pendingText.getStudentAnswerId())
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marksAwarded\": 7}"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(patch("/api/answers/{id}/grade", pendingText.getStudentAnswerId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marksAwarded\": 7.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marksAwarded").value(7.5))
                .andExpect(jsonPath("$..correctAnswer").doesNotExist());

        entityManager.flush();
        entityManager.clear();
        assertThat(testAttemptRepository.findById(completedAttempt.getAttemptId()).orElseThrow().getScore())
                .isEqualByComparingTo("12.50");

        mockMvc.perform(get("/api/teacher/assessments/pending").with(as("teacher@learnsphere.com")))
                .andExpect(jsonPath("$[*].answerId", not(hasItem(pendingText.getStudentAnswerId().intValue()))));
        mockMvc.perform(get("/api/teacher/assessments").with(as("teacher@learnsphere.com")))
                .andExpect(jsonPath("$[?(@.testId == " + test.getTestId() + ")].pendingCount").value(hasItem(0)));
    }
}
