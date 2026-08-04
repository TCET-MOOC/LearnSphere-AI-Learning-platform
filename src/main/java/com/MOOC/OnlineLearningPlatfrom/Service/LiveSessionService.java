package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.LiveSessionResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.List;

public interface LiveSessionService {
    List<LiveSessionResponseDto> getMySessions(CustomUserDetails principal);
    LiveSessionResponseDto create(LiveSessionRequestDto request, CustomUserDetails principal);
    LiveSessionResponseDto start(Long id, CustomUserDetails principal);
    LiveSessionResponseDto end(Long id, CustomUserDetails principal);
}
