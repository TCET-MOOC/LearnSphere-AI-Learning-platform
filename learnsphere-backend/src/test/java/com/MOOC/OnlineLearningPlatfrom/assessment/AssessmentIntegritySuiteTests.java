package com.MOOC.OnlineLearningPlatfrom.assessment;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Service.AttemptTimeoutJob;
import com.MOOC.OnlineLearningPlatfrom.Service.AuditService;
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
 * W4-10: Comprehensive Assessment Integrity Suite.
 *
 * Rules: AC-15…AC-18, PLAN §10, A-10, A-12, BR-12, BR-13, NFR-03.
 *
 * Validates the core assessment integrity guarantees landed across Wave 4:
 * 1. Pre-submit answer leakage prevention across student assessment surfaces and retired legacy endpoints.
 * 2. In-progress attempt review answer and score masking for students with private, no-store caching.
 * 3. Finalized attempt review revelation (score, correct answers, awarded marks).
 * 4. One answer per question database uniqueness and in-place upsert semantics.
 * 5. Rejection of answer submissions after attempt finalization, abandonment, or duration timeout.
 * 6. Server-side auto-grading correctness for MCQ/TF while preserving null marks on TEXT questions.
 * 7. Instructor manual grading of TEXT questions with parent attempt score recalculation and audit logging.
 * 8. Attempt lifecycle with server-side timestamps (startedAt, completedAt) and background timeout job.
 * 9. Start attempt validation: scheduledAt future blocking, duration, in-progress resume, and maxAttempts limit.
 * 10. BigDecimal(8,2) precision across question marks, student marks awarded, and attempt scores.
 * 11. Stealth 404 IDOR defense for unenrolled students and cross-student / cross-teacher attempt probing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AssessmentIntegritySuiteTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EntityManager entityManager;

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
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AttemptTimeoutJob attemptTimeoutJob;

    @Autowired
    private UserDetailsService userDetailsService;

    private UserAccount teacher;
    private UserAccount student;
    private UserAccount foreignStudent;
    private Course course;
    private com.MOOC.OnlineLearningPlatfrom.Entity.Test test;
    private Question mcqQuestion;
    private Question tfQuestion;
    private Question textQuestion;

    @BeforeEach
    public void setup() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        foreignStudent = userAccountRepository.findByEmail("external@learnsphere.com");

        course = new Course();
        course.setTitle("Assessment Integrity Masterclass");
        course.setDescription("Comprehensive testing for Wave 4 assessment integrity");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        courseRepository.save(course);

        // Enroll legitimate student
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollment.setEnrolledAt(LocalDateTime.now().minusDays(1));
        enrollmentRepository.save(enrollment);

        test = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test.setCourse(course);
        test.setTitle("Wave 4 Integrity Exam");
        test.setDurationMinutes(30);
        test.setMaxAttempts(2);
        testRepository.save(test);

        // 1. MCQ Question
        mcqQuestion = new Question();
        mcqQuestion.setTest(test);
        mcqQuestion.setBody("What is the authoritative source of truth for LMS entities?");
        mcqQuestion.setQuestionType("MCQ");
        mcqQuestion.setOptions(List.of("LMS_Entity_Reference.pdf", "Wikipedia", "StackOverflow", "Random Guess"));
        mcqQuestion.setCorrectAnswer("LMS_Entity_Reference.pdf");
        mcqQuestion.setMarks(new BigDecimal("5.00"));
        questionRepository.save(mcqQuestion);

        // 2. TF Question
        tfQuestion = new Question();
        tfQuestion.setTest(test);
        tfQuestion.setBody("Student DTOs must strictly hide correctAnswer prior to submit.");
        tfQuestion.setQuestionType("TF");
        tfQuestion.setOptions(List.of("True", "False"));
        tfQuestion.setCorrectAnswer("True");
        tfQuestion.setMarks(new BigDecimal("3.50"));
        questionRepository.save(tfQuestion);

        // 3. TEXT Question (manual grading required)
        textQuestion = new Question();
        textQuestion.setTest(test);
        textQuestion.setBody("Explain why client-side score computation violates assessment integrity.");
        textQuestion.setQuestionType("TEXT");
        textQuestion.setOptions(List.of());
        textQuestion.setCorrectAnswer("Scores must be computed server-side from persisted answers only.");
        textQuestion.setMarks(new BigDecimal("10.00"));
        questionRepository.save(textQuestion);

        entityManager.flush();
    }

    private RequestPostProcessor auth(UserAccount user) {
        return user(userDetailsService.loadUserByUsername(user.getEmail()));
    }

    // -------------------------------------------------------------------------
    // 1. Pre-Submit Answer Leakage Prevention (AC-15, [D-F1], NFR-03)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Contract 1: Pre-submit questions hide correctAnswer in student DTO and retired surfaces return 404/405")
    public void testPreSubmitAnswerLeakagePrevention() throws Exception {
        // Enrolled student views assessment
        mockMvc.perform(get("/api/assessments/" + test.getTestId())
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.id").value(test.getTestId()))
                .andExpect(jsonPath("$.title").value("Wave 4 Integrity Exam"))
                .andExpect(jsonPath("$.questions", hasSize(3)))
                .andExpect(jsonPath("$.questions[0].body").value("What is the authoritative source of truth for LMS entities?"))
                .andExpect(jsonPath("$.questions[0].questionType").value("MCQ"))
                .andExpect(jsonPath("$.questions[0].options", hasSize(4)))
                .andExpect(jsonPath("$.questions[0].correctAnswer").doesNotExist()) // CRITICAL: correctAnswer must not exist
                .andExpect(jsonPath("$.questions[1].correctAnswer").doesNotExist())
                .andExpect(jsonPath("$.questions[2].correctAnswer").doesNotExist());

        // Retired leaky question reads return 404 or 405
        mockMvc.perform(get("/api/tests/" + test.getTestId() + "/questions")
                        .with(auth(student)))
                .andExpect(status().is(anyOf(is(404), is(405))));

        mockMvc.perform(get("/api/questions/" + mcqQuestion.getQuestionId())
                        .with(auth(student)))
                .andExpect(status().is(anyOf(is(404), is(405))));
    }

    // -------------------------------------------------------------------------
    // 2. In-Progress Attempt Review Privacy (AC-15, NFR-03)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Contract 2: In-progress attempt review masks correctAnswer, marksAwarded, and score from students")
    public void testInProgressAttemptReviewPrivacy() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(student.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
        attempt.setScore(BigDecimal.ZERO);
        testAttemptRepository.save(attempt);

        StudentAnswer answer = new StudentAnswer();
        answer.setAttempt(attempt);
        answer.setQuestion(mcqQuestion);
        answer.setAnswerText("LMS_Entity_Reference.pdf");
        answer.setMarksAwarded(new BigDecimal("5.00")); // Stored, but must be masked during IN_PROGRESS
        studentAnswerRepository.save(answer);
        entityManager.flush();

        // Student attempt review
        mockMvc.perform(get("/api/assessments/attempts/" + attempt.getAttemptId())
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.attemptId").value(attempt.getAttemptId()))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.score").doesNotExist()) // Masked for student in progress
                .andExpect(jsonPath("$.answers[0].marksAwarded").doesNotExist()) // Masked for student in progress
                .andExpect(jsonPath("$.answers[0].correctAnswer").doesNotExist()); // Masked for student in progress

        // Student answers list
        mockMvc.perform(get("/api/attempts/" + attempt.getAttemptId() + "/answers")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$[0].marksAwarded").doesNotExist());
    }

    // -------------------------------------------------------------------------
    // 3. Finalized Attempt Review Revelation (AC-15, NFR-03)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Contract 3: Finalized attempt review reveals score, correct answers, and marks awarded with private, no-store")
    public void testFinalizedAttemptReviewRevelation() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(student.getUserId());
        attempt.setStatus("COMPLETED");
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(20));
        attempt.setCompletedAt(LocalDateTime.now().minusMinutes(5));
        attempt.setScore(new BigDecimal("8.50"));
        testAttemptRepository.save(attempt);

        StudentAnswer ans1 = new StudentAnswer();
        ans1.setAttempt(attempt);
        ans1.setQuestion(mcqQuestion);
        ans1.setAnswerText("LMS_Entity_Reference.pdf");
        ans1.setMarksAwarded(new BigDecimal("5.00"));
        studentAnswerRepository.save(ans1);

        StudentAnswer ans2 = new StudentAnswer();
        ans2.setAttempt(attempt);
        ans2.setQuestion(tfQuestion);
        ans2.setAnswerText("True");
        ans2.setMarksAwarded(new BigDecimal("3.50"));
        studentAnswerRepository.save(ans2);
        entityManager.flush();

        mockMvc.perform(get("/api/assessments/attempts/" + attempt.getAttemptId())
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(8.50))
                .andExpect(jsonPath("$.answers", hasSize(2)))
                .andExpect(jsonPath("$.answers[0].correctAnswer").value("LMS_Entity_Reference.pdf"))
                .andExpect(jsonPath("$.answers[0].marksAwarded").value(5.00))
                .andExpect(jsonPath("$.answers[1].correctAnswer").value("True"))
                .andExpect(jsonPath("$.answers[1].marksAwarded").value(3.50));
    }

    // -------------------------------------------------------------------------
    // 4. One Answer Per Question & In-Place Upsert (AC-16, [D-F4])
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Contract 4: Unique (attempt_id, question_id) enforces in-place answer upsert without duplicate records")
    public void testUniqueAnswerUpsertSemantics() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(student.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(2));
        testAttemptRepository.save(attempt);
        entityManager.flush();

        // First answer submission
        String firstAnswerPayload = objectMapper.writeValueAsString(Map.of(
                "questionId", mcqQuestion.getQuestionId(),
                "answerText", "Wikipedia"
        ));

        MvcResult firstResult = mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstAnswerPayload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andReturn();

        JsonNode firstNode = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        Long firstAnswerId = firstNode.get("studentAnswerId").asLong();

        List<StudentAnswer> answersAfterFirst = studentAnswerRepository.findByAttempt_AttemptId(attempt.getAttemptId());
        assertThat(answersAfterFirst).hasSize(1);
        assertThat(answersAfterFirst.get(0).getAnswerText()).isEqualTo("Wikipedia");

        // Second submission for the same question (revised answer)
        String revisedAnswerPayload = objectMapper.writeValueAsString(Map.of(
                "questionId", mcqQuestion.getQuestionId(),
                "answerText", "LMS_Entity_Reference.pdf"
        ));

        MvcResult revisedResult = mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(revisedAnswerPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode revisedNode = objectMapper.readTree(revisedResult.getResponse().getContentAsString());
        Long revisedAnswerId = revisedNode.get("studentAnswerId").asLong();

        // Upsert must preserve the exact same answer ID without creating a duplicate record
        assertThat(revisedAnswerId).isEqualTo(firstAnswerId);

        List<StudentAnswer> answersAfterRevised = studentAnswerRepository.findByAttempt_AttemptId(attempt.getAttemptId());
        assertThat(answersAfterRevised).hasSize(1);
        assertThat(answersAfterRevised.get(0).getAnswerText()).isEqualTo("LMS_Entity_Reference.pdf");
    }

    // -------------------------------------------------------------------------
    // 5. Answer Submission Rejection Post-Finalize / Expiry (AC-16, AC-18)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Contract 5: Rejects answer submission after finalize, abandonment, duration timeout, or foreign question")
    public void testAnswerSubmissionRejections() throws Exception {
        // A. Completed attempt
        TestAttempt completedAttempt = new TestAttempt();
        completedAttempt.setTest(test);
        completedAttempt.setUserId(student.getUserId());
        completedAttempt.setStatus("COMPLETED");
        completedAttempt.setStartedAt(LocalDateTime.now().minusMinutes(20));
        completedAttempt.setCompletedAt(LocalDateTime.now().minusMinutes(5));
        testAttemptRepository.save(completedAttempt);

        String answerPayload = objectMapper.writeValueAsString(Map.of(
                "questionId", mcqQuestion.getQuestionId(),
                "answerText", "LMS_Entity_Reference.pdf"
        ));

        mockMvc.perform(post("/api/assessments/attempts/" + completedAttempt.getAttemptId() + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // B. Abandoned attempt
        TestAttempt abandonedAttempt = new TestAttempt();
        abandonedAttempt.setTest(test);
        abandonedAttempt.setUserId(student.getUserId());
        abandonedAttempt.setStatus("ABANDONED");
        abandonedAttempt.setStartedAt(LocalDateTime.now().minusMinutes(60));
        abandonedAttempt.setCompletedAt(LocalDateTime.now().minusMinutes(30));
        testAttemptRepository.save(abandonedAttempt);

        mockMvc.perform(post("/api/assessments/attempts/" + abandonedAttempt.getAttemptId() + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerPayload))
                .andExpect(status().isBadRequest());

        // C. Expired attempt (in_progress, but now > startedAt + duration)
        TestAttempt expiredAttempt = new TestAttempt();
        expiredAttempt.setTest(test);
        expiredAttempt.setUserId(student.getUserId());
        expiredAttempt.setStatus("IN_PROGRESS");
        expiredAttempt.setStartedAt(LocalDateTime.now().minusMinutes(40)); // Test duration is 30 mins
        testAttemptRepository.save(expiredAttempt);

        mockMvc.perform(post("/api/assessments/attempts/" + expiredAttempt.getAttemptId() + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(answerPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // Auto-abandoned in DB
        TestAttempt refreshed = testAttemptRepository.findById(expiredAttempt.getAttemptId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo("ABANDONED");
        assertThat(refreshed.getCompletedAt()).isNotNull();

        // D. Question belonging to another test
        com.MOOC.OnlineLearningPlatfrom.Entity.Test foreignTest = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        foreignTest.setCourse(course);
        foreignTest.setTitle("Other Test");
        foreignTest.setDurationMinutes(20);
        testRepository.save(foreignTest);

        Question foreignQuestion = new Question();
        foreignQuestion.setTest(foreignTest);
        foreignQuestion.setBody("Foreign question");
        foreignQuestion.setQuestionType("TEXT");
        foreignQuestion.setMarks(new BigDecimal("5.00"));
        questionRepository.save(foreignQuestion);

        TestAttempt activeAttempt = new TestAttempt();
        activeAttempt.setTest(test);
        activeAttempt.setUserId(student.getUserId());
        activeAttempt.setStatus("IN_PROGRESS");
        activeAttempt.setStartedAt(LocalDateTime.now().minusMinutes(2));
        testAttemptRepository.save(activeAttempt);
        entityManager.flush();

        String foreignAnswerPayload = objectMapper.writeValueAsString(Map.of(
                "questionId", foreignQuestion.getQuestionId(),
                "answerText", "Testing foreign question"
        ));

        mockMvc.perform(post("/api/assessments/attempts/" + activeAttempt.getAttemptId() + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foreignAnswerPayload))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // 6. Auto-Grading Correctness & Manual TEXT Grading (AC-16, AC-17, [D-F3])
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Contract 6: Auto-grades MCQ/TF, preserves null on TEXT, and derives score dynamically from stored answers")
    public void testAutoGradingAndDynamicScoring() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(student.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(5));
        testAttemptRepository.save(attempt);

        // 1. Submit MCQ (correct) -> 5.00
        mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", mcqQuestion.getQuestionId(),
                                "answerText", "LMS_Entity_Reference.pdf"
                        ))))
                .andExpect(status().isOk());

        // 2. Submit TF (wrong) -> 0.00
        mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", tfQuestion.getQuestionId(),
                                "answerText", "False"
                        ))))
                .andExpect(status().isOk());

        // 3. Submit TEXT -> marksAwarded must remain null
        mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", textQuestion.getQuestionId(),
                                "answerText", "Client-side grading exposes correctAnswer and can be manipulated by malicious requests."
                        ))))
                .andExpect(status().isOk());

        // Finalize attempt
        mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/finalize")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(5.00)); // 5.00 (MCQ) + 0 (TF) + null (TEXT) = 5.00

        // Verify TEXT answer in DB remains null
        StudentAnswer textAnswer = studentAnswerRepository.findByAttempt_AttemptId(attempt.getAttemptId())
                .stream().filter(a -> a.getQuestion().getQuestionId().equals(textQuestion.getQuestionId()))
                .findFirst().orElseThrow();
        assertThat(textAnswer.getMarksAwarded()).isNull();
    }

    // -------------------------------------------------------------------------
    // 7. Manual Grade Awards, Recalculation, and Audit Logging (AC-17, [D-F12])
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Contract 7: Teacher manual grade awards recalculate attempt score and record immutable audit log")
    public void testManualGradingScoreRecalculationAndAudit() throws Exception {
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(student.getUserId());
        attempt.setStatus("COMPLETED");
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(20));
        attempt.setCompletedAt(LocalDateTime.now().minusMinutes(10));
        attempt.setScore(new BigDecimal("5.00")); // Only MCQ graded initially
        testAttemptRepository.save(attempt);

        StudentAnswer mcqAnswer = new StudentAnswer();
        mcqAnswer.setAttempt(attempt);
        mcqAnswer.setQuestion(mcqQuestion);
        mcqAnswer.setAnswerText("LMS_Entity_Reference.pdf");
        mcqAnswer.setMarksAwarded(new BigDecimal("5.00"));
        studentAnswerRepository.save(mcqAnswer);

        StudentAnswer textAnswer = new StudentAnswer();
        textAnswer.setAttempt(attempt);
        textAnswer.setQuestion(textQuestion); // maxMarks = 10.00
        textAnswer.setAnswerText("Comprehensive explanation of server-side integrity.");
        textAnswer.setMarksAwarded(null);
        studentAnswerRepository.save(textAnswer);
        entityManager.flush();

        // Teacher grades TEXT answer: awards 8.50 marks
        String gradePayload = objectMapper.writeValueAsString(Map.of(
                "marksAwarded", 8.50,
                "feedback", "Excellent explanation of server clock and scoring rules."
        ));

        mockMvc.perform(patch("/api/answers/" + textAnswer.getStudentAnswerId() + "/grade")
                        .with(auth(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradePayload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.studentAnswerId").value(textAnswer.getStudentAnswerId()))
                .andExpect(jsonPath("$.marksAwarded").value(8.50));

        // Parent attempt score recalculated: 5.00 + 8.50 = 13.50
        TestAttempt updatedAttempt = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow();
        assertThat(updatedAttempt.getScore()).isEqualByComparingTo(new BigDecimal("13.50"));

        // Audit log verified
        List<AuditLog> logs = auditLogRepository.findAll().stream()
                .filter(l -> AuditService.MANUAL_GRADE_AWARDED.equals(l.getAction()))
                .toList();
        assertThat(logs).isNotEmpty();
        AuditLog latest = logs.get(logs.size() - 1);
        assertThat(latest.getActorId()).isEqualTo(teacher.getUserId());
        assertThat(latest.getResourceType()).isEqualTo(AuditService.RESOURCE_STUDENT_ANSWER);
        assertThat(latest.getResourceId()).isEqualTo(textAnswer.getStudentAnswerId());
        assertThat(new BigDecimal(latest.getAfterJson().get("marksAwarded").toString()))
                .isEqualByComparingTo(new BigDecimal("8.50"));
        assertThat(new BigDecimal(latest.getAfterJson().get("attemptScore").toString()))
                .isEqualByComparingTo(new BigDecimal("13.50"));

        // Student cannot call manual grade endpoint (stealth 404 IDOR defense)
        mockMvc.perform(patch("/api/answers/" + textAnswer.getStudentAnswerId() + "/grade")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(gradePayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // 8. Attempt Lifecycle & Background Timeout Job (AC-18, [D-F5, D-F11])
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Contract 8: Attempt lifecycle stamps server timestamps and background timeout job auto-abandons expired attempts")
    public void testAttemptLifecycleAndTimeoutJob() throws Exception {
        // Start attempt stamps server clock
        MvcResult startResult = mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("private, no-store")))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.completedAt").doesNotExist())
                .andReturn();

        JsonNode startNode = objectMapper.readTree(startResult.getResponse().getContentAsString());
        Long attemptId = startNode.get("attemptId").asLong();

        TestAttempt activeAttempt = testAttemptRepository.findById(attemptId).orElseThrow();
        assertThat(activeAttempt.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(activeAttempt.getStartedAt()).isNotNull();
        assertThat(activeAttempt.getCompletedAt()).isNull();

        // Simulate time passing beyond test duration (30 mins)
        activeAttempt.setStartedAt(LocalDateTime.now().minusMinutes(35));
        testAttemptRepository.save(activeAttempt);

        StudentAnswer ans = new StudentAnswer();
        ans.setAttempt(activeAttempt);
        ans.setQuestion(mcqQuestion);
        ans.setAnswerText("LMS_Entity_Reference.pdf");
        studentAnswerRepository.save(ans);
        entityManager.flush();

        // Run background timeout sweep job
        attemptTimeoutJob.checkAndExpireTimedOutAttempts();

        // Verify attempt transitioned to ABANDONED with completedAt set to deadline
        TestAttempt sweepAttempt = testAttemptRepository.findById(attemptId).orElseThrow();
        assertThat(sweepAttempt.getStatus()).isEqualTo("ABANDONED");
        assertThat(sweepAttempt.getCompletedAt()).isNotNull();
        assertThat(sweepAttempt.getScore()).isEqualByComparingTo(new BigDecimal("5.00")); // Auto-graded MCQ on sweep
    }

    // -------------------------------------------------------------------------
    // 9. Schedule, Duration, Resume & Max Attempts Enforcement (AC-18, [D-F6, D-F7, D-F10])
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Contract 9: Enforces scheduledAt blocking, active attempt resume, and maxAttempts limit on start")
    public void testScheduleAndAttemptLimitEnforcement() throws Exception {
        // A. Scheduled in the future blocks start
        com.MOOC.OnlineLearningPlatfrom.Entity.Test scheduledTest = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        scheduledTest.setCourse(course);
        scheduledTest.setTitle("Future Scheduled Test");
        scheduledTest.setDurationMinutes(20);
        scheduledTest.setScheduledAt(LocalDateTime.now().plusHours(2));
        testRepository.save(scheduledTest);

        mockMvc.perform(post("/api/assessments/" + scheduledTest.getTestId() + "/start")
                        .with(auth(student)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.detail", containsString("Test is scheduled for a future time")));

        // B. Active attempt resume: calling start with existing active attempt resumes same attempt
        MvcResult firstStart = mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andReturn();
        Long firstAttemptId = objectMapper.readTree(firstStart.getResponse().getContentAsString()).get("attemptId").asLong();

        MvcResult secondStart = mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andReturn();
        Long secondAttemptId = objectMapper.readTree(secondStart.getResponse().getContentAsString()).get("attemptId").asLong();

        assertThat(secondAttemptId).isEqualTo(firstAttemptId);

        // Finalize attempt 1
        mockMvc.perform(post("/api/assessments/attempts/" + firstAttemptId + "/finalize")
                        .with(auth(student)))
                .andExpect(status().isOk());

        // Start attempt 2 (maxAttempts = 2, so this should succeed)
        MvcResult thirdStart = mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andReturn();
        Long thirdAttemptId = objectMapper.readTree(thirdStart.getResponse().getContentAsString()).get("attemptId").asLong();
        assertThat(thirdAttemptId).isNotEqualTo(firstAttemptId);

        // Finalize attempt 2
        mockMvc.perform(post("/api/assessments/attempts/" + thirdAttemptId + "/finalize")
                        .with(auth(student)))
                .andExpect(status().isOk());

        // Start attempt 3: exceeds maxAttempts (2) -> rejected
        mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(auth(student)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.detail", containsString("Maximum attempt limit of 2 reached")));
    }

    // -------------------------------------------------------------------------
    // 10. BigDecimal Precision Across Schema, Marks, and Scores (A-10, [D-F13])
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Contract 10: Enforces exact BigDecimal arithmetic for fractional marks and attempt scores")
    public void testBigDecimalPrecision() throws Exception {
        // mcqQuestion: 5.00, tfQuestion: 3.50
        TestAttempt attempt = new TestAttempt();
        attempt.setTest(test);
        attempt.setUserId(student.getUserId());
        attempt.setStatus("IN_PROGRESS");
        attempt.setStartedAt(LocalDateTime.now().minusMinutes(2));
        testAttemptRepository.save(attempt);

        // Answer MCQ correctly -> +5.00
        mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", mcqQuestion.getQuestionId(),
                                "answerText", "LMS_Entity_Reference.pdf"
                        ))))
                .andExpect(status().isOk());

        // Answer TF correctly -> +3.50
        mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/answers")
                        .with(auth(student))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", tfQuestion.getQuestionId(),
                                "answerText", "True"
                        ))))
                .andExpect(status().isOk());

        // Finalize
        mockMvc.perform(post("/api/assessments/attempts/" + attempt.getAttemptId() + "/finalize")
                        .with(auth(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(8.50));

        TestAttempt finalized = testAttemptRepository.findById(attempt.getAttemptId()).orElseThrow();
        assertThat(finalized.getScore()).isEqualByComparingTo(new BigDecimal("8.50"));
    }

    // -------------------------------------------------------------------------
    // 11. Stealth 404 IDOR Defense (AC-15)
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Contract 11: Unenrolled students and foreign users receive stealth 404 on assessment endpoints")
    public void testStealth404IdorDefense() throws Exception {
        // Unenrolled student viewing test
        mockMvc.perform(get("/api/assessments/" + test.getTestId())
                        .with(auth(foreignStudent)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Unenrolled student starting test
        mockMvc.perform(post("/api/assessments/" + test.getTestId() + "/start")
                        .with(auth(foreignStudent)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Student attempt
        TestAttempt studentAttempt = new TestAttempt();
        studentAttempt.setTest(test);
        studentAttempt.setUserId(student.getUserId());
        studentAttempt.setStatus("IN_PROGRESS");
        studentAttempt.setStartedAt(LocalDateTime.now().minusMinutes(2));
        testAttemptRepository.save(studentAttempt);

        // Foreign student probing attempt review
        mockMvc.perform(get("/api/assessments/attempts/" + studentAttempt.getAttemptId())
                        .with(auth(foreignStudent)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Foreign student probing answers list
        mockMvc.perform(get("/api/attempts/" + studentAttempt.getAttemptId() + "/answers")
                        .with(auth(foreignStudent)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Foreign student probing finalize
        mockMvc.perform(post("/api/assessments/attempts/" + studentAttempt.getAttemptId() + "/finalize")
                        .with(auth(foreignStudent)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
