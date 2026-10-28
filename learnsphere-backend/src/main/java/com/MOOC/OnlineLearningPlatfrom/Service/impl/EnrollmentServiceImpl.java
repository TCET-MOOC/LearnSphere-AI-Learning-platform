package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseProgressCalculation;
import com.MOOC.OnlineLearningPlatfrom.Dto.CourseProgressSummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserProgressResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.CourseContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ConflictException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseContentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.LectureRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserProgressRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserProgressRepository userProgressRepository;
    private final CourseRepository courseRepository;
    private final CourseContentRepository courseContentRepository;
    private final LectureRepository lectureRepository;
    private final UserAccountRepository userAccountRepository;

    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepository,
                                 UserProgressRepository userProgressRepository,
                                 CourseRepository courseRepository,
                                 CourseContentRepository courseContentRepository,
                                 LectureRepository lectureRepository,
                                 UserAccountRepository userAccountRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userProgressRepository = userProgressRepository;
        this.courseRepository = courseRepository;
        this.courseContentRepository = courseContentRepository;
        this.lectureRepository = lectureRepository;
        this.userAccountRepository = userAccountRepository;
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
                if (content.getLegacyLectureId() != null && target.getLecture() == null) {
                    lectureRepository.findById(content.getLegacyLectureId()).ifPresent(target::setLecture);
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

        // AC-12 / A-08: completion at >= 90 %, un-complete on regression (< 90 %)
        if (target.getProgressPercent() != null && target.getProgressPercent().compareTo(BigDecimal.valueOf(90)) >= 0) {
            if (target.getCompletedAt() == null) {
                target.setCompletedAt(LocalDateTime.now());
            }
        } else {
            target.setCompletedAt(null);
        }
        return userProgressRepository.save(target);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProgressResponseDto> getStudentCourseProgress(Long courseId, Long userId) {
        List<CourseContent> contents = courseContentRepository.findByModule_Course_IdOrderByModule_PositionAscPositionAsc(courseId);
        List<com.MOOC.OnlineLearningPlatfrom.Entity.Lecture> legacyLectures = contents.isEmpty()
                ? lectureRepository.findByCourse_IdOrderByNumberAsc(courseId)
                : Collections.emptyList();

        List<UserProgress> studentProgress = userProgressRepository.findByUserIdAndCourseId(userId, courseId);
        Map<Long, UserProgress> byContentId = new HashMap<>();
        Map<Long, UserProgress> byLectureId = new HashMap<>();
        for (UserProgress p : studentProgress) {
            if (p.getContent() != null && p.getContent().getContentId() != null) {
                byContentId.put(p.getContent().getContentId(), p);
            }
            if (p.getLecture() != null && p.getLecture().getId() != null) {
                byLectureId.put(p.getLecture().getId(), p);
            }
        }

        List<UserProgressResponseDto> result = new ArrayList<>();
        if (!contents.isEmpty()) {
            for (CourseContent c : contents) {
                UserProgress p = byContentId.get(c.getContentId());
                if (p == null && c.getLegacyLectureId() != null) {
                    p = byLectureId.get(c.getLegacyLectureId());
                }
                if (p != null) {
                    result.add(UserProgressResponseDto.from(p));
                } else {
                    result.add(UserProgressResponseDto.emptyForContent(c.getContentId(), c.getLegacyLectureId()));
                }
            }
        } else {
            for (com.MOOC.OnlineLearningPlatfrom.Entity.Lecture l : legacyLectures) {
                UserProgress p = byLectureId.get(l.getId());
                if (p != null) {
                    result.add(UserProgressResponseDto.from(p));
                } else {
                    result.add(UserProgressResponseDto.empty(l.getId()));
                }
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgressCalculation calculateCourseProgress(Long courseId, Long userId) {
        List<CourseContent> contents = courseContentRepository.findByModule_Course_IdOrderByModule_PositionAscPositionAsc(courseId);
        List<com.MOOC.OnlineLearningPlatfrom.Entity.Lecture> legacyLectures = contents.isEmpty()
                ? lectureRepository.findByCourse_IdOrderByNumberAsc(courseId)
                : Collections.emptyList();
        long total = contents.isEmpty() ? legacyLectures.size() : contents.size();

        List<UserProgress> studentProgress = userProgressRepository.findByUserIdAndCourseId(userId, courseId);
        Map<Long, UserProgress> byContentId = new HashMap<>();
        Map<Long, UserProgress> byLectureId = new HashMap<>();
        for (UserProgress p : studentProgress) {
            if (p.getContent() != null && p.getContent().getContentId() != null) {
                byContentId.put(p.getContent().getContentId(), p);
            }
            if (p.getLecture() != null && p.getLecture().getId() != null) {
                byLectureId.put(p.getLecture().getId(), p);
            }
        }

        long completed = 0;
        if (!contents.isEmpty()) {
            for (CourseContent c : contents) {
                UserProgress p = byContentId.get(c.getContentId());
                if (p == null && c.getLegacyLectureId() != null) {
                    p = byLectureId.get(c.getLegacyLectureId());
                }
                if (isProgressComplete(p)) {
                    completed++;
                }
            }
        } else {
            for (com.MOOC.OnlineLearningPlatfrom.Entity.Lecture l : legacyLectures) {
                UserProgress p = byLectureId.get(l.getId());
                if (isProgressComplete(p)) {
                    completed++;
                }
            }
        }

        double percent = total > 0 ? Math.min(100.0, Math.round(((double) completed / total) * 100.0)) : 0.0;
        boolean isAllCompleted = total > 0 && completed >= total;
        LocalDateTime lastActivity = studentProgress.stream()
                .map(UserProgress::getCompletedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new CourseProgressCalculation(total, completed, percent, isAllCompleted, lastActivity);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseProgressSummaryDto getCourseProgressSummary(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        List<Enrollment> enrollments = enrollmentRepository.findByCourse_Id(courseId);
        long totalEnrolled = enrollments.size();

        List<CourseContent> contents = courseContentRepository.findByModule_Course_IdOrderByModule_PositionAscPositionAsc(courseId);
        List<com.MOOC.OnlineLearningPlatfrom.Entity.Lecture> legacyLectures = contents.isEmpty()
                ? lectureRepository.findByCourse_IdOrderByNumberAsc(courseId)
                : Collections.emptyList();
        long totalContents = contents.isEmpty() ? legacyLectures.size() : contents.size();

        List<UserProgress> allProgress = userProgressRepository.findByCourseId(courseId);
        Map<Long, List<UserProgress>> progressByUser = allProgress.stream()
                .collect(Collectors.groupingBy(UserProgress::getUserId));

        List<CourseProgressSummaryDto.StudentProgressItemDto> studentItems = new ArrayList<>();
        long completedCount = 0;
        long inProgressCount = 0;
        long notStartedCount = 0;
        BigDecimal totalPercentSum = BigDecimal.ZERO;

        for (Enrollment enrollment : enrollments) {
            Long studentId = enrollment.getUserId();
            UserAccount student = userAccountRepository.findById(studentId).orElse(null);

            List<UserProgress> userProgs = progressByUser.getOrDefault(studentId, Collections.emptyList());
            Map<Long, UserProgress> byContentId = new HashMap<>();
            Map<Long, UserProgress> byLectureId = new HashMap<>();
            for (UserProgress p : userProgs) {
                if (p.getContent() != null && p.getContent().getContentId() != null) {
                    byContentId.put(p.getContent().getContentId(), p);
                }
                if (p.getLecture() != null && p.getLecture().getId() != null) {
                    byLectureId.put(p.getLecture().getId(), p);
                }
            }

            long completedForStudent = 0;
            if (!contents.isEmpty()) {
                for (CourseContent c : contents) {
                    UserProgress p = byContentId.get(c.getContentId());
                    if (p == null && c.getLegacyLectureId() != null) {
                        p = byLectureId.get(c.getLegacyLectureId());
                    }
                    if (isProgressComplete(p)) {
                        completedForStudent++;
                    }
                }
            } else {
                for (com.MOOC.OnlineLearningPlatfrom.Entity.Lecture l : legacyLectures) {
                    UserProgress p = byLectureId.get(l.getId());
                    if (isProgressComplete(p)) {
                        completedForStudent++;
                    }
                }
            }

            BigDecimal studentPercent = totalContents > 0
                    ? BigDecimal.valueOf(completedForStudent).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(totalContents), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            boolean isCompleted = totalContents > 0 && completedForStudent >= totalContents;
            if (isCompleted) {
                completedCount++;
            } else if (completedForStudent > 0) {
                inProgressCount++;
            } else {
                notStartedCount++;
            }
            totalPercentSum = totalPercentSum.add(studentPercent);

            LocalDateTime lastActivity = userProgs.stream()
                    .map(UserProgress::getCompletedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            studentItems.add(new CourseProgressSummaryDto.StudentProgressItemDto(
                    studentId,
                    student != null ? student.getFullName() : "Unknown",
                    student != null ? student.getEmail() : null,
                    completedForStudent,
                    totalContents,
                    studentPercent,
                    isCompleted,
                    enrollment.getEnrolledAt(),
                    lastActivity
            ));
        }

        BigDecimal avgProgress = totalEnrolled > 0
                ? totalPercentSum.divide(BigDecimal.valueOf(totalEnrolled), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new CourseProgressSummaryDto(
                courseId,
                course.getTitle(),
                totalEnrolled,
                totalContents,
                completedCount,
                inProgressCount,
                notStartedCount,
                avgProgress,
                studentItems
        );
    }

    private boolean isProgressComplete(UserProgress p) {
        return p != null && (p.getCompletedAt() != null || (p.getProgressPercent() != null && p.getProgressPercent().compareTo(BigDecimal.valueOf(90)) >= 0));
    }
}

