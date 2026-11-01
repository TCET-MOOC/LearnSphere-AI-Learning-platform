package com.MOOC.OnlineLearningPlatfrom;

import com.MOOC.OnlineLearningPlatfrom.Entity.*;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Post-W6 audit fix (ownership rule "enrolled"): a note or bookmark can only be attached to
 * content of a course the caller is enrolled in (or teaches). Otherwise a stealth 404 and
 * nothing is stored, so a student cannot read titles of courses they have no access to.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class NotesBookmarksEnrolmentGateTests extends PostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private EntityManager entityManager;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private CourseContentRepository courseContentRepository;
    @Autowired private LectureRepository lectureRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private NoteRepository noteRepository;
    @Autowired private BookmarkRepository bookmarkRepository;
    @Autowired private UserDetailsService userDetailsService;

    private UserAccount outsider;
    private CourseContent content;
    private Lecture lecture;

    @BeforeEach
    void setUp() {
        UserAccount teacher = userAccountRepository.findByEmail("teacher@learnsphere.com");
        UserAccount enrolled = userAccountRepository.findByEmail("student@learnsphere.com");
        outsider = userAccountRepository.findByEmail("external@learnsphere.com");

        Course course = new Course();
        course.setTitle("Members Only Course");
        course.setDescription("Notes need enrolment");
        course.setTeacher(teacher);
        course.setStatus(Course.Status.LIVE);
        course = courseRepository.save(course);

        Module module = new Module();
        module.setCourse(course);
        module.setTitle("Module 1");
        module.setPosition(1);
        module = moduleRepository.save(module);

        lecture = new Lecture();
        lecture.setCourse(course);
        lecture.setTitle("Members lecture");
        lecture.setNumber(1);
        lecture.setDuration(600);
        lecture = lectureRepository.save(lecture);

        content = new CourseContent();
        content.setModule(module);
        content.setTitle("Members lecture");
        content.setContentType("VIDEO");
        content.setPosition(1);
        content.setDurationSeconds(600);
        content.setLegacyLectureId(lecture.getId());
        content.setSourceType("UPLOAD");
        content = courseContentRepository.save(content);

        Enrollment e = new Enrollment();
        e.setUserId(enrolled.getUserId());
        e.setCourse(course);
        e.setRole("STUDENT");
        e.setEnrolledAt(LocalDateTime.now().minusDays(1));
        enrollmentRepository.save(e);
        entityManager.flush();
    }

    private RequestPostProcessor as(String email) {
        return user(userDetailsService.loadUserByUsername(email));
    }

    private String note(String target) {
        return "{" + target + ",\"title\":\"My note\",\"content\":\"text\",\"timestampSeconds\":10}";
    }

    @Test
    @DisplayName("An unenrolled student cannot attach a note to the course's content (404, nothing stored)")
    void outsiderNoteOnContent() throws Exception {
        long before = noteRepository.count();
        mockMvc.perform(post("/api/notes").with(as(outsider.getEmail())).contentType(MediaType.APPLICATION_JSON)
                        .content(note("\"contentId\":" + content.getContentId())))
                .andExpect(status().isNotFound());
        assertThat(noteRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("An unenrolled student cannot attach a note through a lecture id")
    void outsiderNoteOnLecture() throws Exception {
        mockMvc.perform(post("/api/notes").with(as(outsider.getEmail())).contentType(MediaType.APPLICATION_JSON)
                        .content(note("\"lectureId\":" + lecture.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("An unenrolled student cannot bookmark the course's content (404, nothing stored)")
    void outsiderBookmark() throws Exception {
        long before = bookmarkRepository.count();
        mockMvc.perform(post("/api/bookmarks").with(as(outsider.getEmail())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contentId\":" + content.getContentId() + ",\"timestampSeconds\":10,\"label\":\"x\"}"))
                .andExpect(status().isNotFound());
        assertThat(bookmarkRepository.count()).isEqualTo(before);
    }

    @Test
    @DisplayName("An enrolled student can still create notes and bookmarks")
    void enrolledStudentStillWorks() throws Exception {
        mockMvc.perform(post("/api/notes").with(as("student@learnsphere.com")).contentType(MediaType.APPLICATION_JSON)
                        .content(note("\"contentId\":" + content.getContentId())))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(post("/api/bookmarks").with(as("student@learnsphere.com")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lectureId\":" + lecture.getId() + ",\"timestampSeconds\":10,\"label\":\"x\"}"))
                .andExpect(status().is2xxSuccessful());
    }
}
