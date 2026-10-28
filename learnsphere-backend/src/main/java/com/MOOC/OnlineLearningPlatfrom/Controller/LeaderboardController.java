package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.LeaderboardEntryDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Leaderboard;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.LeaderboardService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntryDto>> getLeaderboard(
            @RequestParam(defaultValue = "global") String scope,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(scope, principal));
    }

    @GetMapping("/courses/{courseId}/leaderboard")
    public ResponseEntity<List<Leaderboard>> getCourseLeaderboard(@PathVariable Long courseId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(leaderboardService.getCourseLeaderboard(courseId));
    }

    @GetMapping("/leaderboard/me")
    public ResponseEntity<List<Leaderboard>> getMyLeaderboard(@AuthenticationPrincipal CustomUserDetails principal) {
        if (principal == null || principal.getUser() == null || principal.getUser().getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Long userId = principal.getUser().getUserId();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(leaderboardService.getMyLeaderboards(userId));
    }
}

