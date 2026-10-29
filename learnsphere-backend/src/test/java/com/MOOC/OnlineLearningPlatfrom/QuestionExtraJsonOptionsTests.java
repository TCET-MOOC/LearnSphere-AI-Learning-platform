package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.QuestionRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W4-03 Integration tests: Question.extra JSON options storage, legacy question_options table retirement,
 * and correctAnswer validation (A-12, [D-F8]).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuestionExtraJsonOptionsTests extends PostgresIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private Long teacherId;
    private Long studentId;
    private Long courseId;
    private Long testId;

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @BeforeEach
    void setUp() {
        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        teacherId = teacher.getUserId();

        UserAccount student = userAccountRepository.findByEmail("student@learnsphere.com");
        studentId = student.getUserId();

        Course course = new Course();
        course.setTitle("Database Engineering & Internals");
        course.setDescription("LSM-trees, B-trees and storage engines");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);
        courseId = course.getId();

        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setUserId(studentId);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        com.MOOC.OnlineLearningPlatfrom.Entity.Test test = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test.setCourse(course);
        test.setTitle("Storage Engines Quiz");
        test.setDurationMinutes(30);
        test = testRepository.save(test);
        testId = test.getTestId();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("W4-03: question_options collection table is dropped; extra column holds JSON options array")
    void shouldDropQuestionOptionsTableAndStoreOptionsInExtra() {
        // Assert question_options table does not exist in information_schema
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'question_options'",
                Integer.class
        );
        assertThat(tableCount).isEqualTo(0);

        // Save a question with options
        Question q = new Question();
        q.setTest(testRepository.findById(testId).orElseThrow());
        q.setBody("Which tree structure minimizes disk random I/O for append-heavy workloads?");
        q.setQuestionType("MCQ");
        q.setMarks(10);
        q.setOptions(List.of("LSM-tree", "B+ Tree", "Red-Black Tree", "AVL Tree"));
        q.setCorrectAnswer("LSM-tree");
        q = questionRepository.save(q);

        entityManager.flush();
        entityManager.clear();

        // Verify extra column in DB directly
        String dbExtra = jdbcTemplate.queryForObject(
                "SELECT extra FROM public.questions WHERE question_id = ?",
                String.class,
                q.getQuestionId()
        );
        assertThat(dbExtra).isNotNull();
        assertThat(dbExtra).contains("LSM-tree").contains("B+ Tree");

        // Verify entity reload
        Question reloaded = questionRepository.findById(q.getQuestionId()).orElseThrow();
        assertThat(reloaded.getOptions()).containsExactly("LSM-tree", "B+ Tree", "Red-Black Tree", "AVL Tree");
    }

    @Test
    @DisplayName("W4-03: Student assessment detail GET /api/assessments/{id} returns options from extra with correctAnswer hidden")
    void shouldReturnOptionsFromExtraInStudentDto() throws Exception {
        Question q = new Question();
        q.setTest(testRepository.findById(testId).orElseThrow());
        q.setBody("What is the Write Amplification Factor of an append-only WAL?");
        q.setQuestionType("MCQ");
        q.setMarks(5);
        q.setOptions(List.of("1.0", "2.0", "O(log N)", "O(N)"));
        q.setCorrectAnswer("1.0");
        questionRepository.save(q);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/api/assessments/{id}", testId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", hasSize(1)))
                .andExpect(jsonPath("$.questions[0].options", hasSize(4)))
                .andExpect(jsonPath("$.questions[0].options[0]").value("1.0"))
                .andExpect(jsonPath("$.questions[0].options[1]").value("2.0"))
                .andExpect(jsonPath("$.questions[0].correctAnswer").doesNotExist());
    }

    @Test
    @DisplayName("W4-03: Teacher can create MCQ question when correctAnswer matches one of the options")
    void shouldAllowCreatingMcqWhenCorrectAnswerMatchesOption() throws Exception {
        String payload = """
                {
                    "body": "What component in RocksDB buffers writes in RAM before SSTable flush?",
                    "questionType": "MCQ",
                    "marks": 10,
                    "options": ["MemTable", "Block Cache", "Write Ahead Log", "Bloom Filter"],
                    "correctAnswer": "MemTable"
                }
                """;

        mockMvc.perform(post("/api/teacher/tests/{testId}/questions", testId)
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionId").isNumber())
                .andExpect(jsonPath("$.options", hasSize(4)))
                .andExpect(jsonPath("$.correctAnswer").value("MemTable"));

        entityManager.flush();
        entityManager.clear();

        // Verify stored extra in DB
        String extra = jdbcTemplate.queryForObject(
                "SELECT extra FROM public.questions WHERE body LIKE 'What component in RocksDB%'",
                String.class
        );
        assertThat(extra).contains("MemTable").contains("Block Cache");
    }

    @Test
    @DisplayName("W4-03: Reject MCQ question creation when correctAnswer does NOT match any option (400 Bad Request)")
    void shouldRejectMcqWhenCorrectAnswerDoesNotMatchOptions() throws Exception {
        String payload = """
                {
                    "body": "Which compaction strategy is best for space amplification?",
                    "questionType": "MCQ",
                    "marks": 10,
                    "options": ["Leveled Compaction", "Universal Compaction", "FIFO Compaction"],
                    "correctAnswer": "NonExistentStrategy"
                }
                """;

        mockMvc.perform(post("/api/teacher/tests/{testId}/questions", testId)
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail", containsString("correctAnswer must match one of the options")));
    }

    @Test
    @DisplayName("W4-03: Reject MCQ question creation when options array is empty (400 Bad Request)")
    void shouldRejectMcqWhenOptionsEmpty() throws Exception {
        String payload = """
                {
                    "body": "Is fsync synchronous or asynchronous?",
                    "questionType": "MCQ",
                    "marks": 5,
                    "options": [],
                    "correctAnswer": "Synchronous"
                }
                """;

        mockMvc.perform(post("/api/teacher/tests/{testId}/questions", testId)
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail", containsString("options in extra")));
    }

    @Test
    @DisplayName("W4-03: Reject MCQ question update when updated correctAnswer does NOT match options (400 Bad Request)")
    void shouldRejectMcqUpdateWhenCorrectAnswerDoesNotMatchOptions() throws Exception {
        Question q = new Question();
        q.setTest(testRepository.findById(testId).orElseThrow());
        q.setBody("Initial question body");
        q.setQuestionType("MCQ");
        q.setMarks(5);
        q.setOptions(List.of("Option 1", "Option 2"));
        q.setCorrectAnswer("Option 1");
        q = questionRepository.save(q);

        entityManager.flush();
        entityManager.clear();

        String updatePayload = """
                {
                    "correctAnswer": "InvalidOption"
                }
                """;

        mockMvc.perform(put("/api/teacher/tests/{testId}/questions/{questionId}", testId, q.getQuestionId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail", containsString("correctAnswer must match one of the options")));
    }
}

