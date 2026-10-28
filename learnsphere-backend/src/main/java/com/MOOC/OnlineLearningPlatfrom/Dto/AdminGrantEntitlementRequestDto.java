package com.MOOC.OnlineLearningPlatfrom.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminGrantEntitlementRequestDto {
    private Long studentId;
    private Long courseId;
    private String reason;
}
