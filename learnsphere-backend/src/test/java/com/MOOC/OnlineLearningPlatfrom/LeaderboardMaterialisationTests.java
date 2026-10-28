package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W3-07 integration tests:
 * - Materialise leaderboard after attempt completion
 * - Recalculate avg_score, global_rank, dept_rank after TestAttempt finalization
 * - GET /api/courses/{courseId}/leaderboard returns materialised rows with Cache-Control: private, no-store
 * - GET /api/leaderboard/me returns student's course rank rows with Cache-Control: private, no-store
 * - XP leaderboard (GET /api/leaderboard) is kept separate from course assessment leaderboard
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LeaderboardMaterialisationTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @Autowired
    private LeaderboardRepository leaderboardRepository;

    @Autowired
    private TestService testService;

    private UserAccount student1;
    private UserAccount student2;
    private UserAccount student3;
    private UserAccount teacher;
    private Department deptA;
    private Department deptB;
    private Course course;
    private com.MOOC.OnlineLearningPlatfrom.Entity.Test test;
    private Question q1;
    private Question q2;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student1 = userAccountRepository.findByEmail("student@learnsphere.com");
        student2 = userAccountRepository.findByEmail("external@learnsphere.com");

        deptA = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Computer Science W307");
            return departmentRepository.save(d);
        });

        deptB = new Department();
        deptB.setName("Electrical Engineering W307");
        deptB = departmentRepository.save(deptB);

        // Assign departments
        student1.setDepartment(deptA);
        userAccountRepository.save(student1);

        student2.setDepartment(deptA);
        userAccountRepository.save(student2);

        // Create student 3 in deptB
        student3 = userAccountRepository.findByEmail("pending@learnsphere.com");
        student3.setDepartment(deptB);
        userAccountRepository.save(student3);

        course = new Course();
        course.setTitle("Distributed Database Internals");
        course.setDescription("Storage engines, transaction processing, and replication");
        course.setDepartment(deptA);
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        test = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test.setCourse(course);
        test.setTitle("Midterm Assessment");
        test.setDurationMinutes(45);
        test = testRepository.save(test);

        q1 = new Question();
        q1.setTest(test);
        q1.setBody("Explain 2PC phase 1");
        q1.setQuestionType("MCQ");
        q1.setMarks(50);
        q1.setCorrectAnswer("Prepare");
        q1 = questionRepository.save(q1);

        q2 = new Question();
        q2.setTest(test);
        q2.setBody("Explain 2PC phase 2");
        q2.setQuestionType("MCQ");
        q2.setMarks(50);
        q2.setCorrectAnswer("Commit");
        q2 = questionRepository.save(q2);
    }

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("Materialisation: Leaderboard is populated and ranked when student finalizes attempt")
    void shouldMaterialiseLeaderboardAfterTestAttemptCompletion() throws Exception {
        // Student 1 starts and completes test with 100/100 (100%)
        TestAttempt a1 = testService.startTestAttempt(test.getTestId(), student1.getUserId());
        StudentAnswer sa1 = new StudentAnswer();
        sa1.setAnswerText("Prepare");
        testService.submitAnswer(a1.getAttemptId(), q1.getQuestionId(), sa1);
        StudentAnswer sa2 = new StudentAnswer();
        sa2.setAnswerText("Commit");
        testService.submitAnswer(a1.getAttemptId(), q2.getQuestionId(), sa2);

        // Finalize student 1
        mockMvc.perform(post("/api/attempts/" + a1.getAttemptId() + "/finalize")
                        .with(as(student1.getEmail())))
                .andExpect(status().isOk());

        // Student 2 starts and completes test with 50/100 (50%)
        TestAttempt a2 = testService.startTestAttempt(test.getTestId(), student2.getUserId());
        StudentAnswer sa3 = new StudentAnswer();
        sa3.setAnswerText("Prepare");
        testService.submitAnswer(a2.getAttemptId(), q1.getQuestionId(), sa3);
        StudentAnswer sa4 = new StudentAnswer();
        sa4.setAnswerText("Wrong Answer");
        testService.submitAnswer(a2.getAttemptId(), q2.getQuestionId(), sa4);

        // Finalize student 2
        mockMvc.perform(post("/api/attempts/" + a2.getAttemptId() + "/finalize")
                        .with(as(student2.getEmail())))
                .andExpect(status().isOk());

        // Query course leaderboard
        mockMvc.perform(get("/api/courses/" + course.getId() + "/leaderboard")
                        .with(as(student1.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(student1.getUserId()))
                .andExpect(jsonPath("$[0].avgScore").value(100.0))
                .andExpect(jsonPath("$[0].globalRank").value(1))
                .andExpect(jsonPath("$[0].deptRank").value(1))
                .andExpect(jsonPath("$[1].userId").value(student2.getUserId()))
                .andExpect(jsonPath("$[1].avgScore").value(50.0))
                .andExpect(jsonPath("$[1].globalRank").value(2))
                .andExpect(jsonPath("$[1].deptRank").value(2));
    }

    @Test
    @DisplayName("Department Ranking: Students in different departments receive distinct dept_rank")
    void shouldCalculateDepartmentRanksCorrectly() throws Exception {
        // Student 1 (Dept A): 100%
        TestAttempt a1 = testService.startTestAttempt(test.getTestId(), student1.getUserId());
        StudentAnswer s1a1 = new StudentAnswer();
        s1a1.setAnswerText("Prepare");
        testService.submitAnswer(a1.getAttemptId(), q1.getQuestionId(), s1a1);
        StudentAnswer s1a2 = new StudentAnswer();
        s1a2.setAnswerText("Commit");
        testService.submitAnswer(a1.getAttemptId(), q2.getQuestionId(), s1a2);
        testService.calculateAndFinalizeScore(a1.getAttemptId());

        // Student 2 (Dept A): 50%
        TestAttempt a2 = testService.startTestAttempt(test.getTestId(), student2.getUserId());
        StudentAnswer s2a1 = new StudentAnswer();
        s2a1.setAnswerText("Prepare");
        testService.submitAnswer(a2.getAttemptId(), q1.getQuestionId(), s2a1);
        testService.calculateAndFinalizeScore(a2.getAttemptId());

        // Student 3 (Dept B): 50%
        TestAttempt a3 = testService.startTestAttempt(test.getTestId(), student3.getUserId());
        StudentAnswer s3a1 = new StudentAnswer();
        s3a1.setAnswerText("Prepare");
        testService.submitAnswer(a3.getAttemptId(), q1.getQuestionId(), s3a1);
        testService.calculateAndFinalizeScore(a3.getAttemptId());

        // Check leaderboard:
        // Student 1: globalRank 1, deptRank 1 (Dept A)
        // Student 2: globalRank 2 or 3, deptRank 2 (Dept A)
        // Student 3: globalRank 2 or 3, deptRank 1 (Dept B - top of their department!)
        Leaderboard lb3 = leaderboardRepository.findByUser_UserIdAndCourse_Id(student3.getUserId(), course.getId())
                .orElseThrow();
        assertThat(lb3.getDeptRank()).isEqualTo(1);
        assertThat(lb3.getAvgScore()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
    }

    @Test
    @DisplayName("My Leaderboard: GET /api/leaderboard/me returns student's course leaderboard ranks")
    void shouldReturnMyLeaderboardEntries() throws Exception {
        TestAttempt a1 = testService.startTestAttempt(test.getTestId(), student1.getUserId());
        StudentAnswer s1a1 = new StudentAnswer();
        s1a1.setAnswerText("Prepare");
        testService.submitAnswer(a1.getAttemptId(), q1.getQuestionId(), s1a1);
        testService.calculateAndFinalizeScore(a1.getAttemptId());

        mockMvc.perform(get("/api/leaderboard/me")
                        .with(as(student1.getEmail())))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseId").value(course.getId()))
                .andExpect(jsonPath("$[0].avgScore").value(50.0))
                .andExpect(jsonPath("$[0].globalRank").value(1));
    }

    @Test
    @DisplayName("Separation: Global XP leaderboard remains independent of course assessment leaderboard")
    void shouldKeepXPLeaderboardSeparate() throws Exception {
        mockMvc.perform(get("/api/leaderboard?scope=global")
                        .with(as(student1.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
