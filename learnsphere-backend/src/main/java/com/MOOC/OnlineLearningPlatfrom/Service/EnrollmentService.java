package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseProgressCalculation;
import com.MOOC.OnlineLearningPlatfrom.Dto.CourseProgressSummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserProgressResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;

import java.util.List;

public interface EnrollmentService {
    Enrollment enrollUser(Enrollment enrollment);
    UserProgress updateUserProgress(UserProgress userProgress);
    CourseProgressSummaryDto getCourseProgressSummary(Long courseId);
    List<UserProgressResponseDto> getStudentCourseProgress(Long courseId, Long userId);
    CourseProgressCalculation calculateCourseProgress(Long courseId, Long userId);
    java.util.Map<Long, CourseProgressCalculation> calculateCoursesProgressBatch(List<Long> courseIds, Long userId);
    com.MOOC.OnlineLearningPlatfrom.Dto.ResumeTargetDto getResumeTarget(Long courseId, Long userId);
}
