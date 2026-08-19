package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.AffiliationRequestResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.CollegeResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.College;

import java.util.List;

public interface AdminService {
    List<CollegeResponseDto> getAllColleges();
    CollegeResponseDto createCollege(College college);
    void verifyCollege(Long collegeId);
    void rejectCollege(Long collegeId);

    List<AffiliationRequestResponseDto> getAffiliationRequests();
    void approveAffiliation(Long requestId);
    void rejectAffiliation(Long requestId);
}
