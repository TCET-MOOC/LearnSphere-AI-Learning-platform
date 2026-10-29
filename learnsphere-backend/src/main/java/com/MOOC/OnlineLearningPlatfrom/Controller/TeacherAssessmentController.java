package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Entity.Question;
import com.MOOC.OnlineLearningPlatfrom.Entity.Test;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LectureRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.QuestionRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.TestRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Security.OwnershipService;
import com.MOOC.OnlineLearningPlatfrom.Service.TestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Teacher-only endpoints for building assessments (tests/quizzes) on their own courses.
 * Enforces course ownership via stealth 404 (IDOR protection).
 */
@RestController
@RequestMapping("/api/teacher/tests")
public class TeacherAssessmentController {

    private final TestService testService;
    private final TestRepository testRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final QuestionRepository questionRepository;
    private final OwnershipService ownershipService;
    private final CourseContentRepository courseContentRepository;

    public TeacherAssessmentController(TestService testService,
                                       TestRepository testRepository,
                                       CourseRepository courseRepository,
                                       LectureRepository lectureRepository,
                                       QuestionRepository questionRepository,
                                       OwnershipService ownershipService,
                                       CourseContentRepository courseContentRepository) {
        this.testService = testService;
        this.testRepository = testRepository;
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.questionRepository = questionRepository;
        this.ownershipService = ownershipService;
        this.courseContentRepository = courseContentRepository;
    }

    @PostMapping
    public ResponseEntity<Test> createTest(@RequestBody Test test, @AuthenticationPrincipal CustomUserDetails principal) {
        if (test.getCourse() == null || test.getCourse().getId() == null) {
            throw new BadRequestException("A course is required to create a test.");
        }
        Course course = assertOwnedCourse(test.getCourse().getId(), principal.getUser().getUserId());
        test.setCourse(course);

        if (test.getContent() != null && test.getContent().getContentId() != null) {
            CourseContent content = courseContentRepository.findById(test.getContent().getContentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course content not found with id: " + test.getContent().getContentId()));
            test.setContent(content);
            if (test.getLecture() == null && content.getLegacyLectureId() != null) {
                lectureRepository.findById(content.getLegacyLectureId()).ifPresent(test::setLecture);
            }
        }

        if (test.getLecture() != null && test.getLecture().getId() != null && test.getLecture().getCourse() == null) {
            com.MOOC.OnlineLearningPlatfrom.Entity.Lecture lecture = lectureRepository.findById(test.getLecture().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with id: " + test.getLecture().getId()));
            test.setLecture(lecture);
        }

        return ResponseEntity.ok(testService.createTest(test));
    }

    @PutMapping("/{testId}")
    public ResponseEntity<Test> updateTest(@PathVariable Long testId,
                                           @RequestBody Test testDetails,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        Test test = ownershipService.requireTestStaff(testId, principal);
        if (testDetails.getTitle() != null) test.setTitle(testDetails.getTitle());
        if (testDetails.getDurationMinutes() != null) test.setDurationMinutes(testDetails.getDurationMinutes());
        if (testDetails.getIsRemedial() != null) test.setIsRemedial(testDetails.getIsRemedial());
        if (testDetails.getSecurityPolicy() != null) test.setSecurityPolicy(testDetails.getSecurityPolicy());
        if (testDetails.getScheduledAt() != null) test.setScheduledAt(testDetails.getScheduledAt());
        if (testDetails.getPlacement() != null) test.setPlacement(testDetails.getPlacement());
        if (testDetails.getRequiredForCompletion() != null) test.setRequiredForCompletion(testDetails.getRequiredForCompletion());
        if (testDetails.getPassThreshold() != null) test.setPassThreshold(testDetails.getPassThreshold());
        if (testDetails.getMaxAttempts() != null) test.setMaxAttempts(testDetails.getMaxAttempts());
        if (testDetails.getContent() != null && testDetails.getContent().getContentId() != null) {
            CourseContent content = courseContentRepository.findById(testDetails.getContent().getContentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course content not found with id: " + testDetails.getContent().getContentId()));
            test.setContent(content);
        }
        return ResponseEntity.ok(testRepository.save(test));
    }

    @PostMapping("/{testId}/questions")
    public ResponseEntity<Question> addQuestion(@PathVariable Long testId,
                                                @RequestBody Question question,
                                                @AuthenticationPrincipal CustomUserDetails principal) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));
        if (test.getCourse() == null) {
            throw new BadRequestException("This test is not linked to a course.");
        }
        assertOwnedCourse(test.getCourse().getId(), principal.getUser().getUserId());
        question.validateMcqCorrectAnswer();
        return ResponseEntity.ok(testService.addQuestionToTest(testId, question));
    }

