package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LectureResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LectureRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.CourseService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final JdbcTemplate jdbcTemplate;

    public CourseServiceImpl(CourseRepository courseRepository, LectureRepository lectureRepository, JdbcTemplate jdbcTemplate) {
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CourseResponseDto> getCourses(String status, String department, Long teacherId) {
        List<Course> courses;
        if (teacherId != null) {
            courses = courseRepository.findByTeacher_UserId(teacherId);
        } else if (status != null && department != null) {
            courses = courseRepository.findByStatusAndDepartment(Course.Status.valueOf(status.toUpperCase()), department);
        } else if (status != null) {
            courses = courseRepository.findByStatus(Course.Status.valueOf(status.toUpperCase()));
        } else if (department != null) {
            courses = courseRepository.findByDepartment(department);
        } else {
            courses = courseRepository.findAll();
        }
        return courses.stream()
                .map(c -> CourseResponseDto.from(c, lectureRepository.countByCourse_Id(c.getId())))
                .toList();
    }

    @Override
    public CourseResponseDto getCourseById(Long id) {
        Course course = getCourseEntity(id);
        return CourseResponseDto.from(course, lectureRepository.countByCourse_Id(id));
    }

    @Override
    public Course getCourseEntity(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    @Override
    public CourseResponseDto createCourse(Course course, CustomUserDetails principal) {
        course.setTeacher(principal.getUser());
        course.setStatus(Course.Status.DRAFT);
        Course saved = courseRepository.save(course);
        return CourseResponseDto.from(saved, 0);
    }

    @Override
    public CourseResponseDto updateCourse(Long id, Course updates, CustomUserDetails principal) {
        Course course = getCourseEntity(id);
        assertOwner(course, principal);

        if (updates.getTitle() != null) course.setTitle(updates.getTitle());
        if (updates.getDescription() != null) course.setDescription(updates.getDescription());
        if (updates.getDepartment() != null) course.setDepartment(updates.getDepartment());
        if (updates.getThumbnail() != null) course.setThumbnail(updates.getThumbnail());
        if (updates.getPrice() != null) course.setPrice(updates.getPrice());
        if (updates.getStatus() != null) course.setStatus(updates.getStatus());

        Course saved = courseRepository.save(course);
        return CourseResponseDto.from(saved, lectureRepository.countByCourse_Id(id));
    }

    @Override
    @Transactional
    public void deleteCourse(Long id, CustomUserDetails principal) {
        Course course = getCourseEntity(id);
        assertOwner(course, principal);

        // 1. Clear note_tags and notes
        jdbcTemplate.update("DELETE FROM note_tags WHERE note_id IN (SELECT id FROM notes WHERE course_id = ? OR lecture_id IN (SELECT id FROM lectures WHERE course_id = ?))", id, id);
        jdbcTemplate.update("DELETE FROM notes WHERE course_id = ? OR lecture_id IN (SELECT id FROM lectures WHERE course_id = ?)", id, id);

        // 2. Clear bookmarks (only has lecture_id)
        jdbcTemplate.update("DELETE FROM bookmarks WHERE lecture_id IN (SELECT id FROM lectures WHERE course_id = ?)", id);

        // 3. Clear self-referential foreign keys in discussion_posts (parent_post_id), then delete discussion_posts
        jdbcTemplate.update("UPDATE discussion_posts SET parent_post_id = NULL WHERE course_id = ? OR lecture_id IN (SELECT id FROM lectures WHERE course_id = ?)", id, id);
        jdbcTemplate.update("DELETE FROM discussion_posts WHERE course_id = ? OR lecture_id IN (SELECT id FROM lectures WHERE course_id = ?)", id, id);

        // 4. Clear messages in conversations linked to this course, then conversations
        jdbcTemplate.update("DELETE FROM messages WHERE conversation_id IN (SELECT id FROM conversations WHERE course_id = ?)", id);
        jdbcTemplate.update("DELETE FROM conversations WHERE course_id = ?", id);

        // 5. Clear quizzes: student_answers -> test_attempts -> question_options -> questions -> tests
        jdbcTemplate.update("DELETE FROM student_answers WHERE attempt_id IN (SELECT attempt_id FROM test_attempts WHERE test_id IN (SELECT test_id FROM tests WHERE course_id = ? OR lecture_id IN (SELECT id FROM lectures WHERE course_id = ?))) OR question_id IN (SELECT question_id FROM questions WHERE test_id IN (SELECT test_id FROM tests WHERE course_id = ? OR lecture_id IN (SELECT id FROM lectures WHERE course_id = ?)))", id, id, id, id);
        jdbcTemplate.update("DELETE FROM test_attempts WHERE test_id IN (SELECT test_id FROM tests WHERE course_id = ? OR lecture_id IN (SELECT id FROM lectures WHERE course_id = ?))", id, id);
        jdbcTemplate.update("DELETE FROM question_options WHERE question_id IN (SELECT question_id FROM questions WHERE test_id IN (SELECT test_id FROM tests WHERE course_id = ? OR lecture_id IN (SELECT id FROM lectures WHERE course_id = ?)))", id, id);
        jdbcTemplate.update("DELETE FROM questions WHERE test_id IN (SELECT test_id FROM tests WHERE course_id = ? OR lecture_id IN (SELECT id FROM lectures WHERE course_id = ?))", id, id);
        jdbcTemplate.update("DELETE FROM tests WHERE course_id = ? OR lecture_id IN (SELECT id FROM lectures WHERE course_id = ?)", id, id);

        // 6. Clear user progress (only has lecture_id)
        jdbcTemplate.update("DELETE FROM user_progress WHERE lecture_id IN (SELECT id FROM lectures WHERE course_id = ?)", id);

        // 7. Clear certificates, royalties, payments, live_sessions, enrollments
        jdbcTemplate.update("DELETE FROM certificates WHERE course_id = ?", id);
        jdbcTemplate.update("DELETE FROM royalties WHERE course_id = ?", id);
        jdbcTemplate.update("DELETE FROM payments WHERE course_id = ?", id);
        jdbcTemplate.update("DELETE FROM live_sessions WHERE course_id = ?", id);
        jdbcTemplate.update("DELETE FROM enrollments WHERE course_id = ?", id);

        // 8. Delete lectures
        jdbcTemplate.update("DELETE FROM lectures WHERE course_id = ?", id);

        // 9. Finally delete the course
        jdbcTemplate.update("DELETE FROM courses WHERE id = ?", id);
    }

    @Override
    public List<LectureResponseDto> getLectures(Long courseId) {
        // ensure course exists
        getCourseEntity(courseId);
        return lectureRepository.findByCourse_IdOrderByNumberAsc(courseId).stream()
                .map(LectureResponseDto::from)
                .toList();
    }

    @Override
    public LectureResponseDto getLectureById(Long lectureId) {
        return LectureResponseDto.from(getLectureEntity(lectureId));
    }

    @Override
    public Lecture getLectureEntity(Long lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ResourceNotFoundException("Lecture not found with id: " + lectureId));
    }

    @Override
    public LectureResponseDto addLecture(Long courseId, Lecture lecture, CustomUserDetails principal) {
        Course course = getCourseEntity(courseId);
        assertOwner(course, principal);
        lecture.setCourse(course);
        if (lecture.getStatus() == null) {
            lecture.setStatus(Lecture.Status.PUBLISHED);
        }
        Lecture saved = lectureRepository.save(lecture);
        return LectureResponseDto.from(saved);
    }

    @Override
    public List<LectureResponseDto> addLecturesBulk(Long courseId, List<Lecture> lectures, CustomUserDetails principal) {
        Course course = getCourseEntity(courseId);
        assertOwner(course, principal);
        long currentCount = lectureRepository.countByCourse_Id(courseId);
        int startIndex = (int) currentCount + 1;

        for (int i = 0; i < lectures.size(); i++) {
            Lecture l = lectures.get(i);
            l.setCourse(course);
            if (l.getNumber() == null) {
                l.setNumber(startIndex + i);
            }
            if (l.getStatus() == null) {
                l.setStatus(Lecture.Status.PUBLISHED);
            }
        }
        List<Lecture> saved = lectureRepository.saveAll(lectures);
        return saved.stream().map(LectureResponseDto::from).toList();
    }

    @Override
    public LectureResponseDto updateLecture(Long lectureId, Lecture updates, CustomUserDetails principal) {
        Lecture lecture = getLectureEntity(lectureId);
        assertOwner(lecture.getCourse(), principal);

        if (updates.getTitle() != null) lecture.setTitle(updates.getTitle());
        if (updates.getNumber() != null) lecture.setNumber(updates.getNumber());
        if (updates.getVideoUrl() != null) lecture.setVideoUrl(updates.getVideoUrl());
        if (updates.getDuration() != null) lecture.setDuration(updates.getDuration());
        if (updates.getIsDownloadable() != null) lecture.setIsDownloadable(updates.getIsDownloadable());
        if (updates.getStatus() != null) lecture.setStatus(updates.getStatus());

        Lecture saved = lectureRepository.save(lecture);
        return LectureResponseDto.from(saved);
    }

    @Override
    public void deleteLecture(Long lectureId, CustomUserDetails principal) {
        Lecture lecture = getLectureEntity(lectureId);
        assertOwner(lecture.getCourse(), principal);
        lectureRepository.delete(lecture);
    }

    private void assertOwner(Course course, CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null) {
            return;
        }
        boolean isAdmin = principal.getRoles() != null && 
                (principal.getRoles().contains("ADMIN") || principal.getRoles().contains("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }
        if (course.getTeacher() == null) {
            course.setTeacher(principal.getUser());
            courseRepository.save(course);
            return;
        }
        if (course.getTeacher().getUserId() != null && principal.getUser().getUserId() != null) {
            if (course.getTeacher().getUserId().equals(principal.getUser().getUserId())) {
                return;
            }
        }
        if (course.getTeacher().getEmail() != null && principal.getUsername() != null) {
            if (course.getTeacher().getEmail().equalsIgnoreCase(principal.getUsername())) {
                return;
            }
        }
        throw new AccessDeniedException("You do not own this course");
    }
}
