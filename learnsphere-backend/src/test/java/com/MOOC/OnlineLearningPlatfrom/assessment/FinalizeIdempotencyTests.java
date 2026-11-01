package com.MOOC.OnlineLearningPlatfrom.assessment;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Event.TestAttemptCompletedEvent;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Post-W6 audit fix ("replays must be harmless"): finalising an attempt that is already
 * COMPLETED or ABANDONED returns the stored result. It is not re-graded, a finished
 * attempt is never flipped to ABANDONED after its deadline, and no second completion
 * event is published.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class FinalizeIdempotencyTests extends PostgresIntegrationTest {

    @TestConfiguration
    static class EventCounter {
        static final AtomicInteger COMPLETED_EVENTS = new AtomicInteger();

        @Bean
        Object attemptCompletedCounter() {
            return new Object() {
                @EventListener
                public void on(TestAttemptCompletedEvent e) { COMPLETED_EVENTS.incrementAndGet(); }
            };
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private CourseRepository courseRepository;
    @Autowired private TestRepository testRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private TestAttemptRepository testAttemptRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private UserDetailsService userDetailsService;
    @Autowired private TestService testService;

    private UserAccount student;
    private TestAttempt attempt;
    private Question question;

    @BeforeEach
    void setUp() {
        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");

        Course course = new Course();
        course.setTitle("Finalize Once Course");
        course.setDescription("Replays are harmless");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        Enrollment e = new Enrollment();
        e.setUserId(student.getUserId());
        e.setCourse(course);
        e.setRole("STUDENT");
        e.setEnrolledAt(LocalDateTime.now().minusDays(1));
        enrollmentRepository.save(e);

        com.MOOC.OnlineLearningPlatfrom.Entity.Test quiz = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        quiz.setCourse(course);
        quiz.setTitle("Finalize once");
        quiz.setDurationMinutes(30);
        quiz = testRepository.save(quiz);

        question = new Question();
        question.setTest(quiz);
        question.setBody("2 + 2 = ?");
        question.setQuestionType("MCQ");
        question.setOptions(List.of("3", "4"));
        question.setCorrectAnswer("4");
        question.setMarks(new BigDecimal("1.00"));
        question = questionRepository.save(question);

        attempt = testService.startTestAttempt(quiz.getTestId(), student.getUserId());
        StudentAnswer answer = new StudentAnswer();
        answer.setAnswerText("4");
        testService.submitAnswer(attempt.getAttemptId(), question.getQuestionId(), answer);
        entityManager.flush();
        EventCounter.COMPLETED_EVENTS.set(0);
    }

    private void finalizeOk() throws Exception {
        mockMvc.perform(post("/api/assessments/attempts/{id}/finalize", attempt.getAttemptId())
                        .with(user(userDetailsService.loadUserByUsername(student.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(1.00));
    }

    @Test
    @DisplayName("Finalising twice returns the same stored result and publishes one completion event")
    void finalizeTwiceIsHarmless() throws Exception {
        finalizeOk();
        entityManager.flush();
        entityManager.clear();
        LocalDateTime firstCompletedAt = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow().getCompletedAt();

        finalizeOk();
        entityManager.flush();
        entityManager.clear();

        TestAttempt stored = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow();
        assertThat(stored.getCompletedAt()).isEqualTo(firstCompletedAt);
        assertThat(EventCounter.COMPLETED_EVENTS.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("A completed attempt is not flipped to ABANDONED when finalised again after its deadline")
    void finishedAttemptStaysCompletedAfterDeadline() throws Exception {
        finalizeOk();
        entityManager.flush();
        entityManager.clear();

        TestAttempt stored = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow();
        stored.setStartedAt(LocalDateTime.now().minusHours(2)); // the 30-minute deadline has long passed
        testAttemptRepository.save(stored);
        entityManager.flush();
        entityManager.clear();

        finalizeOk();
        assertThat(testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow().getStatus()).isEqualTo("COMPLETED");
    }
}
