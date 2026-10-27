package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W1-08 integration test suite for the unified ownership layer.
 *
 * <p>Enforces BR-04, AC-04, and IDOR protection:
 * Foreign IDs across curriculum, module, content, assignment, test, and enrolment
 * endpoints MUST return 404 Not Found (not 403) so resource existence is never leaked.
 * Course creator and ADMIN have mutating access.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OwnershipAuthorizationTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private ModuleRepository moduleRepository;

    @Autowired
    private CourseContentRepository courseContentRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private TestAttemptRepository testAttemptRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private Long ownerCourseId;
    private Long moduleId;
    private Long contentId;
    private Long assignmentId;
    private Long testId;
    private Long questionId;
    private Long enrollmentId;

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @BeforeEach
    void setUpFixtures() {
        // Teacher 1 is the seeded teacher (teacher@learnsphere.com)
        UserAccount teacher1 = userAccountRepository.findByEmail("teacher@learnsphere.com");
        assertThat(teacher1).isNotNull();

        // Find seeded course CS201
        Course course = courseRepository.findByTeacher_UserId(teacher1.getUserId()).stream()
                .findFirst()
                .orElseThrow();
        ownerCourseId = course.getId();

        // Create a Module under course
        Module module = new Module();
        module.setCourse(course);
        module.setTitle("Module 1: Foundations");
        module.setPosition(1);
        module = moduleRepository.save(module);
        moduleId = module.getModuleId();

        // Create CourseContent under module
        CourseContent content = new CourseContent();
        content.setModule(module);
        content.setTitle("Lecture 1 Video");
        content.setContentType("VIDEO");
        content.setPosition(1);
        content.setDurationSeconds(600);
        content = courseContentRepository.save(content);
        contentId = content.getContentId();

        // Create Assignment under module
        Assignment assignment = new Assignment();
        assignment.setModule(module);
        assignment.setTitle("Assignment 1");
        assignment.setMaxMarks(100);
        assignment.setDueDate(LocalDateTime.now().plusDays(7));
        assignment = assignmentRepository.save(assignment);
        assignmentId = assignment.getAssignmentId();

        // Create Test under course
        com.MOOC.OnlineLearningPlatfrom.Entity.Test test = new com.MOOC.OnlineLearningPlatfrom.Entity.Test();
        test.setCourse(course);
        test.setTitle("Quiz 1");
        test.setDurationMinutes(30);
        test = testRepository.save(test);
        testId = test.getTestId();

        // Create Question under test
        Question question = new Question();
        question.setTest(test);
        question.setBody("What is 2+2?");
        question.setQuestionType("MCQ");
        question.setMarks(5);
        question.setCorrectAnswer("4");
        question = questionRepository.save(question);
        questionId = question.getQuestionId();

        // Student 1 (student@learnsphere.com) enrollment in CS201
        UserAccount student1 = userAccountRepository.findByEmail("student@learnsphere.com");
        Enrollment enrollment = enrollmentRepository.findByCourse_Id(ownerCourseId).stream()
                .filter(e -> e.getUserId().equals(student1.getUserId()))
                .findFirst()
                .orElseThrow();
        enrollmentId = enrollment.getEnrollmentId();
    }

    @Test
    void foreignTeacherCannotMutateCourse_returns404() throws Exception {
        // pending@learnsphere.com is another teacher who does NOT own this course
        mockMvc.perform(put("/api/courses/{id}", ownerCourseId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hijacked Course Title\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(delete("/api/courses/{id}", ownerCourseId)
                        .with(as("pending@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void foreignTeacherCannotMutateModule_returns404() throws Exception {
        // Create module under foreign course
        mockMvc.perform(post("/api/courses/{courseId}/modules", ownerCourseId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Unauthorized Module\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Update foreign module
        mockMvc.perform(put("/api/modules/{id}", moduleId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed Module\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Delete foreign module
        mockMvc.perform(delete("/api/modules/{id}", moduleId)
                        .with(as("pending@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void foreignTeacherCannotMutateContent_returns404() throws Exception {
        // Add content to foreign module
        mockMvc.perform(post("/api/modules/{id}/contents", moduleId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Video\",\"contentType\":\"VIDEO\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Update foreign content
        mockMvc.perform(put("/api/contents/{id}", contentId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed Content\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Delete foreign content
        mockMvc.perform(delete("/api/contents/{id}", contentId)
                        .with(as("pending@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void foreignTeacherCannotMutateAssignment_returns404() throws Exception {
        // Create assignment under foreign module
        mockMvc.perform(post("/api/modules/{id}/assignments", moduleId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Rogue Assignment\",\"maxMarks\":50}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Update foreign assignment
        mockMvc.perform(put("/api/assignments/{id}", assignmentId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Assignment\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Delete foreign assignment
        mockMvc.perform(delete("/api/assignments/{id}", assignmentId)
                        .with(as("pending@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Get submissions of foreign assignment
        mockMvc.perform(get("/api/assignments/{id}/submissions", assignmentId)
                        .with(as("pending@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void foreignTeacherCannotMutateTestOrQuestions_returns404() throws Exception {
        // Create test under foreign course
        mockMvc.perform(post("/api/courses/{courseId}/tests", ownerCourseId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Rogue Test\",\"durationMinutes\":20}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Update foreign test
        mockMvc.perform(put("/api/tests/{testId}", testId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Modified Test Title\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Delete foreign test
        mockMvc.perform(delete("/api/tests/{testId}", testId)
                        .with(as("pending@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Add question to foreign test
        mockMvc.perform(post("/api/tests/{testId}/questions", testId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"What is 1+1?\",\"marks\":2}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Update foreign question
        mockMvc.perform(put("/api/questions/{id}", questionId)
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"Modified question body\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Delete foreign question
        mockMvc.perform(delete("/api/questions/{id}", questionId)
                        .with(as("pending@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void courseOwnerAndAdminCanMutateCourseAndCurriculum() throws Exception {
        // Teacher owner update
        mockMvc.perform(put("/api/courses/{id}", ownerCourseId)
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated By Owner\"}"))
                .andExpect(status().isOk());

        // Admin update
        mockMvc.perform(put("/api/courses/{id}", ownerCourseId)
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated By Admin\"}"))
                .andExpect(status().isOk());

        // Teacher owner update module
        mockMvc.perform(put("/api/modules/{id}", moduleId)
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Owner Updated Module\"}"))
                .andExpect(status().isOk());

        // Admin update module
        mockMvc.perform(put("/api/modules/{id}", moduleId)
                        .with(as("admin@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Admin Updated Module\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void studentCannotDeleteAnotherStudentsEnrollment_returns404() throws Exception {
        // external@learnsphere.com is another student
        mockMvc.perform(delete("/api/enrollments/{id}", enrollmentId)
                        .with(as("external@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Student owner can unenroll
        mockMvc.perform(delete("/api/enrollments/{id}", enrollmentId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isNoContent());

        assertThat(enrollmentRepository.findById(enrollmentId)).isEmpty();
    }

    @Test
    void studentCannotLogProgressForAnotherStudent_returns404() throws Exception {
        UserAccount student1 = userAccountRepository.findByEmail("student@learnsphere.com");

        // external@ tries to log progress for student1
        mockMvc.perform(post("/api/progress")
                        .with(as("external@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + student1.getUserId() + ",\"progressPercent\":50}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void unenrolledStudentCannotStartTest_returns404() throws Exception {
        // external@learnsphere.com is NOT enrolled in CS201
        mockMvc.perform(post("/api/tests/{testId}/attempts", testId)
                        .with(as("external@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // student@learnsphere.com IS enrolled in CS201
        mockMvc.perform(post("/api/tests/{testId}/attempts", testId)
                        .with(as("student@learnsphere.com")))
                .andExpect(status().isOk());
    }
}
