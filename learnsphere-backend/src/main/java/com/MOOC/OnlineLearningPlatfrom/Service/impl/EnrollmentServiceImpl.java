package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ConflictException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LectureRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserProgressRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserProgressRepository userProgressRepository;
    private final CourseRepository courseRepository;
    private final CourseContentRepository courseContentRepository;
    private final LectureRepository lectureRepository;

    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepository,
                                 UserProgressRepository userProgressRepository,
                                 CourseRepository courseRepository,
                                 CourseContentRepository courseContentRepository,
                                 LectureRepository lectureRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userProgressRepository = userProgressRepository;
        this.courseRepository = courseRepository;
        this.courseContentRepository = courseContentRepository;
        this.lectureRepository = lectureRepository;
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
    @Transactional
    public UserProgress updateUserProgress(UserProgress userProgress) {
        if (userProgress.getUserId() == null) {
            throw new BadRequestException("User id is required for progress");
        }

        CourseContent content = null;
        if (userProgress.getContent() != null && userProgress.getContent().getContentId() != null) {
            content = courseContentRepository.findById(userProgress.getContent().getContentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Course content not found with id: " + userProgress.getContent().getContentId()));
        } else if (userProgress.getLecture() != null && userProgress.getLecture().getId() != null) {
            content = courseContentRepository.findByLegacyLectureId(userProgress.getLecture().getId()).orElse(null);
        }

        // Clamp progressPercent to [0.00, 100.00] with scale 2
        BigDecimal percent = userProgress.getProgressPercent();
        if (percent != null) {
            if (percent.compareTo(BigDecimal.ZERO) < 0) {
                percent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            } else if (percent.compareTo(BigDecimal.valueOf(100)) > 0) {
                percent = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
            } else {
                percent = percent.setScale(2, RoundingMode.HALF_UP);
            }
        }

        // Check for existing progress record for (userId, contentId) to perform upsert
        UserProgress target;
        if (content != null && content.getContentId() != null) {
            Optional<UserProgress> existingOpt = userProgressRepository
                    .findByUserIdAndContent_ContentId(userProgress.getUserId(), content.getContentId());
            if (existingOpt.isPresent()) {
                target = existingOpt.get();
                target.setProgressPercent(percent);
                if (userProgress.getSecondsWatched() != null) {
                    target.setSecondsWatched(userProgress.getSecondsWatched());
                }
            } else {
                target = userProgress;
                target.setContent(content);
                target.setProgressPercent(percent);
                if (content.getLegacyLectureId() != null && target.getLecture() == null) {
                    lectureRepository.findById(content.getLegacyLectureId()).ifPresent(target::setLecture);
                }
            }
        } else if (userProgress.getLecture() != null && userProgress.getLecture().getId() != null) {
            Optional<UserProgress> existingOpt = userProgressRepository
                    .findByUserIdAndLecture_Id(userProgress.getUserId(), userProgress.getLecture().getId());
            if (existingOpt.isPresent()) {
                target = existingOpt.get();
                target.setProgressPercent(percent);
                if (userProgress.getSecondsWatched() != null) {
                    target.setSecondsWatched(userProgress.getSecondsWatched());
                }
            } else {
                target = userProgress;
                target.setProgressPercent(percent);
            }
        } else {
            target = userProgress;
            target.setProgressPercent(percent);
        }

        if (target.getProgressPercent() != null && target.getProgressPercent().compareTo(BigDecimal.valueOf(100)) >= 0) {
            if (target.getCompletedAt() == null) {
                target.setCompletedAt(LocalDateTime.now());
            }
        }
        return userProgressRepository.save(target);
    }
}

