package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LectureResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Lecture;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.List;

public interface CourseService {
    List<CourseResponseDto> getCourses(String status, String department, Long teacherId);
    CourseResponseDto getCourseById(Long id);
    Course getCourseEntity(Long id);
    CourseResponseDto createCourse(Course course, CustomUserDetails principal);
    CourseResponseDto updateCourse(Long id, Course updates, CustomUserDetails principal);
    void deleteCourse(Long id, CustomUserDetails principal);

    List<LectureResponseDto> getLectures(Long courseId);
    LectureResponseDto getLectureById(Long lectureId);
    Lecture getLectureEntity(Long lectureId);
    LectureResponseDto addLecture(Long courseId, Lecture lecture, CustomUserDetails principal);
    List<LectureResponseDto> addLecturesBulk(Long courseId, List<Lecture> lectures, CustomUserDetails principal);
    LectureResponseDto updateLecture(Long lectureId, Lecture updates, CustomUserDetails principal);
    void deleteLecture(Long lectureId, CustomUserDetails principal);
}
