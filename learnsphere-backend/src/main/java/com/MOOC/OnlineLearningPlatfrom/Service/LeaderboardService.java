package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.LeaderboardEntryDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import java.util.List;

public interface LeaderboardService {
    List<LeaderboardEntryDto> getLeaderboard(String scope, CustomUserDetails principal);
}
