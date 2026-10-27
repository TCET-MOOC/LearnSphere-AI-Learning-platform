package com.MOOC.OnlineLearningPlatfrom.Security;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Unified ownership enforcement layer (W1-08).
 *
 * <p>Enforces BR-04 and IDOR protection:
 * Foreign IDs across curriculum, module, content, assignment, test, and enrolment
 * endpoints MUST return 404 Not Found (not 403) so resource existence is never leaked.
 * Course creator and ADMIN have mutating access.</p>
 */
@Service
public class OwnershipService {

    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final CourseContentRepository courseContentRepository;
    private final LectureRepository lectureRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;

    public OwnershipService(CourseRepository courseRepository,
                            ModuleRepository moduleRepository,
                            CourseContentRepository courseContentRepository,
                            LectureRepository lectureRepository,
                            AssignmentRepository assignmentRepository,
                            AssignmentSubmissionRepository assignmentSubmissionRepository,
                            TestRepository testRepository,
                            QuestionRepository questionRepository,
                            TestAttemptRepository testAttemptRepository,
                            EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.courseContentRepository = courseContentRepository;
        this.lectureRepository = lectureRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.testAttemptRepository = testAttemptRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    public boolean isAdmin(CustomUserDetails principal) {
        if (principal == null || principal.getRoles() == null) {
            return false;
        }
        return principal.getRoles().stream()
                .anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN"));
    }

    public boolean isCourseStaff(Course course, CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null || course == null) {
            return false;
        }
        if (isAdmin(principal)) {
            return true;
        }
        UserAccount teacher = course.getTeacher();
        if (teacher == null) {
            return false;
        }
        if (teacher.getUserId() != null && principal.getUser().getUserId() != null) {
            return teacher.getUserId().equals(principal.getUser().getUserId());
        }
        if (teacher.getEmail() != null && principal.getUsername() != null) {
            return teacher.getEmail().equalsIgnoreCase(principal.getUsername());
        }
        return false;
    }

    public Course requireCourseStaff(Long courseId, CustomUserDetails principal) {
        if (courseId == null) {
            throw new ResourceNotFoundException("Course not found with id: null");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        if (!isCourseStaff(course, principal)) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        return course;
    }

    public Module requireModuleStaff(Long moduleId, CustomUserDetails principal) {
        if (moduleId == null) {
            throw new ResourceNotFoundException("Module not found with id: null");
        }
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));
        if (module.getCourse() == null) {
            throw new ResourceNotFoundException("Module not found with id: " + moduleId);
        }
        requireCourseStaff(module.getCourse().getId(), principal);
        return module;
    }

    public CourseContent requireContentStaff(Long contentId, CustomUserDetails principal) {
        if (contentId == null) {
            throw new ResourceNotFoundException("Content not found with id: null");
        }
        CourseContent content = courseContentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found with id: " + contentId));
        if (content.getModule() == null) {
            throw new ResourceNotFoundException("Content not found with id: " + contentId);
        }
        requireModuleStaff(content.getModule().getModuleId(), principal);
        return content;
    }

    public Lecture requireLectureStaff(Long lectureId, CustomUserDetails principal) {
        if (lectureId == null) {
            throw new ResourceNotFoundException("Lecture not found with id: null");
        }
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with id: " + lectureId));
        if (lecture.getCourse() == null) {
            throw new ResourceNotFoundException("Lecture not found with id: " + lectureId);
        }
        requireCourseStaff(lecture.getCourse().getId(), principal);
        return lecture;
    }

    public Assignment requireAssignmentStaff(Long assignmentId, CustomUserDetails principal) {
        if (assignmentId == null) {
            throw new ResourceNotFoundException("Assignment not found with id: null");
        }
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));
        if (assignment.getModule() == null) {
            throw new ResourceNotFoundException("Assignment not found with id: " + assignmentId);
        }
        requireModuleStaff(assignment.getModule().getModuleId(), principal);
        return assignment;
    }

    public Test requireTestStaff(Long testId, CustomUserDetails principal) {
        if (testId == null) {
            throw new ResourceNotFoundException("Test not found with id: null");
        }
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));
        if (test.getCourse() != null) {
            requireCourseStaff(test.getCourse().getId(), principal);
        } else if (!isAdmin(principal)) {
            throw new ResourceNotFoundException("Test not found with id: " + testId);
        }
        return test;
    }

    public Question requireQuestionStaff(Long questionId, CustomUserDetails principal) {
        if (questionId == null) {
            throw new ResourceNotFoundException("Question not found with id: null");
        }
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found with id: " + questionId));
        if (question.getTest() == null) {
            throw new ResourceNotFoundException("Question not found with id: " + questionId);
        }
        requireTestStaff(question.getTest().getTestId(), principal);
        return question;
    }

    public TestAttempt requireAttemptOwnerOrStaff(Long attemptId, CustomUserDetails principal) {
        if (attemptId == null) {
            throw new ResourceNotFoundException("Attempt not found with id: null");
        }
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + attemptId));
        if (isAdmin(principal)) {
            return attempt;
        }
        if (principal != null && principal.getUser() != null && attempt.getUserId() != null
                && attempt.getUserId().equals(principal.getUser().getUserId())) {
            return attempt;
        }
        if (attempt.getTest() != null && attempt.getTest().getCourse() != null
                && isCourseStaff(attempt.getTest().getCourse(), principal)) {
            return attempt;
        }
        throw new ResourceNotFoundException("Attempt not found with id: " + attemptId);
    }

    public AssignmentSubmission requireSubmissionOwnerOrStaff(Long submissionId, CustomUserDetails principal) {
        if (submissionId == null) {
            throw new ResourceNotFoundException("Submission not found with id: null");
        }
        AssignmentSubmission sub = assignmentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));
        if (isAdmin(principal)) {
            return sub;
        }
        if (principal != null && principal.getUser() != null && sub.getStudent() != null
                && sub.getStudent().getUserId().equals(principal.getUser().getUserId())) {
            return sub;
        }
        if (sub.getAssignment() != null && sub.getAssignment().getModule() != null
                && sub.getAssignment().getModule().getCourse() != null
                && isCourseStaff(sub.getAssignment().getModule().getCourse(), principal)) {
            return sub;
        }
        throw new ResourceNotFoundException("Submission not found with id: " + submissionId);
    }

    public Enrollment requireEnrolled(Long courseId, CustomUserDetails principal) {
        if (courseId == null) {
            throw new ResourceNotFoundException("Course not found with id: null");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        if (isAdmin(principal) || isCourseStaff(course, principal)) {
            return enrollmentRepository.findByCourse_Id(courseId).stream()
                    .filter(e -> principal.getUser() != null && principal.getUser().getUserId().equals(e.getUserId()))
                    .findFirst()
                    .orElse(null);
        }
        if (principal == null || principal.getUser() == null) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }
        return enrollmentRepository.findByCourse_Id(courseId).stream()
                .filter(e -> principal.getUser().getUserId().equals(e.getUserId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
    }

    public void requireSelf(Long targetUserId, CustomUserDetails principal) {
        if (isAdmin(principal)) {
            return;
        }
        if (principal == null || principal.getUser() == null || targetUserId == null
                || !principal.getUser().getUserId().equals(targetUserId)) {
            throw new ResourceNotFoundException("User not found with id: " + targetUserId);
        }
    }

    public void requireAdmin(CustomUserDetails principal) {
        if (!isAdmin(principal)) {
            throw new AccessDeniedException("Admin access required");
        }
    }
}
