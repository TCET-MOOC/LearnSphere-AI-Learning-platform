package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W2-01 integration tests: Add courses.department_id FK, backfill, require FK on new courses.
 * Rules: A-03, [B-F2].
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourseDepartmentFkTests extends PostgresIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    private UserAccount teacher;
    private Department csDept;
    private Department eeDept;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        assertThat(teacher).isNotNull();

        csDept = departmentRepository.findByNameIgnoreCase("Computer Science")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Computer Science", null)));
        eeDept = departmentRepository.findByNameIgnoreCase("Electrical Engineering")
                .orElseGet(() -> departmentRepository.save(new Department(null, "Electrical Engineering", null)));
    }

    private RequestPostProcessor asTeacher() {
        return user(userDetailsService.loadUserByUsername(teacher.getEmail()));
    }

    @Test
    void shouldVerifyDatabaseSchemaHasDepartmentIdFkAndIndex() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.columns " +
                        "WHERE table_name = 'courses' AND column_name = 'department_id'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);

        Integer fkCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.table_constraints tc " +
                        "JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name " +
                        "WHERE tc.table_name = 'courses' AND kcu.column_name = 'department_id' " +
                        "AND tc.constraint_type = 'FOREIGN KEY'",
                Integer.class
        );
        assertThat(fkCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldRejectCourseCreationWithoutDepartment() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Discrete Mathematics",
                                  "description": "Logic and sets"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldRejectCourseCreationWithNonExistentDepartment() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Quantum Physics",
                                  "description": "Entanglement",
                                  "departmentId": 999999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldCreateCourseWithValidDepartmentId() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Algorithms & Data Structures",
                                  "description": "Graphs and trees",
                                  "departmentId": %d
                                }
                                """.formatted(csDept.getDepartmentId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Algorithms & Data Structures"))
                .andExpect(jsonPath("$.departmentId").value(csDept.getDepartmentId()))
                .andExpect(jsonPath("$.department").value(csDept.getName()));
    }

    @Test
    void shouldUpdateCourseDepartment() throws Exception {
        Course course = new Course();
        course.setTitle("Signal Processing");
        course.setDescription("Fourier analysis");
        course.setTeacher(teacher);
        course.setDepartment(csDept);
        course = courseRepository.save(course);

        mockMvc.perform(put("/api/courses/" + course.getId())
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "departmentId": %d
                                }
                                """.formatted(eeDept.getDepartmentId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentId").value(eeDept.getDepartmentId()))
                .andExpect(jsonPath("$.department").value(eeDept.getName()));

        Course refreshed = courseRepository.findById(course.getId()).orElseThrow();
        assertThat(refreshed.getDepartment().getDepartmentId()).isEqualTo(eeDept.getDepartmentId());
    }

    @Test
    void shouldReturnCoursesByDepartmentOnlyForLiveStatus() throws Exception {
        Course liveCourse = new Course();
        liveCourse.setTitle("Compilers");
        liveCourse.setDescription("Lexing and parsing");
        liveCourse.setTeacher(teacher);
        liveCourse.setDepartment(csDept);
        liveCourse.setStatus(Course.Status.LIVE);
        courseRepository.save(liveCourse);

        Course draftCourse = new Course();
        draftCourse.setTitle("Operating Systems Draft");
        draftCourse.setDescription("Kernel internals");
        draftCourse.setTeacher(teacher);
        draftCourse.setDepartment(csDept);
        draftCourse.setStatus(Course.Status.DRAFT);
        courseRepository.save(draftCourse);

        mockMvc.perform(get("/api/departments/" + csDept.getDepartmentId() + "/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.title == 'Compilers')]").exists())
                .andExpect(jsonPath("$[?(@.title == 'Operating Systems Draft')]").doesNotExist());
    }

    @Test
    void shouldReturnNotFoundForNonExistentDepartmentCourses() throws Exception {
        mockMvc.perform(get("/api/departments/999999/courses"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
