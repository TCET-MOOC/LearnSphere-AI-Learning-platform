package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.Assignment;
import com.MOOC.OnlineLearningPlatfrom.Entity.AssignmentSubmission;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.AssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    // --- Assignment CRUD ---

    @GetMapping("/courses/{id}/assignments")
    public ResponseEntity<List<Assignment>> getAssignmentsByCourse(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByCourseId(id));
    }

    @GetMapping("/assignments/{id}")
    public ResponseEntity<Assignment> getAssignmentById(@PathVariable Long id) {

        return ResponseEntity.ok(assignmentService.getAssignmentById(id));
    }

    @PutMapping("/assignments/{id}")
    public ResponseEntity<Assignment> updateAssignment(@PathVariable Long id, @RequestBody Assignment assignment) {
        return ResponseEntity.ok(assignmentService.updateAssignment(id, assignment));
    }

    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }

    // --- Submissions ---

    @GetMapping("/assignments/{id}/submissions")
    public ResponseEntity<List<AssignmentSubmission>> getSubmissionsForAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getSubmissionsForAssignment(id));
    }

    @PostMapping("/assignments/{id}/submissions")
    public ResponseEntity<AssignmentSubmission> submitAssignment(
            @PathVariable Long id,
            @RequestBody AssignmentSubmission submission,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long studentId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : submission.getStudentId();
        return ResponseEntity.ok(assignmentService.submitAssignment(id, studentId, submission));
    }

    @GetMapping("/submissions/{id}")
    public ResponseEntity<AssignmentSubmission> getSubmissionById(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getSubmissionById(id));
    }

    @PatchMapping("/submissions/{id}/grade")
    public ResponseEntity<AssignmentSubmission> gradeSubmission(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Integer marksAwarded = body.get("marksAwarded") != null ? Integer.valueOf(body.get("marksAwarded").toString()) : null;
        String feedback = body.get("feedback") != null ? body.get("feedback").toString() : null;
        return ResponseEntity.ok(assignmentService.gradeSubmission(id, marksAwarded, feedback));
    }

    @GetMapping("/submissions/me")
    public ResponseEntity<List<AssignmentSubmission>> getMySubmissions(@AuthenticationPrincipal CustomUserDetails principal) {
        Long studentId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        return ResponseEntity.ok(assignmentService.getMySubmissions(studentId));
    }
}
