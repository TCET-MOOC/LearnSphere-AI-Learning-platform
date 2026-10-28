package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.CollegeEntitlement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollegeEntitlementResponseDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private Long courseId;
    private String courseTitle;
    private Long collegeId;
    private String collegeName;
    private String grantType;
    private LocalDateTime grantedAt;
    private Boolean revoked;
    private String reason;

    public static CollegeEntitlementResponseDto from(CollegeEntitlement entitlement) {
        if (entitlement == null) return null;
        return CollegeEntitlementResponseDto.builder()
                .id(entitlement.getId())
                .studentId(entitlement.getStudent() != null ? entitlement.getStudent().getUserId() : null)
                .studentName(entitlement.getStudent() != null ? entitlement.getStudent().getFullName() : null)
                .courseId(entitlement.getCourse() != null ? entitlement.getCourse().getId() : null)
                .courseTitle(entitlement.getCourse() != null ? entitlement.getCourse().getTitle() : null)
                .collegeId(entitlement.getCollege() != null ? entitlement.getCollege().getId() : null)
                .collegeName(entitlement.getCollege() != null ? entitlement.getCollege().getName() : null)
                .grantType(entitlement.getGrantType() != null ? entitlement.getGrantType().name() : null)
                .grantedAt(entitlement.getGrantedAt())
                .revoked(entitlement.getRevoked())
                .reason(entitlement.getReason())
                .build();
    }
}
