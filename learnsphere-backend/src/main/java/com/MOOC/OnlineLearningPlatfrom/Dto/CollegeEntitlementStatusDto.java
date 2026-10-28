package com.MOOC.OnlineLearningPlatfrom.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollegeEntitlementStatusDto {
    private boolean eligible;
    private String reason; // "SAME_COLLEGE", "ADMIN_OVERRIDE", "NONE"
    private String collegeName;
    private Long courseId;
}
