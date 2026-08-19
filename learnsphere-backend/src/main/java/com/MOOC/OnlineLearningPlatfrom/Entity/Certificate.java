package com.MOOC.OnlineLearningPlatfrom.Entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificates")
@Data
@NoArgsConstructor
public class Certificate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String title;

    @Column(unique = true)
    private String verificationCode;

    private String studentName;
    private String instructorName;
    private String grade;
    private Double score;
    private String status = "ACTIVE";

    private LocalDateTime issuedAt;

    @PrePersist
    protected void onCreate() {
        this.issuedAt = LocalDateTime.now();
        if (this.verificationCode == null) {
            this.verificationCode = "LS-" + java.time.Year.now().getValue() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }
    }

    public enum Type {
        STANDARD, REMEDIAL
    }
}
