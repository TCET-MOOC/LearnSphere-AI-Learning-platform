package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Entity.Assignment;
import com.MOOC.OnlineLearningPlatfrom.Entity.AssignmentSubmission;

import java.util.List;

public interface AssignmentService {
    List<Assignment> getAssignmentsByModuleId(Long moduleId);
    List<Assignment> getAssignmentsByCourseId(Long courseId);
    Assignment getAssignmentById(Long assignmentId);
    Assignment createAssignment(Long moduleId, Assignment assignment);
    Assignment updateAssignment(Long assignmentId, Assignment assignment);
    void deleteAssignment(Long assignmentId);
    List<AssignmentSubmission> getSubmissionsForAssignment(Long assignmentId);

    AssignmentSubmission submitAssignment(Long assignmentId, Long studentId, AssignmentSubmission submission);
    AssignmentSubmission getSubmissionById(Long submissionId);
    AssignmentSubmission gradeSubmission(Long submissionId, Integer marksAwarded, String feedback);
    List<AssignmentSubmission> getMySubmissions(Long studentId);
}

