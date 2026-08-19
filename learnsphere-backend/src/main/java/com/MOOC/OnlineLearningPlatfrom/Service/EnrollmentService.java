package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;

public interface EnrollmentService {
    Enrollment enrollUser(Enrollment enrollment);
    UserProgress updateUserProgress(UserProgress userProgress);
}