    @PutMapping("/{testId}/questions/{questionId}")
    public ResponseEntity<Question> updateQuestion(@PathVariable Long testId,
                                                   @PathVariable Long questionId,
                                                   @RequestBody Question updated,
                                                   @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireTestStaff(testId, principal);
        Question question = ownershipService.requireQuestionStaff(questionId, principal);
        if (question.getTest() == null || !question.getTest().getTestId().equals(testId)) {
            throw new ResourceNotFoundException("Question not found with id: " + questionId);
        }
        if (updated.getBody() != null) question.setBody(updated.getBody());
        if (updated.getQuestionType() != null) question.setQuestionType(updated.getQuestionType());
        if (updated.getMarks() != null) question.setMarks(updated.getMarks());
        if (updated.getOptions() != null) question.setOptions(updated.getOptions());
        if (updated.getExtra() != null) question.setExtra(updated.getExtra());
        if (updated.getCorrectAnswer() != null) question.setCorrectAnswer(updated.getCorrectAnswer());
        question.validateMcqCorrectAnswer();
        return ResponseEntity.ok(questionRepository.save(question));
    }

    @DeleteMapping("/{testId}/questions/{questionId}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long testId,
                                               @PathVariable Long questionId,
                                               @AuthenticationPrincipal CustomUserDetails principal) {
        ownershipService.requireTestStaff(testId, principal);
        Question question = ownershipService.requireQuestionStaff(questionId, principal);
        if (question.getTest() == null || !question.getTest().getTestId().equals(testId)) {
            throw new ResourceNotFoundException("Question not found with id: " + questionId);
        }
        questionRepository.deleteById(questionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{testId}/questions/bulk")
    public ResponseEntity<List<Question>> addQuestionsBulk(@PathVariable Long testId,
                                                           @RequestBody List<Question> questions,
                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));
        if (test.getCourse() == null) {
            throw new BadRequestException("This test is not linked to a course.");
        }
        assertOwnedCourse(test.getCourse().getId(), principal.getUser().getUserId());

        List<Question> saved = new java.util.ArrayList<>();
        for (Question q : questions) {
            saved.add(testService.addQuestionToTest(testId, q));
        }
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Test>> getTestsByCourse(@PathVariable Long courseId,
                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        assertOwnedCourse(courseId, principal.getUser().getUserId());
        List<Test> tests = testRepository.findByCourse_Id(courseId);
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/lecture/{lectureId}")
    public ResponseEntity<List<Test>> getTestsByLecture(@PathVariable Long lectureId,
                                                        @AuthenticationPrincipal CustomUserDetails principal) {
        List<Test> tests = testRepository.findByLecture_Id(lectureId);
        return ResponseEntity.ok(tests);
    }

    @GetMapping("/{testId}")
    public ResponseEntity<Test> getTestById(@PathVariable Long testId,
                                            @AuthenticationPrincipal CustomUserDetails principal) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));
        if (test.getCourse() != null) {
            assertOwnedCourse(test.getCourse().getId(), principal.getUser().getUserId());
        }
        return ResponseEntity.ok(test);
    }

    @DeleteMapping("/{testId}")
    public ResponseEntity<Void> deleteTest(@PathVariable Long testId,
                                           @AuthenticationPrincipal CustomUserDetails principal) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));
        if (test.getCourse() != null) {
            assertOwnedCourse(test.getCourse().getId(), principal.getUser().getUserId());
        }
        testService.deleteTest(testId);
        return ResponseEntity.noContent().build();
    }

    private Course assertOwnedCourse(Long courseId, Long teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        if (course.getTeacher() == null || !course.getTeacher().getUserId().equals(teacherId)) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        return course;
    }
}
