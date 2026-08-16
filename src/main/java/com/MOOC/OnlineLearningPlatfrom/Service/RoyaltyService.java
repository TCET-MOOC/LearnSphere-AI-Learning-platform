package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.PayoutResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.TeacherRoyaltiesResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.List;

public interface RoyaltyService {
    TeacherRoyaltiesResponseDto getRoyalties(CustomUserDetails principal);
    List<PayoutResponseDto> getPayouts(CustomUserDetails principal);
}
