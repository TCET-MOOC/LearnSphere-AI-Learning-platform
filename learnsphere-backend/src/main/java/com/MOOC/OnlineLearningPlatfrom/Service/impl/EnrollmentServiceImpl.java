package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ConflictException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserProgressRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserProgressRepository userProgressRepository;
    private final CourseRepository courseRepository;

    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepository,
                                 UserProgressRepository userProgressRepository,
                                 CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userProgressRepository = userProgressRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public Enrollment enrollUser(Enrollment enrollment) {
        if (enrollment.getCourse() == null || enrollment.getCourse().getId() == null) {
            throw new BadRequestException("Course id is required for enrollment");
        }

        Long courseId = enrollment.getCourse().getId();
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        if (course.getStatus() != Course.Status.LIVE) {
            throw new BadRequestException("Cannot enroll in course that is not published (current status: " + course.getStatus() + ")");
        }

        String role = (enrollment.getRole() != null && !enrollment.getRole().isBlank())
                ? enrollment.getRole().trim().toUpperCase()
                : "STUDENT";
        enrollment.setRole(role);

        if (enrollment.getUserId() != null && enrollmentRepository.existsByUserIdAndCourse_IdAndRole(enrollment.getUserId(), courseId, role)) {
            throw new ConflictException("User is already enrolled in course " + courseId + " with role: " + role);
        }

        enrollment.setCourse(course);
        if (enrollment.getEnrolledAt() == null) {
            enrollment.setEnrolledAt(LocalDateTime.now());
        }
        return enrollmentRepository.save(enrollment);
    }

    @Override
    public UserProgress updateUserProgress(UserProgress userProgress) {
        if (userProgress.getProgressPercent() != null && userProgress.getProgressPercent() >= 100) {
            userProgress.setCompletedAt(LocalDateTime.now());
        }
        return userProgressRepository.save(userProgress);
    }
}

