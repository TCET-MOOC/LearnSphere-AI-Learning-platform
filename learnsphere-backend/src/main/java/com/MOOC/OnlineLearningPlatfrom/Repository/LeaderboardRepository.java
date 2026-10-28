package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {
    List<Leaderboard> findByCourse_IdOrderByGlobalRankAsc(Long courseId);
    List<Leaderboard> findByCourse_IdOrderByAvgScoreDesc(Long courseId);
    List<Leaderboard> findByUser_UserId(Long userId);
    Optional<Leaderboard> findByUser_UserIdAndCourse_Id(Long userId, Long courseId);
}
