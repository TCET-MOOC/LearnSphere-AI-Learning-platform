package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W4-11: Player-Attached Quiz End-to-End & Completion Gate Integration Tests.
 *
 * Rules: A-27, BR-25, BR-10, AC-12, AC-15, AC-18.
 *
 * Scenarios:
 * 1. watch-to-90 without quiz stays incomplete (completedAt is null).
 * 2. pass before 90% stays incomplete (completedAt is null).
 * 3. watch-90 + pass completes once (completedAt stamped).
 * 4. failed retake stays complete after an earlier pass (sticky pass).
 * 5. retake limit enforced (maxAttempts).
 * 6. adding/changing a required quiz does not retroactively revoke.
 * 7. foreign content returns stealth 404.
 * 8. client completion rejected when quiz is not passed.
 * 9. quiz/attempt responses return private, no-store.
 * 10. teacher authoring: bind quiz to CourseContent with placement, required, threshold, maxAttempts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PlayerAttachedQuizCompletionGateTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private CourseContentRepository courseContentRepository;

    @Autowired
    private LectureRepository lectureRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private StudentAnswerRepository studentAnswerRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserProgressRepository userProgressRepository;

    @Autowired
    private UserDetailsService userDetailsService;

    private UserAccount teacher;
    private UserAccount student;
    private UserAccount foreignStudent;
    private Course course;
    private Module module;
    private CourseContent content;
    private Lecture lecture;
    private com.MOOC.OnlineLearningPlatfrom.Entity.Test quiz;
    private Question question1;
    private Question question2;

    @BeforeEach
    public void setup() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        foreignStudent = userAccountRepository.findByEmail("external@learnsphere.com");

        course = new Course();
        course.setTitle("Advanced Systems Engineering");
        course.setDescription("Comprehensive course with video lectures and gated quizzes");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        // Module
        module = new Module();
        module.setCourse(course);
        module.setTitle("Module 1: Kernel Architecture");
        module.setPosition(1);
        module = moduleRepository.save(module);

        // Legacy Lecture
        lecture = new Lecture();
        lecture.setCourse(course);
        lecture.setTitle("Processes and Threads");
        lecture.setNumber(1);
        lecture.setDuration(1000); // 1000 seconds
        lecture = lectureRepository.save(lecture);

        // CourseContent bound to module and legacy lecture
        content = new CourseContent();
        content.setModule(module);
        content.setTitle("Processes and Threads");
        content.setContentType("VIDEO");
        content.setPosition(1);
        content.setDurationSeconds(1000);
        content.setLegacyLectureId(lecture.getId());
        content.setSourceType("UPLOAD");
        content = courseContentRepository.save(content);

        // Enroll legitimate student
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now().minusDays(2));
        enrollmentRepository.save(enrollment);

        // Quiz bound to content with required_for_completion = true, pass_threshold = 60.00, max_attempts = 3
        quiz = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        quiz.setCourse(course);
        quiz.setContent(content);
        quiz.setLecture(lecture);
        quiz.setTitle("Processes Post-Video Quiz");
        quiz.setDurationMinutes(15);
        quiz.setPlacement("POST_VIDEO");
        quiz.setRequiredForCompletion(true);
        quiz.setPassThreshold(new BigDecimal("60.00"));
        quiz.setMaxAttempts(3);
        quiz = testRepository.save(quiz);

        // 2 MCQ Questions, 5 marks each = 10 max marks
        question1 = new Question();
        question1.setTest(quiz);
        question1.setBody("What system call creates a new process in POSIX?");
        question1.setQuestionType("MCQ");
        question1.setOptions(List.of("fork()", "exec()", "pthread_create()", "wait()"));
        question1.setCorrectAnswer("fork()");
        question1.setMarks(new BigDecimal("5.00"));
        questionRepository.save(question1);

        question2 = new Question();
        question2.setTest(quiz);
        question2.setBody("Threads within the same process share which of the following?");
        question2.setQuestionType("MCQ");
        question2.setOptions(List.of("Address Space", "Stack Pointer", "Registers", "Program Counter"));
        question2.setCorrectAnswer("Address Space");
        question2.setMarks(new BigDecimal("5.00"));
        questionRepository.save(question2);

        entityManager.flush();
    }

    private RequestPostProcessor auth(UserAccount user) {
        return user(userDetailsService.loadUserByUsername(user.getEmail()));
    }

    // -------------------------------------------------------------------------
    // 1. watch-to-90 without quiz stays incomplete
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 1: Watching 95% without passing required quiz leaves completedAt null")
    public void testWatch90WithoutQuizStaysIncomplete() throws Exception {
        String progressPayload = objectMapper.writeValueAsString(Map.of(
                "content", Map.of("contentId", content.getContentId()),
                "progressPercent", 95.00,
                "secondsWatched", 950
        ));

        mockMvc.perform(post("/api/progress")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressPayload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.progressPercent").value(95.0))
                .andExpect(jsonPath("$.completedAt").doesNotExist()); // Incomplete!

        UserProgress saved = userProgressRepository.findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId()).orElseThrow();
        assertThat(saved.getCompletedAt()).isNull();
    }

    // -------------------------------------------------------------------------
    // 2. pass before 90% stays incomplete
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 2: Passing quiz with 100% before watching 90% leaves completedAt null")
    public void testPassBefore90PercentStaysIncomplete() throws Exception {
        // Watch 50%
        mockMvc.perform(post("/api/progress")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("contentId", content.getContentId()),
                                "progressPercent", 50.00,
                                "secondsWatched", 500
                        ))))
                .andExpect(status().isOk());

        // Start attempt
        MvcResult startResult = mockMvc.perform(post("/api/assessments/" + quiz.getTestId() + "/start")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andReturn();
        Long attemptId = objectMapper.readTree(startResult.getResponse().getContentAsString()).get("attemptId").asLong();

        // Answer Q1 correctly
        mockMvc.perform(post("/api/assessments/attempts/" + attemptId + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", question1.getQuestionId(),
                                "answerText", "fork()"
                        ))))
                .andExpect(status().isOk());

        // Answer Q2 correctly
        mockMvc.perform(post("/api/assessments/attempts/" + attemptId + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", question2.getQuestionId(),
                                "answerText", "Address Space"
                        ))))
                .andExpect(status().isOk());

        // Finalize attempt: score 10/10 (100% >= 60% threshold -> PASS)
        mockMvc.perform(post("/api/assessments/attempts/" + attemptId + "/finalize")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(10.00));

        // Content progress must still be incomplete because watchPercent is only 50% (< 90%)
        UserProgress saved = userProgressRepository.findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId()).orElseThrow();
        assertThat(saved.getCompletedAt()).isNull();
    }

    // -------------------------------------------------------------------------
    // 3. watch-90 + pass completes once
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 3: Watching 90%+ AND passing quiz stamps completedAt exactly once")
    public void testWatch90AndPassCompletesOnce() throws Exception {
        // 1. Watch 92% first
        mockMvc.perform(post("/api/progress")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("contentId", content.getContentId()),
                                "progressPercent", 92.00,
                                "secondsWatched", 920
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").doesNotExist());

        // 2. Take quiz and pass (Q1 correct = 5/10 = 50%, Q2 correct = 10/10 = 100%)
        MvcResult startResult = mockMvc.perform(post("/api/assessments/" + quiz.getTestId() + "/start")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andReturn();
        Long attemptId = objectMapper.readTree(startResult.getResponse().getContentAsString()).get("attemptId").asLong();

        mockMvc.perform(post("/api/assessments/attempts/" + attemptId + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", question1.getQuestionId(),
                                "answerText", "fork()"
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/assessments/attempts/" + attemptId + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", question2.getQuestionId(),
                                "answerText", "Address Space"
                        ))))
                .andExpect(status().isOk());

        // Finalize triggers content completion
        mockMvc.perform(post("/api/assessments/attempts/" + attemptId + "/finalize")
                        .with(auth(student)))
                .andExpect(status().isOk());

        UserProgress completed = userProgressRepository.findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId()).orElseThrow();
        assertThat(completed.getCompletedAt()).isNotNull();
        LocalDateTime firstCompletedAt = completed.getCompletedAt();

        // Repeated progress update preserves the completedAt timestamp
        mockMvc.perform(post("/api/progress")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("contentId", content.getContentId()),
                                "progressPercent", 98.00,
                                "secondsWatched", 980
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        UserProgress refreshed = userProgressRepository.findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId()).orElseThrow();
        assertThat(refreshed.getCompletedAt()).isEqualTo(firstCompletedAt);
    }

    // -------------------------------------------------------------------------
    // 4. failed retake stays complete after an earlier pass (sticky pass)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 4: Failed retake leaves earlier content completion intact (sticky pass)")
    public void testFailedRetakeStaysCompleteStickyPass() throws Exception {
        // Watch 95%
        mockMvc.perform(post("/api/progress")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("contentId", content.getContentId()),
                                "progressPercent", 95.00,
                                "secondsWatched", 950
                        ))))
                .andExpect(status().isOk());

        // Attempt 1: Pass with 10/10
        MvcResult attempt1Result = mockMvc.perform(post("/api/assessments/" + quiz.getTestId() + "/start")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andReturn();
        Long att1Id = objectMapper.readTree(attempt1Result.getResponse().getContentAsString()).get("attemptId").asLong();

        mockMvc.perform(post("/api/assessments/attempts/" + att1Id + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", question1.getQuestionId(),
                                "answerText", "fork()"
                        ))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/assessments/attempts/" + att1Id + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", question2.getQuestionId(),
                                "answerText", "Address Space"
                        ))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/assessments/attempts/" + att1Id + "/finalize")
                        .with(auth(student)))
                .andExpect(status().isOk());

        // Verify content is complete
        UserProgress completed = userProgressRepository.findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId()).orElseThrow();
        assertThat(completed.getCompletedAt()).isNotNull();
        LocalDateTime originalCompletedAt = completed.getCompletedAt();

        // Attempt 2 (retake): Fail with 0/10 (wrong answers)
        MvcResult attempt2Result = mockMvc.perform(post("/api/assessments/" + quiz.getTestId() + "/start")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andReturn();
        Long att2Id = objectMapper.readTree(attempt2Result.getResponse().getContentAsString()).get("attemptId").asLong();

        mockMvc.perform(post("/api/assessments/attempts/" + att2Id + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", question1.getQuestionId(),
                                "answerText", "wait()"
                        ))))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/assessments/attempts/" + att2Id + "/finalize")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0.00));

        // Sticky pass guarantee: completedAt is STILL preserved!
        UserProgress afterFailRetake = userProgressRepository.findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId()).orElseThrow();
        assertThat(afterFailRetake.getCompletedAt()).isNotNull();
        assertThat(afterFailRetake.getCompletedAt()).isEqualTo(originalCompletedAt);

        // Ongoing progress updates still return completedAt
        mockMvc.perform(get("/api/progress/content/" + content.getContentId())
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    // -------------------------------------------------------------------------
    // 5. retake limit enforced (maxAttempts)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 5: Enforces maxAttempts limit (3) on retakes")
    public void testRetakeLimitEnforcement() throws Exception {
        // Complete attempt 1
        MvcResult r1 = mockMvc.perform(post("/api/assessments/" + quiz.getTestId() + "/start").with(auth(student))).andExpect(status().isOk()).andReturn();
        Long id1 = objectMapper.readTree(r1.getResponse().getContentAsString()).get("attemptId").asLong();
        mockMvc.perform(post("/api/assessments/attempts/" + id1 + "/finalize").with(auth(student))).andExpect(status().isOk());

        // Complete attempt 2
        MvcResult r2 = mockMvc.perform(post("/api/assessments/" + quiz.getTestId() + "/start").with(auth(student))).andExpect(status().isOk()).andReturn();
        Long id2 = objectMapper.readTree(r2.getResponse().getContentAsString()).get("attemptId").asLong();
        mockMvc.perform(post("/api/assessments/attempts/" + id2 + "/finalize").with(auth(student))).andExpect(status().isOk());

        // Complete attempt 3
        MvcResult r3 = mockMvc.perform(post("/api/assessments/" + quiz.getTestId() + "/start").with(auth(student))).andExpect(status().isOk()).andReturn();
        Long id3 = objectMapper.readTree(r3.getResponse().getContentAsString()).get("attemptId").asLong();
        mockMvc.perform(post("/api/assessments/attempts/" + id3 + "/finalize").with(auth(student))).andExpect(status().isOk());

        // 4th attempt: exceeded maxAttempts = 3
        mockMvc.perform(post("/api/assessments/" + quiz.getTestId() + "/start")
                        .with(auth(student)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.detail", containsString("Maximum attempt limit of 3 reached")));
    }

    // -------------------------------------------------------------------------
    // 6. adding/changing a required quiz does not retroactively revoke
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 6: Adding or changing a required quiz does not retroactively revoke completed content")
    public void testAddingRequiredQuizDoesNotRetroactivelyRevoke() throws Exception {
        // Create second content without quiz
        CourseContent ungateContent = new CourseContent();
        ungateContent.setModule(module);
        ungateContent.setTitle("Virtual Memory Introduction");
        ungateContent.setContentType("VIDEO");
        ungateContent.setPosition(2);
        ungateContent.setDurationSeconds(500);
        ungateContent = courseContentRepository.save(ungateContent);

        // Student watches 95% -> completed
        mockMvc.perform(post("/api/progress")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("contentId", ungateContent.getContentId()),
                                "progressPercent", 95.00,
                                "secondsWatched", 475
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        UserProgress prog = userProgressRepository.findByUserIdAndContent_ContentId(student.getUserId(), ungateContent.getContentId()).orElseThrow();
        assertThat(prog.getCompletedAt()).isNotNull();
        LocalDateTime originalCompleted = prog.getCompletedAt();

        // Teacher binds a required quiz to this content AFTER completion
        com.MOOC.OnlineLearningPlatfrom.Entity.Test newQuiz = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        newQuiz.setCourse(course);
        newQuiz.setContent(ungateContent);
        newQuiz.setTitle("Virtual Memory Quiz");
        newQuiz.setDurationMinutes(10);
        newQuiz.setRequiredForCompletion(true);
        newQuiz.setPassThreshold(new BigDecimal("70.00"));
        testRepository.save(newQuiz);
        entityManager.flush();

        // Subsequent progress reporting at >=90% watch MUST NOT retroactively revoke completedAt
        mockMvc.perform(post("/api/progress")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "content", Map.of("contentId", ungateContent.getContentId()),
                                "progressPercent", 96.00,
                                "secondsWatched", 480
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        UserProgress preserved = userProgressRepository.findByUserIdAndContent_ContentId(student.getUserId(), ungateContent.getContentId()).orElseThrow();
        assertThat(preserved.getCompletedAt()).isEqualTo(originalCompleted);
    }

    // -------------------------------------------------------------------------
    // 7. foreign content returns stealth 404
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 7: Foreign student querying content assessment receives stealth 404")
    public void testForeignContentReturnsStealth404() throws Exception {
        mockMvc.perform(get("/api/assessments/content/" + content.getContentId())
                        .with(auth(foreignStudent)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // 8. client completion rejected when quiz is not passed
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 8: Client calling complete directly without passing required quiz is rejected")
    public void testClientCompletionRejectedWhenQuizNotPassed() throws Exception {
        // Student attempts to call /api/student/lectures/{id}/complete directly
        mockMvc.perform(post("/api/student/lectures/" + lecture.getId() + "/complete")
                        .with(auth(student)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.detail", containsString("A required post-video quiz must be passed")));

        // Progress remains incomplete
        UserProgress prog = userProgressRepository.findByUserIdAndContent_ContentId(student.getUserId(), content.getContentId()).orElse(null);
        if (prog != null) {
            assertThat(prog.getCompletedAt()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // 9. quiz/attempt responses return private, no-store
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 9: Content quiz and attempt endpoints return Cache-Control: private, no-store")
    public void testQuizAttemptNoStoreHeaders() throws Exception {
        // GET /api/assessments/content/{contentId}
        mockMvc.perform(get("/api/assessments/content/" + content.getContentId())
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.id").value(quiz.getTestId()))
                .andExpect(jsonPath("$.placement").value("POST_VIDEO"))
                .andExpect(jsonPath("$.requiredForCompletion").value(true))
                .andExpect(jsonPath("$.passThreshold").value(60.00));

        // Start attempt
        MvcResult startRes = mockMvc.perform(post("/api/assessments/" + quiz.getTestId() + "/start")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andReturn();
        Long attId = objectMapper.readTree(startRes.getResponse().getContentAsString()).get("attemptId").asLong();

        // Finalize attempt
        mockMvc.perform(post("/api/assessments/attempts/" + attId + "/finalize")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")));
    }

    // -------------------------------------------------------------------------
    // 10. teacher authoring: bind quiz to CourseContent
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Scenario 10: Teacher can create and update quiz bound to CourseContent with post-video settings")
    public void testTeacherQuizContentAuthoring() throws Exception {
        // Teacher creates quiz bound to content
        String createPayload = objectMapper.writeValueAsString(Map.of(
                "course", Map.of("id", course.getId()),
                "content", Map.of("contentId", content.getContentId()),
                "title", "Advanced Process Scheduling Quiz",
                "durationMinutes", 20,
                "placement", "POST_VIDEO",
                "requiredForCompletion", true,
                "passThreshold", 75.00,
                "maxAttempts", 3
        ));

        MvcResult createRes = mockMvc.perform(post("/api/teacher/tests")
                        .with(auth(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode createdNode = objectMapper.readTree(createRes.getResponse().getContentAsString());
        Long newTestId = createdNode.get("testId").asLong();
        assertThat(newTestId).isNotNull();

        com.MOOC.OnlineLearningPlatfrom.Entity.Test savedInDb = testRepository.findById(newTestId).orElseThrow();
        assertThat(savedInDb.getContent()).isNotNull();
        assertThat(savedInDb.getContent().getContentId()).isEqualTo(content.getContentId());
        assertThat(savedInDb.getPlacement()).isEqualTo("POST_VIDEO");
        assertThat(savedInDb.getRequiredForCompletion()).isTrue();
        assertThat(savedInDb.getPassThreshold()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(savedInDb.getMaxAttempts()).isEqualTo(3);

        // Teacher updates quiz settings
        String updatePayload = objectMapper.writeValueAsString(Map.of(
                "title", "Updated Scheduling Quiz",
                "passThreshold", 80.00,
                "maxAttempts", 5
        ));

        mockMvc.perform(put("/api/teacher/tests/" + newTestId)
                        .with(auth(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Scheduling Quiz"))
                .andExpect(jsonPath("$.passThreshold").value(80.00))
                .andExpect(jsonPath("$.maxAttempts").value(5));
    }
}

