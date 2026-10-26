package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    List<AssignmentSubmission> findByAssignment_AssignmentId(Long assignmentId);
    List<AssignmentSubmission> findByStudent_UserId(Long studentId);
    Optional<AssignmentSubmission> findByAssignment_AssignmentIdAndStudent_UserId(Long assignmentId, Long studentId);
}
