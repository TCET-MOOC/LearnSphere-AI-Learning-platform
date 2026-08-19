package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Certificate;
import java.time.LocalDateTime;

public class CertificateVerificationDto {
    private boolean valid;
    private String verificationCode;
    private String title;
    private String courseTitle;
    private String studentName;
    private String instructorName;
    private String type;
    private String grade;
    private Double score;
    private LocalDateTime issuedAt;
    private String message;

    public static CertificateVerificationDto valid(Certificate cert) {
        CertificateVerificationDto dto = new CertificateVerificationDto();
        dto.valid = true;
        dto.verificationCode = cert.getVerificationCode();
        dto.title = cert.getTitle();
        dto.courseTitle = cert.getCourse() != null ? cert.getCourse().getTitle() : "";
        dto.studentName = cert.getStudentName() != null ? cert.getStudentName() : 
                (cert.getUser() != null ? cert.getUser().getFullName() : "Learner");
        dto.instructorName = cert.getInstructorName() != null ? cert.getInstructorName() : 
                (cert.getCourse() != null && cert.getCourse().getTeacher() != null ? cert.getCourse().getTeacher().getFullName() : "Faculty Board");
        dto.type = cert.getType() != null ? cert.getType().name() : "STANDARD";
        dto.grade = cert.getGrade() != null ? cert.getGrade() : "Distinction";
        dto.score = cert.getScore();
        dto.issuedAt = cert.getIssuedAt();
        dto.message = "Official Certificate verified and recognized by LearnSphere AI.";
        return dto;
    }

    public static CertificateVerificationDto invalid(String code) {
        CertificateVerificationDto dto = new CertificateVerificationDto();
        dto.valid = false;
        dto.verificationCode = code;
        dto.message = "No authentic certificate record was found matching this code.";
        return dto;
    }

    public boolean isValid() { return valid; }
    public String getVerificationCode() { return verificationCode; }
    public String getTitle() { return title; }
    public String getCourseTitle() { return courseTitle; }
    public String getStudentName() { return studentName; }
    public String getInstructorName() { return instructorName; }
    public String getType() { return type; }
    public String getGrade() { return grade; }
    public Double getScore() { return score; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public String getMessage() { return message; }
}
