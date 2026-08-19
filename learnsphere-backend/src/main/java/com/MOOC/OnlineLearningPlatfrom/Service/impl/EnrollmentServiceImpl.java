package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserProgressRepository;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserProgressRepository userProgressRepository;

    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepository,
                                  UserProgressRepository userProgressRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userProgressRepository = userProgressRepository;
    }

    @Override
    public Enrollment enrollUser(Enrollment enrollment) {
        enrollment.setEnrolledAt(LocalDateTime.now());
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
