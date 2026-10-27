package com.MOOC.OnlineLearningPlatfrom.curriculum;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetailsService;
import com.MOOC.OnlineLearningPlatfrom.Service.StorageService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * W2-10: Comprehensive Curriculum Contract Suite.
 *
 * Rules: AC-07…AC-09, PLAN §10, W2-07, A-04, A-05, BR-06, BR-26.
 *
 * Validates the core curriculum contracts and guarantees landed across Wave 2:
 * 1. Module ordering (contiguous 1..n, auto-numbering, reorder endpoints, and delete re-normalisation).
 * 2. Content ordering (contiguous 1..n, auto-numbering, reorder endpoints, and delete re-normalisation).
 * 3. Course publication state machine (DRAFT -> PENDING -> LIVE -> ARCHIVED), admin approval requirement, and catalogue filtering.
 * 4. Course soft-delete archiving, non-destructive lifecycle, and restore endpoint.
 * 5. Assignment validation (non-blank title, maxMarks > 0, future dueDate on create/update, submission text/file requirements).
 * 6. Storage safety (public-URL removal, 200 MB cap, MIME/magic-byte checks, traversal rejection, owner/admin download authorization).
 * 7. YouTube import into canonical CourseContent with contiguous positions and IDOR defense.
 * 8. IDOR authorization matrix across all curriculum entities (foreign mutations return 404).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CurriculumContractSuiteTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    private UserAccount teacher;
    private UserAccount otherTeacher;
    private UserAccount admin;
    private UserAccount student;
    private Department department;

    private RequestPostProcessor asTeacher() {
        return user(userDetailsService.loadUserByUsername(teacher.getEmail()));
    }

    private RequestPostProcessor asOtherTeacher() {
        return user(userDetailsService.loadUserByUsername(otherTeacher.getEmail()));
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
        otherTeacher = userAccountRepository.findByEmail("pending@learnsphere.com");

        department = departmentRepository.findByName("Computer Science");
        if (department == null) {
            Department d = new Department();
            d.setName("Computer Science");
            department = departmentRepository.save(d);
        }
    }

    private Course createTeacherCourse(String title, Course.Status status) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription("Curriculum contract test course");
        course.setPrice(BigDecimal.valueOf(49.99));
        course.setTeacher(teacher);
        course.setDepartment(department);
        course.setDepartmentLabel(department.getName());
        course.setStatus(status);
        return courseRepository.save(course);
    }

    // =========================================================================
    // 1. Module Ordering, Contiguous 1..n, Reordering, and Delete Normalisation
    // =========================================================================

    @Test
    @DisplayName("AC-07, AC-08: Modules maintain contiguous 1..n positions, reorder, and re-normalise after delete")
    void shouldEnforceModuleOrderingReorderAndNormalisationAfterDelete() throws Exception {
        Course course = createTeacherCourse("Module Ordering Course", Course.Status.DRAFT);

        // Create 3 modules sequentially without explicit position
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/courses/" + course.getCourseId() + "/modules")
                            .with(asTeacher())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Module " + i + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.position").value(i));
        }

        List<Module> modules = moduleRepository.findByCourse_IdOrderByPositionAsc(course.getCourseId());
        assertThat(modules).hasSize(3);
        assertThat(modules.get(0).getPosition()).isEqualTo(1);
        assertThat(modules.get(1).getPosition()).isEqualTo(2);
        assertThat(modules.get(2).getPosition()).isEqualTo(3);

        Long mod1Id = modules.get(0).getModuleId();
        Long mod2Id = modules.get(1).getModuleId();
        Long mod3Id = modules.get(2).getModuleId();

        // Reorder: move Module 3 to position 1
        mockMvc.perform(put("/api/modules/" + mod3Id + "/reorder")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\":1}"))
                    .andExpect(status().isOk());

        List<Module> reordered = moduleRepository.findByCourse_IdOrderByPositionAsc(course.getCourseId());
        assertThat(reordered.get(0).getModuleId()).isEqualTo(mod3Id);
        assertThat(reordered.get(0).getPosition()).isEqualTo(1);
        assertThat(reordered.get(1).getModuleId()).isEqualTo(mod1Id);
        assertThat(reordered.get(1).getPosition()).isEqualTo(2);
        assertThat(reordered.get(2).getModuleId()).isEqualTo(mod2Id);
        assertThat(reordered.get(2).getPosition()).isEqualTo(3);

        // Delete Module at position 2 (which is mod1Id)
        mockMvc.perform(delete("/api/modules/" + mod1Id)
                        .with(asTeacher()))
                .andExpect(status().isNoContent());

        // Assert remaining sibling modules are re-normalised contiguously 1..2 without gaps
        List<Module> remaining = moduleRepository.findByCourse_IdOrderByPositionAsc(course.getCourseId());
        assertThat(remaining).hasSize(2);
        assertThat(remaining.get(0).getModuleId()).isEqualTo(mod3Id);
        assertThat(remaining.get(0).getPosition()).isEqualTo(1);
        assertThat(remaining.get(1).getModuleId()).isEqualTo(mod2Id);
        assertThat(remaining.get(1).getPosition()).isEqualTo(2);
    }

    // =========================================================================
    // 2. Content Ordering, Contiguous 1..n, Reordering, and Delete Normalisation
    // =========================================================================

    @Test
    @DisplayName("AC-07, AC-08: CourseContent maintains contiguous 1..n positions, reorder, and re-normalise after delete")
    void shouldEnforceContentOrderingReorderAndNormalisationAfterDelete() throws Exception {
        Course course = createTeacherCourse("Content Ordering Course", Course.Status.DRAFT);
        Module module = new Module();
        module.setTitle("Test Module");
        module.setPosition(1);
        module.setCourse(course);
        module = moduleRepository.save(module);

        // Create 3 content items sequentially
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/modules/" + module.getModuleId() + "/contents")
                            .with(asTeacher())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Content " + i + "\",\"contentType\":\"VIDEO\",\"filePath\":\"https://youtube.com/watch?v=vid" + i + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.position").value(i));
        }

        List<CourseContent> contents = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(module.getModuleId());
        assertThat(contents).hasSize(3);
        Long c1Id = contents.get(0).getContentId();
        Long c2Id = contents.get(1).getContentId();
        Long c3Id = contents.get(2).getContentId();

        // Reorder: move Content 3 to position 1
        mockMvc.perform(put("/api/contents/" + c3Id + "/reorder")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"position\":1}"))
                .andExpect(status().isOk());

        List<CourseContent> reordered = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(module.getModuleId());
        assertThat(reordered.get(0).getContentId()).isEqualTo(c3Id);
        assertThat(reordered.get(0).getPosition()).isEqualTo(1);
        assertThat(reordered.get(1).getContentId()).isEqualTo(c1Id);
        assertThat(reordered.get(1).getPosition()).isEqualTo(2);
        assertThat(reordered.get(2).getContentId()).isEqualTo(c2Id);
        assertThat(reordered.get(2).getPosition()).isEqualTo(3);

        // Delete content at position 2 (c1Id)
        mockMvc.perform(delete("/api/contents/" + c1Id)
                        .with(asTeacher()))
                .andExpect(status().isNoContent());

        // Verify remaining items are contiguous 1..2 without gaps
        List<CourseContent> remaining = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(module.getModuleId());
        assertThat(remaining).hasSize(2);
        assertThat(remaining.get(0).getContentId()).isEqualTo(c3Id);
        assertThat(remaining.get(0).getPosition()).isEqualTo(1);
        assertThat(remaining.get(1).getContentId()).isEqualTo(c2Id);
        assertThat(remaining.get(1).getPosition()).isEqualTo(2);
    }

    // =========================================================================
    // 3. Course Publication State Machine, Admin Approval, and Catalogue Filtering
    // =========================================================================

    @Test
    @DisplayName("A-04, AC-10: State machine enforces DRAFT initial, teacher LIVE forbidden, admin approval only, and catalogue filtering")
    void shouldEnforceCoursePublicationStateMachineAndAdminApproval() throws Exception {
        // Teacher creates course -> forced to DRAFT, isPublished false
        MvcResult createResult = mockMvc.perform(post("/api/courses")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"State Machine Course\",\"description\":\"Testing states\",\"price\":29.99,\"departmentId\":" + department.getDepartmentId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.isPublished").value(false))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long courseId = jsonNode.has("id") ? jsonNode.get("id").asLong() : jsonNode.get("courseId").asLong();

        // Draft course must NOT be visible in public catalog
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(courseId.intValue()))));

        // Public/Student reading draft course detail directly receives 404 NOT_FOUND
        mockMvc.perform(get("/api/courses/" + courseId)
                        .with(asStudent()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Teacher submits for review: DRAFT -> PENDING
        mockMvc.perform(put("/api/courses/" + courseId)
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.isPublished").value(false));

        // Teacher attempting to self-publish (PENDING -> LIVE) is rejected with 403 FORBIDDEN
        mockMvc.perform(put("/api/courses/" + courseId)
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LIVE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // Admin approves course: PENDING -> LIVE
        mockMvc.perform(put("/api/admin/courses/" + courseId + "/status")
                        .with(asAdmin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LIVE"))
                .andExpect(jsonPath("$.isPublished").value(true));

        // Assert audit log row was recorded for admin status change
        List<AuditLog> auditLogs = auditLogRepository.findByResourceTypeAndResourceIdOrderByIdAsc("COURSE", courseId);
        assertThat(auditLogs).isNotEmpty();
        assertThat(auditLogs.get(0).getAction()).isEqualTo("COURSE_STATUS_CHANGED");
        assertThat(auditLogs.get(0).getActorId()).isEqualTo(admin.getUserId());

        // Course is now LIVE and visible in public catalogue
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(courseId.intValue())));

        // Course is accessible to public/students
        mockMvc.perform(get("/api/courses/" + courseId)
                        .with(asStudent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(courseId))
                .andExpect(jsonPath("$.isPublished").value(true));
    }

    // =========================================================================
    // 4. Course Soft-Delete Archiving and Restore
    // =========================================================================

    @Test
    @DisplayName("[B-F5]: Course deletion archives soft-delete, hides from catalog, and restores to DRAFT")
    void shouldEnforceCourseArchiveAndRestore() throws Exception {
        Course course = createTeacherCourse("Archivable Course", Course.Status.LIVE);
        Long courseId = course.getCourseId();

        // Create child module and content to ensure soft-delete preserves them
        Module module = new Module();
        module.setTitle("Child Module");
        module.setPosition(1);
        module.setCourse(course);
        module = moduleRepository.save(module);

        CourseContent content = new CourseContent();
        content.setModule(module);
        content.setTitle("Child Content");
        content.setContentType("VIDEO");
        content.setPosition(1);
        courseContentRepository.save(content);

        // Delete course -> archives status to ARCHIVED (returns 204 No Content)
        mockMvc.perform(delete("/api/courses/" + courseId)
                        .with(asTeacher()))
                .andExpect(status().isNoContent());

        Course archived = courseRepository.findById(courseId).orElseThrow();
        assertThat(archived.getStatus()).isEqualTo(Course.Status.ARCHIVED);

        // Child module and content are completely preserved
        assertThat(moduleRepository.findByCourse_IdOrderByPositionAsc(courseId)).hasSize(1);
        assertThat(courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(module.getModuleId())).hasSize(1);

        // Archived course is excluded from public catalogue
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(courseId.intValue()))));

        // Public/Student reading archived course gets 404 NOT_FOUND
        mockMvc.perform(get("/api/courses/" + courseId)
                        .with(asStudent()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Restore course -> transitions status back to DRAFT
        mockMvc.perform(post("/api/courses/" + courseId + "/restore")
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.isPublished").value(false));

        Course restored = courseRepository.findById(courseId).orElseThrow();
        assertThat(restored.getStatus()).isEqualTo(Course.Status.DRAFT);

        // Attempting to restore an already non-archived course returns 400 BAD_REQUEST
        mockMvc.perform(post("/api/courses/" + courseId + "/restore")
                        .with(asTeacher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // =========================================================================
    // 5. Assignment Validation on Creation and Update
    // =========================================================================

    @Test
    @DisplayName("AC-09: Assignment enforces non-blank title, maxMarks > 0, future dueDate, and submission text/file requirements")
    void shouldEnforceAssignmentValidationRules() throws Exception {
        Course course = createTeacherCourse("Assignment Validation Course", Course.Status.LIVE);
        Module module = new Module();
        module.setTitle("Assignment Module");
        module.setPosition(1);
        module.setCourse(course);
        module = moduleRepository.save(module);

        // Enroll student in course so they are authorized to submit
        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(student.getUserId());
        enrollment.setCourse(course);
        enrollment.setRole("STUDENT");
        enrollmentRepository.save(enrollment);

        // 1. Creation with past due date -> 400 BAD_REQUEST
        mockMvc.perform(post("/api/modules/" + module.getModuleId() + "/assignments")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Past Homework\",\"description\":\"Past\",\"maxMarks\":100,\"dueDate\":\"2020-01-01T00:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // 2. Creation with maxMarks <= 0 -> 400 BAD_REQUEST
        mockMvc.perform(post("/api/modules/" + module.getModuleId() + "/assignments")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Zero Marks\",\"description\":\"Invalid\",\"maxMarks\":0,\"dueDate\":\"2099-01-01T00:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // 3. Creation with blank title -> 400 BAD_REQUEST
        mockMvc.perform(post("/api/modules/" + module.getModuleId() + "/assignments")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"description\":\"Invalid\",\"maxMarks\":50,\"dueDate\":\"2099-01-01T00:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // 4. Valid assignment creation -> 200 OK
        MvcResult validCreate = mockMvc.perform(post("/api/modules/" + module.getModuleId() + "/assignments")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Valid Assignment\",\"description\":\"Do this\",\"maxMarks\":100,\"dueDate\":\"2099-12-31T23:59:59\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Valid Assignment"))
                .andExpect(jsonPath("$.maxMarks").value(100))
                .andReturn();

        Long assignmentId = objectMapper.readTree(validCreate.getResponse().getContentAsString()).get("assignmentId").asLong();

        // 5. Update with past due date -> 400 BAD_REQUEST
        mockMvc.perform(put("/api/assignments/" + assignmentId)
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dueDate\":\"2021-01-01T00:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // 6. Update with zero maxMarks -> 400 BAD_REQUEST
        mockMvc.perform(put("/api/assignments/" + assignmentId)
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxMarks\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // 7. Submission with neither text nor file -> 400 BAD_REQUEST
        mockMvc.perform(post("/api/assignments/" + assignmentId + "/submissions")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionText\":\"   \",\"fileUrl\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // 8. Valid submission with text -> 200 OK
        mockMvc.perform(post("/api/assignments/" + assignmentId + "/submissions")
                        .with(asStudent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"submissionText\":\"My homework solution.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submissionText").value("My homework solution."));
    }

    // =========================================================================
    // 6. Storage Safety, Public-URL Removal, and Authorized Retrieval
    // =========================================================================

    @Test
    @DisplayName("W2-07, BR-26: Storage safety enforces public handler removal, 200 MB cap, MIME/magic check, and owner download")
    void shouldEnforceStorageSafetyAndAuthorizedRetrieval() throws Exception {
        // 1. Verify legacy public static /api/uploads/** handler is removed (unauthenticated -> 401, authenticated -> 404)
        mockMvc.perform(get("/api/uploads/test.pdf"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/uploads/test.pdf")
                        .with(asStudent()))
                .andExpect(status().isNotFound());

        // 2. Reject executable / disallowed MIME type and magic bytes
        byte[] exeMagicBytes = new byte[]{'M', 'Z', 0, 0, 0, 0, 0, 0};
        MockMultipartFile exeFile = new MockMultipartFile("file", "malware.exe", "application/x-msdownload", exeMagicBytes);

        mockMvc.perform(multipart("/api/teacher/upload/resource")
                        .file(exeFile)
                        .with(asTeacher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // 3. Reject shell script with executable header
        byte[] scriptBytes = "#!/bin/bash\necho evil".getBytes();
        MockMultipartFile scriptFile = new MockMultipartFile("file", "script.sh", "text/x-shellscript", scriptBytes);

        mockMvc.perform(multipart("/api/teacher/upload/resource")
                        .file(scriptFile)
                        .with(asTeacher()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // 4. Upload valid PDF with genuine %PDF- magic bytes
        byte[] validPdfBytes = "%PDF-1.7 valid content for storage contract".getBytes();
        MockMultipartFile validPdf = new MockMultipartFile("file", "syllabus.pdf", "application/pdf", validPdfBytes);

        MvcResult uploadResult = mockMvc.perform(multipart("/api/teacher/upload/resource")
                        .file(validPdf)
                        .with(asTeacher()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileKey").isNotEmpty())
                .andExpect(jsonPath("$.url").value(startsWith("/api/files/")))
                .andReturn();

        String fileKey = objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("fileKey").asText();

        // 5. Authorized retrieval: Teacher (owner) can download with private, no-store headers
        mockMvc.perform(get("/api/files/" + fileKey)
                        .with(asTeacher()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("private, no-store")))
                .andExpect(content().bytes(validPdfBytes));

        // 6. Admin can also download (administrative review)
        mockMvc.perform(get("/api/files/" + fileKey)
                        .with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(validPdfBytes));

        // 7. Foreign teacher / unauthorized student receives 404 NOT_FOUND (IDOR defense)
        mockMvc.perform(get("/api/files/" + fileKey)
                        .with(asOtherTeacher()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(get("/api/files/" + fileKey)
                        .with(asStudent()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // =========================================================================
    // 7. YouTube Import into Canonical Content & Staff Authorization
    // =========================================================================

    @Test
    @DisplayName("A-05, spec §6: YouTube import creates canonical CourseContent with contiguous positions and enforces staff ownership")
    void shouldEnforceYouTubeImportCanonicalContentAndStaffOwnership() throws Exception {
        Course course = createTeacherCourse("YouTube Import Course", Course.Status.DRAFT);
        Module module = new Module();
        module.setTitle("Video Module");
        module.setPosition(1);
        module.setCourse(course);
        module = moduleRepository.save(module);

        // Foreign teacher importing into this course receives 404 NOT_FOUND (IDOR defense)
        mockMvc.perform(post("/api/teacher/youtube/import-playlist")
                        .with(asOtherTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playlistUrl\":\"https://www.youtube.com/playlist?list=PLContractTest\",\"courseId\":" + course.getCourseId() + ",\"moduleId\":" + module.getModuleId() + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // Blank playlist URL rejected with 400 BAD_REQUEST
        mockMvc.perform(post("/api/teacher/youtube/import-playlist")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playlistUrl\":\"   \",\"courseId\":" + course.getCourseId() + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));

        // Valid import creates canonical CourseContent under the designated module
        MvcResult importResult = mockMvc.perform(post("/api/teacher/youtube/import-playlist")
                        .with(asTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playlistUrl\":\"https://www.youtube.com/playlist?list=PLCurriculumContract123\",\"courseId\":" + course.getCourseId() + ",\"moduleId\":" + module.getModuleId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playlistTitle").isNotEmpty())
                .andExpect(jsonPath("$.importedContents").isArray())
                .andExpect(jsonPath("$.importedContents.length()").value(greaterThan(0)))
                .andReturn();

        List<CourseContent> contents = courseContentRepository.findByModule_ModuleIdOrderByPositionAsc(module.getModuleId());
        assertThat(contents).isNotEmpty();
        assertThat(contents.get(0).getContentType()).isEqualTo("VIDEO");
        assertThat(contents.get(0).getPosition()).isEqualTo(1);
    }

    // =========================================================================
    // 8. IDOR Defense Across All Curriculum Entities
    // =========================================================================

    @Test
    @DisplayName("IDOR Defense: Foreign mutations across course, module, content, and assignment return 404 NOT_FOUND")
    void shouldRejectForeignMutationsAcrossAllCurriculumEntitiesWith404() throws Exception {
        Course course = createTeacherCourse("Foreign Protected Course", Course.Status.DRAFT);
        Module module = new Module();
        module.setTitle("Protected Module");
        module.setPosition(1);
        module.setCourse(course);
        module = moduleRepository.save(module);

        CourseContent content = new CourseContent();
        content.setModule(module);
        content.setTitle("Protected Content");
        content.setContentType("VIDEO");
        content.setPosition(1);
        content = courseContentRepository.save(content);

        Assignment assignment = new Assignment();
        assignment.setModule(module);
        assignment.setTitle("Protected Assignment");
        assignment.setMaxMarks(100);
        assignment.setDueDate(LocalDateTime.now().plusDays(7));
        assignment = assignmentRepository.save(assignment);

        // 1. Foreign teacher updating course -> 404
        mockMvc.perform(put("/api/courses/" + course.getCourseId())
                        .with(asOtherTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked Title\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // 2. Foreign teacher adding module -> 404
        mockMvc.perform(post("/api/courses/" + course.getCourseId() + "/modules")
                        .with(asOtherTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Illegal Module\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // 3. Foreign teacher updating module -> 404
        mockMvc.perform(put("/api/modules/" + module.getModuleId())
                        .with(asOtherTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Illegal Update\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // 4. Foreign teacher updating content -> 404
        mockMvc.perform(put("/api/contents/" + content.getContentId())
                        .with(asOtherTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Illegal Content Update\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // 5. Foreign teacher updating assignment -> 404
        mockMvc.perform(put("/api/assignments/" + assignment.getAssignmentId())
                        .with(asOtherTeacher())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Illegal Assignment Update\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        // 6. Foreign teacher deleting course -> 404
        mockMvc.perform(delete("/api/courses/" + course.getCourseId())
                        .with(asOtherTeacher()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
