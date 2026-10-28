package com.MOOC.OnlineLearningPlatfrom.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRevokeEntitlementRequestDto {
    private String reason;
}
