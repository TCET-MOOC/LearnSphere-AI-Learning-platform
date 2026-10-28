package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "college_entitlements", uniqueConstraints = {
        @UniqueConstraint(name = "uq_college_entitlements_student_course", columnNames = {"student_id", "course_id"})
})
@Data
@NoArgsConstructor
public class CollegeEntitlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private UserAccount student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id")
    private College college;

    @Enumerated(EnumType.STRING)
    @Column(name = "grant_type", nullable = false)
    private GrantType grantType = GrantType.SAME_COLLEGE;

    @Column(name = "granted_at", nullable = false)
    private LocalDateTime grantedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    private UserAccount grantedBy;

    @Column(nullable = false)
    private Boolean revoked = false;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by")
    private UserAccount revokedBy;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (this.grantedAt == null) {
            this.grantedAt = LocalDateTime.now();
        }
        if (this.revoked == null) {
            this.revoked = false;
        }
    }

    public enum GrantType {
        SAME_COLLEGE,
        ADMIN_OVERRIDE
    }
}
