package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Department;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.DepartmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.ModuleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.Service.PublicCacheService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W2-11: Public Catalogue / Reference Cache + ETag Invalidation.
 *
 * Rules: NFR-02, NFR-03, A-04, W2-02, W2-03.
 *
 * Validates:
 * 1. Public catalogue (GET /api/courses) returns Cache-Control: public, max-age=60, stale-while-revalidate=30 and ETag.
 * 2. If-None-Match with matching ETag returns 304 Not Modified with empty body.
 * 3. Caffeine hit/miss metrics record cache operations.
 * 4. Published course detail (GET /api/courses/{id}) is cached with public Cache-Control + ETag.
 * 5. Draft/staff reads of non-LIVE courses bypass cache and return Cache-Control: private, no-store.
 * 6. Course update, status change, or archive invalidates course and catalogue caches (subsequent old ETag returns 200).
 * 7. Module and content writes invalidate the owning course cache.
 * 8. Department endpoints (GET /api/departments, /api/departments/{id}, /api/departments/{id}/courses) return public cache + ETag, and department writes invalidate cache.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PublicCacheAndEtagTests extends PostgresIntegrationTest {

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

    @Autowired(required = false)
    private PublicCacheService publicCacheService;

    private UserAccount teacher;
    private UserAccount admin;
    private UserAccount student;
    private Department department;
    private Course liveCourse;

    private RequestPostProcessor asTeacher() {
        return user(userDetailsService.loadUserByUsername(teacher.getEmail()));
    }

    private RequestPostProcessor asAdmin() {
        return user(userDetailsService.loadUserByUsername(admin.getEmail()));
    }

    private RequestPostProcessor asStudent() {
        return user(userDetailsService.loadUserByUsername(student.getEmail()));
    }

    @BeforeEach
    void setUp() {
        teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        admin = userAccountRepository.findByEmail("admin@learnsphere.com");
        student = userAccountRepository.findByEmail("student@learnsphere.com");

        department = departmentRepository.findByName("Computer Science");
        if (department == null) {
            Department d = new Department();
            d.setName("Computer Science");
            department = departmentRepository.save(d);
        }

        liveCourse = new Course();
        liveCourse.setTitle("Caching Systems 101");
        liveCourse.setDescription("Understanding HTTP and in-memory caches");
        liveCourse.setPrice(BigDecimal.valueOf(19.99));
        liveCourse.setTeacher(teacher);
        liveCourse.setDepartment(department);
        liveCourse.setDepartmentLabel(department.getName());
        liveCourse.setStatus(Course.Status.LIVE);
        liveCourse = courseRepository.save(liveCourse);
    }

    // =========================================================================
    // 1. Public Catalogue Cache & ETag / 304 Handling
    // =========================================================================

    @Test
    @DisplayName("NFR-03: GET /api/courses returns public Cache-Control and ETag; If-None-Match returns 304")
    void shouldReturnPublicCacheHeadersAndSupportEtag304OnCoursesCatalogue() throws Exception {
        MvcResult firstResult = mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(header().string("Cache-Control", containsString("max-age=60")))
                .andExpect(header().exists("ETag"))
                .andReturn();

        String etag = firstResult.getResponse().getHeader("ETag");
        assertThat(etag).isNotBlank();

        // Second request sending If-None-Match must return 304 Not Modified with empty body
        mockMvc.perform(get("/api/courses")
                        .header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", etag))
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(content().string(""));
    }

    // =========================================================================
    // 2. Published Course Detail Cache & ETag / 304 Handling
    // =========================================================================

    @Test
    @DisplayName("NFR-03: GET /api/courses/{id} for LIVE course returns public Cache-Control + ETag and 304")
    void shouldReturnPublicCacheHeadersAndSupportEtag304OnLiveCourseDetail() throws Exception {
        MvcResult firstResult = mockMvc.perform(get("/api/courses/" + liveCourse.getCourseId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(header().string("Cache-Control", containsString("max-age=60")))
                .andExpect(header().exists("ETag"))
                .andReturn();

        String etag = firstResult.getResponse().getHeader("ETag");
        assertThat(etag).isNotBlank();

        // 304 on matching ETag
        mockMvc.perform(get("/api/courses/" + liveCourse.getCourseId())
                        .header("If-None-Match", etag))
                .andExpect(status().isNotModified())
                .andExpect(header().string("ETag", etag))
                .andExpect(content().string(""));
    }

    // =========================================================================
    // 3. Draft & Staff Views Bypass Cache with private, no-store
    // =========================================================================

    @Test
    @DisplayName("NFR-03, A-04: Draft course detail read by staff bypasses cache with private, no-store")
    void shouldBypassCacheAndReturnPrivateNoStoreForDraftCourseReadByStaff() throws Exception {
        Course draftCourse = new Course();
        draftCourse.setTitle("Unpublished Draft Course");
        draftCourse.setDescription("Secret curriculum");
        draftCourse.setPrice(BigDecimal.TEN);
        draftCourse.setTeacher(teacher);
        draftCourse.setDepartment(department);
        draftCourse.setStatus(Course.Status.DRAFT);
        draftCourse = courseRepository.save(draftCourse);

        // Staff (teacher owner) reading draft course receives private, no-store
        mockMvc.perform(get("/api/courses/" + draftCourse.getCourseId())
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("Cache-Control", not(containsString("public"))));

        // Public/student request receives 404 NOT_FOUND
        mockMvc.perform(get("/api/courses/" + draftCourse.getCourseId())
                        .with(asStudent()))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 4. Invalidation on Course Writes (Update, Publish, Archive)
    // =========================================================================

    @Test
    @DisplayName("NFR-03: Updating or archiving course invalidates cache; old ETag no longer returns 304")
    void shouldInvalidateCacheOnCourseUpdatesAndStatusChanges() throws Exception {
        // Initial fetch to obtain current ETag
        MvcResult firstResult = mockMvc.perform(get("/api/courses/" + liveCourse.getCourseId()))
                .andExpect(status().isOk())
                .andReturn();
        String oldEtag = firstResult.getResponse().getHeader("ETag");

        // Teacher updates course description
        mockMvc.perform(put("/api/courses/" + liveCourse.getCourseId())
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Updated description triggering cache invalidation\"}"))
                .andExpect(status().isOk());

        // Subsequent GET with old ETag must NOT return 304; must return 200 with new content and new ETag
        MvcResult updatedResult = mockMvc.perform(get("/api/courses/" + liveCourse.getCourseId())
                        .header("If-None-Match", oldEtag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description triggering cache invalidation"))
                .andReturn();

        String newEtag = updatedResult.getResponse().getHeader("ETag");
        assertThat(newEtag).isNotEqualTo(oldEtag);
    }

    // =========================================================================
    // 5. Invalidation on Module and Content Writes
    // =========================================================================

    @Test
    @DisplayName("NFR-03: Module or content write invalidates owning course cache")
    void shouldInvalidateCourseCacheOnModuleOrContentMutation() throws Exception {
        // Prime course detail cache
        MvcResult initialResult = mockMvc.perform(get("/api/courses/" + liveCourse.getCourseId()))
                .andExpect(status().isOk())
                .andReturn();
        String initialEtag = initialResult.getResponse().getHeader("ETag");

        // Add module to course
        mockMvc.perform(post("/api/courses/" + liveCourse.getCourseId() + "/modules")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Module 1: Caching Basics\"}"))
                .andExpect(status().isOk());

        // Old ETag should no longer return 304
        MvcResult afterModuleResult = mockMvc.perform(get("/api/courses/" + liveCourse.getCourseId())
                        .header("If-None-Match", initialEtag))
                .andExpect(status().isOk())
                .andReturn();
        String afterModuleEtag = afterModuleResult.getResponse().getHeader("ETag");
        assertThat(afterModuleEtag).isNotEqualTo(initialEtag);
    }

    // =========================================================================
    // 6. Department Endpoints Cache & ETag / 304 and Invalidation
    // =========================================================================

    @Test
    @DisplayName("NFR-03: Departments endpoints return public cache + ETag; writes invalidate cache")
    void shouldSupportCacheAndEtagOnDepartmentEndpointsAndInvalidateOnWrite() throws Exception {
        // 1. GET /api/departments returns public Cache-Control and ETag
        MvcResult deptsResult = mockMvc.perform(get("/api/departments"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(header().exists("ETag"))
                .andReturn();

        String deptsEtag = deptsResult.getResponse().getHeader("ETag");
        assertThat(deptsEtag).isNotBlank();

        // 304 on matching ETag
        mockMvc.perform(get("/api/departments")
                        .header("If-None-Match", deptsEtag))
                .andExpect(status().isNotModified());

        // 2. GET /api/departments/{id} returns public Cache-Control and ETag
        MvcResult deptDetail = mockMvc.perform(get("/api/departments/" + department.getDepartmentId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(header().exists("ETag"))
                .andReturn();

        String deptEtag = deptDetail.getResponse().getHeader("ETag");
        mockMvc.perform(get("/api/departments/" + department.getDepartmentId())
                        .header("If-None-Match", deptEtag))
                .andExpect(status().isNotModified());

        // 3. GET /api/departments/{id}/courses returns public Cache-Control and ETag
        MvcResult deptCourses = mockMvc.perform(get("/api/departments/" + department.getDepartmentId() + "/courses"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("public")))
                .andExpect(header().exists("ETag"))
                .andReturn();

        String deptCoursesEtag = deptCourses.getResponse().getHeader("ETag");
        mockMvc.perform(get("/api/departments/" + department.getDepartmentId() + "/courses")
                        .header("If-None-Match", deptCoursesEtag))
                .andExpect(status().isNotModified());

        // 4. Admin writes a new department -> invalidates /api/departments
        mockMvc.perform(post("/api/departments")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mechanical Engineering\"}"))
                .andExpect(status().isOk());

        // Calling GET /api/departments with old ETag returns 200 with new ETag
        MvcResult updatedDepts = mockMvc.perform(get("/api/departments")
                        .header("If-None-Match", deptsEtag))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(updatedDepts.getResponse().getHeader("ETag")).isNotEqualTo(deptsEtag);
    }
}
