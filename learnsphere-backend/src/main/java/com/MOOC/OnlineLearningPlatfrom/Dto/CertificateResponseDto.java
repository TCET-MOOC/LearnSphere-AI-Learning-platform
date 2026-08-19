package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Certificate;

import java.time.LocalDateTime;

public class CertificateResponseDto {
    private Long id;
    private Long courseId;
    private String courseTitle;
    private String type;
    private String title;
    private String verificationCode;
    private String studentName;
    private String instructorName;
    private String grade;
    private Double score;
    private LocalDateTime issuedAt;

    public static CertificateResponseDto from(Certificate certificate) {
        CertificateResponseDto dto = new CertificateResponseDto();
        dto.id = certificate.getId();
        if (certificate.getCourse() != null) {
            dto.courseId = certificate.getCourse().getId();
            dto.courseTitle = certificate.getCourse().getTitle();
            if (certificate.getCourse().getTeacher() != null) {
                dto.instructorName = certificate.getCourse().getTeacher().getFullName();
            }
        }
        if (certificate.getUser() != null) {
            dto.studentName = certificate.getUser().getFullName();
        }
        if (certificate.getStudentName() != null) {
            dto.studentName = certificate.getStudentName();
        }
        if (certificate.getInstructorName() != null) {
            dto.instructorName = certificate.getInstructorName();
        }
        dto.type = certificate.getType() != null ? certificate.getType().name() : null;
        dto.title = certificate.getTitle();
        dto.verificationCode = certificate.getVerificationCode();
        dto.grade = certificate.getGrade() != null ? certificate.getGrade() : "Distinction";
        dto.score = certificate.getScore();
        dto.issuedAt = certificate.getIssuedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getVerificationCode() { return verificationCode; }
    public String getStudentName() { return studentName; }
    public String getInstructorName() { return instructorName; }
    public String getGrade() { return grade; }
    public Double getScore() { return score; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
}
