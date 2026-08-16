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

import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;

    public CourseServiceImpl(CourseRepository courseRepository, LectureRepository lectureRepository) {
        this.courseRepository = courseRepository;
        this.lectureRepository = lectureRepository;
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
    public void deleteCourse(Long id, CustomUserDetails principal) {
        Course course = getCourseEntity(id);
        assertOwner(course, principal);
        courseRepository.delete(course);
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
            lecture.setStatus(Lecture.Status.DRAFT);
        }
        Lecture saved = lectureRepository.save(lecture);
        return LectureResponseDto.from(saved);
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
        boolean isAdmin = principal.getRoles() != null && principal.getRoles().contains("ADMIN");
        if (isAdmin) {
            return;
        }
        if (course.getTeacher() == null || !course.getTeacher().getUserId().equals(principal.getUser().getUserId())) {
            throw new AccessDeniedException("You do not own this course");
        }
    }
}
