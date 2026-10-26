package com.MOOC.OnlineLearningPlatfrom.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "assignment_submission", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"student_id", "assignment_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    @JsonIgnore
    private Assignment assignment;

    @JsonProperty("assignmentId")
    public Long getAssignmentId() {
        return assignment != null ? assignment.getAssignmentId() : null;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnore
    private UserAccount student;

    @JsonProperty("studentId")
    public Long getStudentId() {
        return student != null ? student.getUserId() : null;
    }

    @JsonProperty("studentName")
    public String getStudentName() {
        return student != null ? student.getFullName() : null;
    }

    @Column(name = "submission_text", columnDefinition = "TEXT")
    private String submissionText;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "marks_awarded")
    private Integer marksAwarded;

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (this.submittedAt == null) {
            this.submittedAt = LocalDateTime.now();
        }
    }
}
