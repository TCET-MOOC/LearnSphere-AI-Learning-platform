package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.LeaderboardEntryDto;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.MOOC.OnlineLearningPlatfrom.Entity.Leaderboard;
import com.MOOC.OnlineLearningPlatfrom.Repository.LeaderboardRepository;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final LeaderboardRepository leaderboardRepository;

    public LeaderboardController(LeaderboardService leaderboardService,
                                 LeaderboardRepository leaderboardRepository) {
        this.leaderboardService = leaderboardService;
        this.leaderboardRepository = leaderboardRepository;
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @RequestParam(defaultValue = "global") String scope,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(scope, principal));
    }

    @GetMapping("/courses/{courseId}/leaderboard")
    public ResponseEntity<List<Leaderboard>> getCourseLeaderboard(@PathVariable Long courseId) {
        return ResponseEntity.ok(leaderboardRepository.findByCourse_IdOrderByAvgScoreDesc(courseId));
    }

    @GetMapping("/leaderboard/me")
    public ResponseEntity<List<Leaderboard>> getMyLeaderboard(@AuthenticationPrincipal CustomUserDetails principal) {
        Long userId = principal != null && principal.getUser() != null ? principal.getUser().getUserId() : 0L;
        return ResponseEntity.ok(leaderboardRepository.findByUser_UserId(userId));
    }
}

