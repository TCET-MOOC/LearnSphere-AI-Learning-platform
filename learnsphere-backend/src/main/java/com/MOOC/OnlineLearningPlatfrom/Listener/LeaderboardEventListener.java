package com.MOOC.OnlineLearningPlatfrom.Listener;

import com.MOOC.OnlineLearningPlatfrom.Event.TestAttemptCompletedEvent;
import com.MOOC.OnlineLearningPlatfrom.Service.LeaderboardService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LeaderboardEventListener {

    private final LeaderboardService leaderboardService;

    public LeaderboardEventListener(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @EventListener
    public void onTestAttemptCompleted(TestAttemptCompletedEvent event) {
        if (event.getCourseId() != null) {
            leaderboardService.recalculateCourseLeaderboard(event.getCourseId());
        }
    }
}
