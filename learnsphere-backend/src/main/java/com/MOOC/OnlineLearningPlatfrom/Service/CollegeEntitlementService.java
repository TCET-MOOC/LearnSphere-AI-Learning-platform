package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.CollegeEntitlementResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.CollegeEntitlementStatusDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.CollegeEntitlement;

import java.util.List;

public interface CollegeEntitlementService {

    CollegeEntitlementStatusDto checkEntitlement(Long studentId, Long courseId);

    CollegeEntitlement grantSameCollegeEntitlement(Long studentId, Long courseId);

    CollegeEntitlement grantAdminOverride(Long studentId, Long courseId, Long adminId, String reason);

    void revokeEntitlement(Long entitlementId, Long adminId, String reason);

    List<CollegeEntitlementResponseDto> getStudentEntitlements(Long studentId);

    List<CollegeEntitlementResponseDto> getCourseEntitlements(Long courseId);
}
