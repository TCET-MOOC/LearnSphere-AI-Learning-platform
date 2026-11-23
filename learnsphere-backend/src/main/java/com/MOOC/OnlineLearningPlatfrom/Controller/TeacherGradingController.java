package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.TeacherGradingDtos.PendingAnswer;
import com.MOOC.OnlineLearningPlatfrom.Dto.TeacherGradingDtos.TestSummary;
import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.StudentAnswer;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.QuestionRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.StudentAnswerRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Teacher assessments screen reads (SCR-35, W7-16; W4-05, AC-17). Scoped to the courses the
 * caller teaches (an admin sees every course); foreign tests are a stealth 404 via
 * {@link OwnershipService#requireTestStaff}. Grading itself stays on
 * {@code PATCH /api/answers/{id}/grade} (bounded by the question, attempt recomputed, audited).
 * Responses are DTOs and {@code private, no-store} (NFR-03).
 */
@RestController
@RequestMapping("/api/teacher/assessments")
public class TeacherGradingController {

    private final TestRepository testRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final UserAccountRepository userAccountRepository;
    private final OwnershipService ownershipService;
    private final QuestionRepository questionRepository;

    public TeacherGradingController(TestRepository testRepository,
                                    StudentAnswerRepository studentAnswerRepository,
                                    UserAccountRepository userAccountRepository,
                                    OwnershipService ownershipService,
                                    QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
        this.testRepository = testRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.userAccountRepository = userAccountRepository;
        this.ownershipService = ownershipService;
    }

    /** Tests per course/lecture with question counts and how many TEXT answers wait for a grade. */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<TestSummary>> tests(@AuthenticationPrincipal CustomUserDetails principal) {
        List<Test> tests = visibleTests(principal);
        List<Long> ids = tests.stream().map(Test::getTestId).toList();
        Map<Long, List<Question>> questionsByTest = ids.isEmpty() ? Map.of() : questionRepository.findByTest_TestIdIn(ids).stream()
                .collect(Collectors.groupingBy(q -> q.getTest().getTestId()));
        Map<Long, Long> pending = studentAnswerRepository
                .findUngradedTextAnswers(ids).stream()
                .collect(Collectors.groupingBy(a -> a.getAttempt().getTest().getTestId(), Collectors.counting()));
        List<TestSummary> body = tests.stream().map(t -> {
            List<Question> questions = questionsByTest.getOrDefault(t.getTestId(), List.of());
            int text = (int) questions.stream().filter(q -> "TEXT".equalsIgnoreCase(q.getQuestionType())).count();
            return new TestSummary(
                    t.getTestId(), t.getTitle(),
                    t.getCourse() != null ? t.getCourse().getId() : null,
                    t.getCourse() != null ? t.getCourse().getTitle() : null,
                    t.getLecture() != null ? t.getLecture().getId() : null,
                    t.getLecture() != null ? t.getLecture().getTitle() : null,
                    questions.size(), text, pending.getOrDefault(t.getTestId(), 0L));
        }).toList();
        return privateNoStore(body);
    }

    /** TEXT answers on finished attempts that still need marks; optionally one test. */
    @GetMapping("/pending")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PendingAnswer>> pending(@RequestParam(required = false) Long testId,
                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        Collection<Long> testIds = testId != null
                ? List.of(ownershipService.requireTestStaff(testId, principal).getTestId())
                : visibleTests(principal).stream().map(Test::getTestId).toList();
        List<StudentAnswer> answers = testIds.isEmpty() ? List.of() : studentAnswerRepository.findUngradedTextAnswers(testIds);
        Map<Long, UserAccount> students = userAccountRepository
                .findAllById(answers.stream().map(a -> a.getAttempt().getUserId()).distinct().toList()).stream()
                .collect(Collectors.toMap(UserAccount::getUserId, Function.identity()));
        List<PendingAnswer> body = answers.stream().map(a -> {
            TestAttempt attempt = a.getAttempt();
            Test test = attempt.getTest();
            Question q = a.getQuestion();
            UserAccount s = students.get(attempt.getUserId());
            return new PendingAnswer(
                    a.getStudentAnswerId(), attempt.getAttemptId(), test.getTestId(), test.getTitle(),
                    test.getCourse() != null ? test.getCourse().getTitle() : null,
                    attempt.getUserId(), s != null ? s.getFullName() : null,
                    q.getQuestionId(), q.getBody(), q.getMarks(), a.getAnswerText(),
                    attempt.getCompletedAt() != null ? attempt.getCompletedAt() : attempt.getStartedAt());
        }).toList();
        return privateNoStore(body);
    }

    private List<Test> visibleTests(CustomUserDetails principal) {
        if (ownershipService.isAdmin(principal)) {
            return testRepository.findAll().stream().filter(t -> t.getCourse() != null).toList();
        }
        return testRepository.findByCourse_Teacher_UserIdOrderByTestIdAsc(principal.getUser().getUserId());
    }

    private static <T> ResponseEntity<T> privateNoStore(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore().cachePrivate()).body(body);
    }
}
