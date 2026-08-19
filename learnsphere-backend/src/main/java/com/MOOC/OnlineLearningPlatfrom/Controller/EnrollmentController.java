package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserProgress;
import com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping("/enrollments")
    public ResponseEntity<Enrollment> enrollInCourse(@RequestBody Enrollment enrollment) {
        return ResponseEntity.ok(enrollmentService.enrollUser(enrollment));
    }

    @PostMapping("/progress")
    public ResponseEntity<UserProgress> logUserProgress(@RequestBody UserProgress userProgress) {
        return ResponseEntity.ok(enrollmentService.updateUserProgress(userProgress));
    }
}
