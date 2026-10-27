package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Assignment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.AssignmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.AssignmentSubmissionRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.ModuleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W2-06 integration tests: Assignment validation (create and update) + submission rules.
 * Rules: AC-09, [B-F8].
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AssignmentValidationTests extends PostgresIntegrationTest {

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
    private ModuleRepository moduleRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private UserAccount teacher;
    private UserAccount otherTeacher;
    private UserAccount student;
    private Course course;
    private Module module;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        otherTeacher = userAccountRepository.findByEmail("pending@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");

        Department dept = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Computer Science");
            return departmentRepository.save(d);
        });

        course = new Course();
        course.setTitle("Distributed Systems");
        course.setTeacher(teacher);
        course.setDepartment(dept);
        course.setPrice(BigDecimal.ZERO);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        module = new Module();
        module.setCourse(course);
        module.setTitle("Consensus Protocols");
        module.setPosition(1);
        module = moduleRepository.save(module);

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);
    }

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    @Test
    @DisplayName("Create assignment with blank title returns 400 Bad Request")
    void createAssignment_withBlankTitle_returns400() throws Exception {
        String body = """
            {
              "title": "   ",
              "maxMarks": 100,
              "dueDate": "%s"
            }
            """.formatted(LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/api/modules/{id}/assignments", module.getModuleId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Create assignment with maxMarks <= 0 returns 400 Bad Request")
    void createAssignment_withNonPositiveMaxMarks_returns400() throws Exception {
        String body = """
            {
              "title": "Raft Assignment",
              "maxMarks": 0,
              "dueDate": "%s"
            }
            """.formatted(LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/api/modules/{id}/assignments", module.getModuleId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        String negativeBody = """
            {
              "title": "Raft Assignment",
              "maxMarks": -50,
              "dueDate": "%s"
            }
            """.formatted(LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/api/modules/{id}/assignments", module.getModuleId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negativeBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Create assignment with past dueDate returns 400 Bad Request")
    void createAssignment_withPastDueDate_returns400() throws Exception {
        String body = """
            {
              "title": "Past Assignment",
              "maxMarks": 100,
              "dueDate": "%s"
            }
            """.formatted(LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        mockMvc.perform(post("/api/modules/{id}/assignments", module.getModuleId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Create assignment with valid data succeeds with 200 OK")
    void createAssignment_withValidData_succeeds200() throws Exception {
        String futureDate = LocalDateTime.now().plusDays(10).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String body = """
            {
              "title": "Paxos Paper Review",
              "description": "Read Paxos Made Simple",
              "maxMarks": 75,
              "dueDate": "%s"
            }
            """.formatted(futureDate);

        mockMvc.perform(post("/api/modules/{id}/assignments", module.getModuleId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Paxos Paper Review"))
                .andExpect(jsonPath("$.maxMarks").value(75))
                .andExpect(jsonPath("$.assignmentId").isNumber());
    }

    @Test
    @DisplayName("Update assignment with blank title returns 400 Bad Request")
    void updateAssignment_withBlankTitle_returns400() throws Exception {
        Assignment assignment = new Assignment();
        assignment.setModule(module);
        assignment.setTitle("Initial Title");
        assignment.setMaxMarks(50);
        assignment.setDueDate(LocalDateTime.now().plusDays(5));
        assignment = assignmentRepository.save(assignment);

        String body = """
            {
              "title": "   "
            }
            """;

        mockMvc.perform(put("/api/assignments/{id}", assignment.getAssignmentId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Update assignment with maxMarks <= 0 returns 400 Bad Request")
    void updateAssignment_withNonPositiveMaxMarks_returns400() throws Exception {
        Assignment assignment = new Assignment();
        assignment.setModule(module);
        assignment.setTitle("Initial Title");
        assignment.setMaxMarks(50);
        assignment.setDueDate(LocalDateTime.now().plusDays(5));
        assignment = assignmentRepository.save(assignment);

        String zeroBody = """
            {
              "maxMarks": 0
            }
            """;

        mockMvc.perform(put("/api/assignments/{id}", assignment.getAssignmentId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(zeroBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        String negBody = """
            {
              "maxMarks": -10
            }
            """;

        mockMvc.perform(put("/api/assignments/{id}", assignment.getAssignmentId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Update assignment with past dueDate returns 400 Bad Request")
    void updateAssignment_withPastDueDate_returns400() throws Exception {
        Assignment assignment = new Assignment();
        assignment.setModule(module);
        assignment.setTitle("Initial Title");
        assignment.setMaxMarks(50);
        assignment.setDueDate(LocalDateTime.now().plusDays(5));
        assignment = assignmentRepository.save(assignment);

        String pastDate = LocalDateTime.now().minusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String body = """
            {
              "dueDate": "%s"
            }
            """.formatted(pastDate);

        mockMvc.perform(put("/api/assignments/{id}", assignment.getAssignmentId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Update assignment with valid fields succeeds with 200 OK")
    void updateAssignment_withValidData_succeeds200() throws Exception {
        Assignment assignment = new Assignment();
        assignment.setModule(module);
        assignment.setTitle("Initial Title");
        assignment.setMaxMarks(50);
        assignment.setDueDate(LocalDateTime.now().plusDays(5));
        assignment = assignmentRepository.save(assignment);

        String futureDate = LocalDateTime.now().plusDays(15).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String body = """
            {
              "title": "Updated Title",
              "maxMarks": 120,
              "dueDate": "%s"
            }
            """.formatted(futureDate);

        mockMvc.perform(put("/api/assignments/{id}", assignment.getAssignmentId())
                        .with(as("teacher@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.maxMarks").value(120));
    }

    @Test
    @DisplayName("Submission without text and without file returns 400 Bad Request")
    void submitAssignment_withoutTextOrFile_returns400() throws Exception {
        Assignment assignment = new Assignment();
        assignment.setModule(module);
        assignment.setTitle("Practical Lab");
        assignment.setMaxMarks(100);
        assignment.setDueDate(LocalDateTime.now().plusDays(5));
        assignment = assignmentRepository.save(assignment);

        String emptyBody = """
            {
              "submissionText": "   ",
              "fileUrl": "   "
            }
            """;

        mockMvc.perform(post("/api/assignments/{id}/submissions", assignment.getAssignmentId())
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Submission with valid text or file succeeds and upserts")
    void submitAssignment_withValidData_succeedsAndUpserts() throws Exception {
        Assignment assignment = new Assignment();
        assignment.setModule(module);
        assignment.setTitle("Practical Lab 2");
        assignment.setMaxMarks(100);
        assignment.setDueDate(LocalDateTime.now().plusDays(5));
        assignment = assignmentRepository.save(assignment);

        // Submit text only
        String textBody = """
            {
              "submissionText": "Here is my essay on Byzantine fault tolerance."
            }
            """;

        mockMvc.perform(post("/api/assignments/{id}/submissions", assignment.getAssignmentId())
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(textBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionText").value("Here is my essay on Byzantine fault tolerance."))
                .andExpect(jsonPath("$.studentId").value(student.getUserId()));

        // Upsert with file
        String fileBody = """
            {
              "fileUrl": "https://storage.example.com/bft-report.pdf"
            }
            """;

        mockMvc.perform(post("/api/assignments/{id}/submissions", assignment.getAssignmentId())
                        .with(as("student@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(fileBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileUrl").value("https://storage.example.com/bft-report.pdf"))
                .andExpect(jsonPath("$.studentId").value(student.getUserId()));

        assertThat(assignmentSubmissionRepository.findByAssignment_AssignmentId(assignment.getAssignmentId())).hasSize(1);
    }

    @Test
    @DisplayName("Foreign teacher cannot update or delete assignment (returns 404)")
    void foreignTeacherCannotMutateAssignment_returns404() throws Exception {
        Assignment assignment = new Assignment();
        assignment.setModule(module);
        assignment.setTitle("Private Assignment");
        assignment.setMaxMarks(50);
        assignment.setDueDate(LocalDateTime.now().plusDays(5));
        assignment = assignmentRepository.save(assignment);

        // Foreign teacher attempts update -> 404
        mockMvc.perform(put("/api/assignments/{id}", assignment.getAssignmentId())
                        .with(as("pending@learnsphere.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked Title\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Foreign teacher attempts delete -> 404
        mockMvc.perform(delete("/api/assignments/{id}", assignment.getAssignmentId())
                        .with(as("pending@learnsphere.com")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
