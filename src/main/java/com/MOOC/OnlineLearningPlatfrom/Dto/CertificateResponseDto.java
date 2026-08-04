package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.Certificate;

import java.time.LocalDateTime;

public class CertificateResponseDto {
    private Long id;
    private Long courseId;
    private String courseTitle;
    private String type;
    private String title;
    private LocalDateTime issuedAt;

    public static CertificateResponseDto from(Certificate certificate) {
        CertificateResponseDto dto = new CertificateResponseDto();
        dto.id = certificate.getId();
        if (certificate.getCourse() != null) {
            dto.courseId = certificate.getCourse().getId();
            dto.courseTitle = certificate.getCourse().getTitle();
        }
        dto.type = certificate.getType() != null ? certificate.getType().name() : null;
        dto.title = certificate.getTitle();
        dto.issuedAt = certificate.getIssuedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getCourseId() { return courseId; }
    public String getCourseTitle() { return courseTitle; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
}
