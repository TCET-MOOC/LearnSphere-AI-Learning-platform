package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * W2-04 integration tests: Module/content ordering, reorder endpoint, and delete re-normalisation.
 * Rules: AC-07, AC-08, [B-F6].
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ModuleContentOrderingTests extends PostgresIntegrationTest {

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
    private CourseContentRepository courseContentRepository;

    private UserAccount teacher;
    private UserAccount otherTeacher;
    private UserAccount admin;
    private UserAccount student;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");
        otherTeacher = userAccountRepository.findByEmail("pending@learnsphere.com");

        Department department = departmentRepository.findAll().stream().findFirst().orElseGet(() -> {
            Department d = new Department();
            d.setName("Computer Science & Engineering");
            return departmentRepository.save(d);
        });

        testCourse = new Course();
        testCourse.setTitle("Course Ordering Test");
        testCourse.setDescription("Testing module and content ordering");
        testCourse.setTeacher(teacher);
        testCourse.setDepartment(department);
        testCourse.setStatus(Course.Status.DRAFT);
        testCourse.setPrice(BigDecimal.ZERO);
        testCourse = courseRepository.save(testCourse);
    }

    private RequestPostProcessor asUser(UserAccount account) {
        return user(userDetailsService.loadUserByUsername(account.getEmail()));
    }

    @Test
    @DisplayName("Modules are returned ordered by position ASC")
    void modulesAreReturnedOrderedByPositionAsc() throws Exception {
        Module m1 = new Module();
        m1.setTitle("Module One");
        m1.setPosition(1);
        m1.setCourse(testCourse);
        moduleRepository.save(m1);

        Module m2 = new Module();
        m2.setTitle("Module Two");
        m2.setPosition(2);
        m2.setCourse(testCourse);
        moduleRepository.save(m2);

        Module m3 = new Module();
        m3.setTitle("Module Three");
        m3.setPosition(3);
        m3.setCourse(testCourse);
        moduleRepository.save(m3);

        mockMvc.perform(get("/api/courses/" + testCourse.getId() + "/modules")
                        .with(asUser(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title").value("Module One"))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[1].title").value("Module Two"))
                .andExpect(jsonPath("$[1].position").value(2))
                .andExpect(jsonPath("$[2].title").value("Module Three"))
                .andExpect(jsonPath("$[2].position").value(3));
    }

    @Test
    @DisplayName("Creating module without position assigns contiguous position (1..n)")
    void creatingModuleWithoutPositionAssignsContiguousPosition() throws Exception {
        mockMvc.perform(post("/api/courses/" + testCourse.getId() + "/modules")
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First Auto Module\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(1));

        mockMvc.perform(post("/api/courses/" + testCourse.getId() + "/modules")
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Second Auto Module\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(2));

        List<Module> modules = moduleRepository.findByCourse_IdOrderByPositionAsc(testCourse.getId());
        assertThat(modules).hasSize(2);
        assertThat(modules.get(0).getPosition()).isEqualTo(1);
        assertThat(modules.get(1).getPosition()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deleting a module re-normalises remaining positions to 1..n without gaps (AC-08)")
    void deletingModuleRenormalizesPositions() throws Exception {
        Module m1 = new Module();
        m1.setTitle("Module 1");
        m1.setPosition(1);
        m1.setCourse(testCourse);
        m1 = moduleRepository.save(m1);

        Module m2 = new Module();
        m2.setTitle("Module 2");
        m2.setPosition(2);
        m2.setCourse(testCourse);
        m2 = moduleRepository.save(m2);

        Module m3 = new Module();
        m3.setTitle("Module 3");
        m3.setPosition(3);
        m3.setCourse(testCourse);
        m3 = moduleRepository.save(m3);

        // Delete middle module (m2)
        mockMvc.perform(delete("/api/modules/" + m2.getModuleId())
                        .with(asUser(teacher)))
                .andExpect(status().isNoContent());

        List<Module> remaining = moduleRepository.findByCourse_IdOrderByPositionAsc(testCourse.getId());
        assertThat(remaining).hasSize(2);
        assertThat(remaining.get(0).getModuleId()).isEqualTo(m1.getModuleId());
        assertThat(remaining.get(0).getPosition()).isEqualTo(1);
        assertThat(remaining.get(1).getModuleId()).isEqualTo(m3.getModuleId());
        assertThat(remaining.get(1).getPosition()).isEqualTo(2);
    }

    @Test
    @DisplayName("PUT /api/modules/{id}/reorder shifts positions contiguously 1..n")
    void reorderModuleEndpointShiftsPositionsContiguously() throws Exception {
        Module m1 = new Module();
        m1.setTitle("Mod A");
        m1.setPosition(1);
        m1.setCourse(testCourse);
        m1 = moduleRepository.save(m1);

        Module m2 = new Module();
        m2.setTitle("Mod B");
        m2.setPosition(2);
        m2.setCourse(testCourse);
        m2 = moduleRepository.save(m2);

        Module m3 = new Module();
        m3.setTitle("Mod C");
        m3.setPosition(3);
        m3.setCourse(testCourse);
        m3 = moduleRepository.save(m3);

        // Move Mod C (pos 3) to position 1
        mockMvc.perform(put("/api/modules/" + m3.getModuleId() + "/reorder")
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\": 1}"))
                .andExpect(status().isOk());

        List<Module> reordered = moduleRepository.findByCourse_IdOrderByPositionAsc(testCourse.getId());
        assertThat(reordered).hasSize(3);
        assertThat(reordered.get(0).getModuleId()).isEqualTo(m3.getModuleId());
        assertThat(reordered.get(0).getPosition()).isEqualTo(1);
        assertThat(reordered.get(1).getModuleId()).isEqualTo(m1.getModuleId());
        assertThat(reordered.get(1).getPosition()).isEqualTo(2);
        assertThat(reordered.get(2).getModuleId()).isEqualTo(m2.getModuleId());
        assertThat(reordered.get(2).getPosition()).isEqualTo(3);
    }

    @Test
    @DisplayName("PUT /api/courses/{courseId}/modules/reorder with orderedIds array")
    void reorderModulesByOrderedIdList() throws Exception {
        Module m1 = new Module();
        m1.setTitle("Mod 1");
        m1.setPosition(1);
        m1.setCourse(testCourse);
        m1 = moduleRepository.save(m1);

        Module m2 = new Module();
        m2.setTitle("Mod 2");
        m2.setPosition(2);
        m2.setCourse(testCourse);
        m2 = moduleRepository.save(m2);

        Module m3 = new Module();
        m3.setTitle("Mod 3");
        m3.setPosition(3);
        m3.setCourse(testCourse);
        m3 = moduleRepository.save(m3);

        // Desired order: m2, m3, m1
        mockMvc.perform(put("/api/courses/" + testCourse.getId() + "/modules/reorder")
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderedIds\": [" + m2.getModuleId() + ", " + m3.getModuleId() + ", " + m1.getModuleId() + "]}"))
                .andExpect(status().isOk());

        List<Module> reordered = moduleRepository.findByCourse_IdOrderByPositionAsc(testCourse.getId());
        assertThat(reordered.get(0).getModuleId()).isEqualTo(m2.getModuleId());
        assertThat(reordered.get(0).getPosition()).isEqualTo(1);
        assertThat(reordered.get(1).getModuleId()).isEqualTo(m3.getModuleId());
        assertThat(reordered.get(1).getPosition()).isEqualTo(2);
        assertThat(reordered.get(2).getModuleId()).isEqualTo(m1.getModuleId());
        assertThat(reordered.get(2).getPosition()).isEqualTo(3);
    }

    @Test
    @DisplayName("Content items are returned ordered by position ASC")
    void contentItemsAreReturnedOrderedByPositionAsc() throws Exception {
        Module module = new Module();
        module.setTitle("Content Parent Module");
        module.setPosition(1);
        module.setCourse(testCourse);
        module = moduleRepository.save(module);

        CourseContent c1 = new CourseContent();
        c1.setTitle("Content One");
        c1.setContentType("VIDEO");
        c1.setPosition(1);
        c1.setModule(module);
        courseContentRepository.save(c1);

        CourseContent c2 = new CourseContent();
        c2.setTitle("Content Two");
        c2.setContentType("PDF");
        c2.setPosition(2);
        c2.setModule(module);
        courseContentRepository.save(c2);

        mockMvc.perform(get("/api/modules/" + module.getModuleId() + "/contents")
                        .with(asUser(teacher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("Content One"))
                .andExpect(jsonPath("$[0].position").value(1))
                .andExpect(jsonPath("$[1].title").value("Content Two"))
                .andExpect(jsonPath("$[1].position").value(2));
    }

    @Test
    @DisplayName("Deleting content re-normalises remaining content positions in module to 1..n")
    void deletingContentRenormalizesRemainingPositions() throws Exception {
        Module module = new Module();
        module.setTitle("Parent Module");
        module.setPosition(1);
        module.setCourse(testCourse);
        module = moduleRepository.save(module);

        CourseContent c1 = new CourseContent();
        c1.setTitle("C1");
        c1.setContentType("VIDEO");
        c1.setPosition(1);
        c1.setModule(module);
        c1 = courseContentRepository.save(c1);

        CourseContent c2 = new CourseContent();
        c2.setTitle("C2");
        c2.setContentType("PDF");
        c2.setPosition(2);
        c2.setModule(module);
        c2 = courseContentRepository.save(c2);

        CourseContent c3 = new CourseContent();
        c3.setTitle("C3");
        c3.setContentType("QUIZ");
        c3.setPosition(3);
        c3.setModule(module);
        c3 = courseContentRepository.save(c3);

        // Delete middle content (c2)
        mockMvc.perform(delete("/api/contents/" + c2.getContentId())
                        .with(asUser(teacher)))
                .andExpect(status().isNoContent());

        List<CourseContent> remaining = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(module.getModuleId());
        assertThat(remaining).hasSize(2);
        assertThat(remaining.get(0).getContentId()).isEqualTo(c1.getContentId());
        assertThat(remaining.get(0).getPosition()).isEqualTo(1);
        assertThat(remaining.get(1).getContentId()).isEqualTo(c3.getContentId());
        assertThat(remaining.get(1).getPosition()).isEqualTo(2);
    }

    @Test
    @DisplayName("PUT /api/contents/{id}/reorder shifts content positions contiguously 1..n")
    void reorderContentEndpointShiftsPositionsContiguously() throws Exception {
        Module module = new Module();
        module.setTitle("Parent Module");
        module.setPosition(1);
        module.setCourse(testCourse);
        module = moduleRepository.save(module);

        CourseContent c1 = new CourseContent();
        c1.setTitle("C1");
        c1.setContentType("VIDEO");
        c1.setPosition(1);
        c1.setModule(module);
        c1 = courseContentRepository.save(c1);

        CourseContent c2 = new CourseContent();
        c2.setTitle("C2");
        c2.setContentType("PDF");
        c2.setPosition(2);
        c2.setModule(module);
        c2 = courseContentRepository.save(c2);

        CourseContent c3 = new CourseContent();
        c3.setTitle("C3");
        c3.setContentType("QUIZ");
        c3.setPosition(3);
        c3.setModule(module);
        c3 = courseContentRepository.save(c3);

        // Move C3 (pos 3) to position 1
        mockMvc.perform(put("/api/contents/" + c3.getContentId() + "/reorder")
                        .with(asUser(teacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\": 1}"))
                .andExpect(status().isOk());

        List<CourseContent> reordered = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(module.getModuleId());
        assertThat(reordered).hasSize(3);
        assertThat(reordered.get(0).getContentId()).isEqualTo(c3.getContentId());
        assertThat(reordered.get(0).getPosition()).isEqualTo(1);
        assertThat(reordered.get(1).getContentId()).isEqualTo(c1.getContentId());
        assertThat(reordered.get(1).getPosition()).isEqualTo(2);
        assertThat(reordered.get(2).getContentId()).isEqualTo(c2.getContentId());
        assertThat(reordered.get(2).getPosition()).isEqualTo(3);
    }

    @Test
    @DisplayName("Foreign teacher or student attempting to reorder or delete module receives 404")
    void foreignTeacherOrStudentReceives404OnModuleReorderOrDelete() throws Exception {
        Module module = new Module();
        module.setTitle("Protected Module");
        module.setPosition(1);
        module.setCourse(testCourse);
        module = moduleRepository.save(module);

        // Foreign teacher cannot delete module
        mockMvc.perform(delete("/api/modules/" + module.getModuleId())
                        .with(asUser(otherTeacher)))
                .andExpect(status().isNotFound());

        // Student cannot delete module
        mockMvc.perform(delete("/api/modules/" + module.getModuleId())
                        .with(asUser(student)))
                .andExpect(status().isNotFound());

        // Foreign teacher cannot reorder module
        mockMvc.perform(put("/api/modules/" + module.getModuleId() + "/reorder")
                        .with(asUser(otherTeacher))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\": 1}"))
                .andExpect(status().isNotFound());
    }
}
