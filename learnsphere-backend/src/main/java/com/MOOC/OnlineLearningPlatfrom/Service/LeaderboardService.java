package com.MOOC.OnlineLearningPlatfrom.Service;

import com.MOOC.OnlineLearningPlatfrom.Dto.LeaderboardEntryDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;

import com.MOOC.OnlineLearningPlatfrom.Entity.Leaderboard;

import java.util.List;

public interface LeaderboardService {
    List<LeaderboardEntryDto> getLeaderboard(String scope, CustomUserDetails principal);
    List<Leaderboard> getCourseLeaderboard(Long courseId);
    List<Leaderboard> getMyLeaderboards(Long userId);
    void recalculateCourseLeaderboard(Long courseId);
}
