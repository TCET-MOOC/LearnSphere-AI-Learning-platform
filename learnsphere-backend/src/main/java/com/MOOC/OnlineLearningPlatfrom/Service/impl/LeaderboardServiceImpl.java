package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.LeaderboardEntryDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.LeaderboardService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private final UserAccountRepository userAccountRepository;

    public LeaderboardServiceImpl(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public List<LeaderboardEntryDto> getLeaderboard(String scope, CustomUserDetails principal) {
        String normalizedScope = scope == null ? "global" : scope.toLowerCase();

        List<UserAccount> students;
        if (normalizedScope.equals("college")) {
            UserAccount currentUser = principal.getUser();
            if (currentUser.getCollege() == null) {
                return List.of();
            }
            students = userAccountRepository.findCollegeStudentLeaderboard(currentUser.getCollege().getId());
        } else if (normalizedScope.equals("global")) {
            students = userAccountRepository.findGlobalStudentLeaderboard();
        } else {
            throw new BadRequestException("scope must be 'global' or 'college'");
        }

        List<LeaderboardEntryDto> result = new ArrayList<>();
        int rank = 1;
        for (UserAccount student : students) {
            int points = student.getLeaderboardPoints() != null ? student.getLeaderboardPoints() : 0;
            result.add(new LeaderboardEntryDto(rank++, student.getUserId(), student.getFullName(),
                    student.getAvatarUrl(), points));
        }
        return result;
    }
}
