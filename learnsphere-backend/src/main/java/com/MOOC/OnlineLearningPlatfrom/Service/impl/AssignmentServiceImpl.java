package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Entity.Assignment;
import com.MOOC.OnlineLearningPlatfrom.Entity.AssignmentSubmission;
import com.MOOC.OnlineLearningPlatfrom.Entity.Module;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.AssignmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.AssignmentSubmissionRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.ModuleRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.AssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final ModuleRepository moduleRepository;
    private final UserAccountRepository userAccountRepository;

    public AssignmentServiceImpl(AssignmentRepository assignmentRepository,
                                 AssignmentSubmissionRepository assignmentSubmissionRepository,
                                 ModuleRepository moduleRepository,
                                 UserAccountRepository userAccountRepository) {
        this.assignmentRepository = assignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.moduleRepository = moduleRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> getAssignmentsByModuleId(Long moduleId) {
        return assignmentRepository.findByModule_ModuleId(moduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> getAssignmentsByCourseId(Long courseId) {
        return assignmentRepository.findByModule_Course_Id(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Assignment getAssignmentById(Long assignmentId) {
        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + assignmentId));
    }


    @Override
    public Assignment createAssignment(Long moduleId, Assignment assignment) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));

        if (assignment.getTitle() == null || assignment.getTitle().isBlank()) {
            throw new BadRequestException("Assignment title is required");
        }
        if (assignment.getMaxMarks() == null || assignment.getMaxMarks() <= 0) {
            throw new BadRequestException("maxMarks must be greater than 0");
        }
        if (assignment.getDueDate() != null && assignment.getDueDate().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("dueDate must be in the future");
        }

        assignment.setModule(module);
        return assignmentRepository.save(assignment);
    }

    @Override
    public Assignment updateAssignment(Long assignmentId, Assignment updated) {
        Assignment existing = getAssignmentById(assignmentId);

        if (updated.getTitle() != null) {
            if (updated.getTitle().isBlank()) {
                throw new BadRequestException("Assignment title cannot be blank");
            }
            existing.setTitle(updated.getTitle());
        }
        if (updated.getDescription() != null) {
            existing.setDescription(updated.getDescription());
        }
        if (updated.getMaxMarks() != null) {
            if (updated.getMaxMarks() <= 0) {
                throw new BadRequestException("maxMarks must be greater than 0");
            }
            existing.setMaxMarks(updated.getMaxMarks());
        }
        if (updated.getDueDate() != null) {
            if (updated.getDueDate().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("dueDate must be in the future");
            }
            existing.setDueDate(updated.getDueDate());
        }

        return assignmentRepository.save(existing);
    }

    @Override
    public void deleteAssignment(Long assignmentId) {
        Assignment existing = getAssignmentById(assignmentId);
        assignmentRepository.delete(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmission> getSubmissionsForAssignment(Long assignmentId) {
        // verify assignment exists
        getAssignmentById(assignmentId);
        return assignmentSubmissionRepository.findByAssignment_AssignmentId(assignmentId);
    }

    @Override
    public AssignmentSubmission submitAssignment(Long assignmentId, Long studentId, AssignmentSubmission submission) {
        Assignment assignment = getAssignmentById(assignmentId);
        UserAccount student = userAccountRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + studentId));

        if ((submission.getSubmissionText() == null || submission.getSubmissionText().isBlank()) &&
                (submission.getFileUrl() == null || submission.getFileUrl().isBlank())) {
            throw new BadRequestException("At least one of submissionText or fileUrl must be provided");
        }

        Optional<AssignmentSubmission> existingOpt =
                assignmentSubmissionRepository.findByAssignment_AssignmentIdAndStudent_UserId(assignmentId, studentId);

        AssignmentSubmission target = existingOpt.orElseGet(AssignmentSubmission::new);
        target.setAssignment(assignment);
        target.setStudent(student);
        if (submission.getSubmissionText() != null) {
            target.setSubmissionText(submission.getSubmissionText());
        }
        if (submission.getFileUrl() != null) {
            target.setFileUrl(submission.getFileUrl());
        }

        return assignmentSubmissionRepository.save(target);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentSubmission getSubmissionById(Long submissionId) {
        return assignmentSubmissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));
    }

    @Override
    public AssignmentSubmission gradeSubmission(Long submissionId, Integer marksAwarded, String feedback) {
        AssignmentSubmission submission = getSubmissionById(submissionId);
        if (marksAwarded != null) {
            if (marksAwarded < 0 || marksAwarded > submission.getAssignment().getMaxMarks()) {
                throw new BadRequestException("marksAwarded must be between 0 and maxMarks (" +
                        submission.getAssignment().getMaxMarks() + ")");
            }
            submission.setMarksAwarded(marksAwarded);
        }
        if (feedback != null) {
            submission.setFeedback(feedback);
        }
        return assignmentSubmissionRepository.save(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmission> getMySubmissions(Long studentId) {
        return assignmentSubmissionRepository.findByStudent_UserId(studentId);
    }
}
