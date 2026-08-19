package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.AffiliationRequest;

public class AffiliationRequestResponseDto {
    private Long id;
    private String teacherName;
    private String avatarUrl;
    private String claimedDepartment;
    private String claimedCollegeName;
    private String idDocumentUrl;
    private String status;

    public static AffiliationRequestResponseDto from(AffiliationRequest req) {
        AffiliationRequestResponseDto dto = new AffiliationRequestResponseDto();
        dto.id = req.getId();
        dto.teacherName = req.getTeacher() != null ? req.getTeacher().getFullName() : null;
        dto.avatarUrl = req.getTeacher() != null ? req.getTeacher().getAvatarUrl() : null;
        dto.claimedDepartment = req.getClaimedDepartment();
        dto.claimedCollegeName = req.getClaimedCollege() != null ? req.getClaimedCollege().getName() : null;
        dto.idDocumentUrl = req.getIdDocumentUrl();
        dto.status = req.getStatus() != null ? req.getStatus().name() : null;
        return dto;
    }

    public Long getId() { return id; }
    public String getTeacherName() { return teacherName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getClaimedDepartment() { return claimedDepartment; }
    public String getClaimedCollegeName() { return claimedCollegeName; }
    public String getIdDocumentUrl() { return idDocumentUrl; }
    public String getStatus() { return status; }
}
