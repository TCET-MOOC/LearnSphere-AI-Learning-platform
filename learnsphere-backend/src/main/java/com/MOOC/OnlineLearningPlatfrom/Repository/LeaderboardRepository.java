package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT lb FROM Leaderboard lb JOIN FETCH lb.user JOIN FETCH lb.course WHERE lb.course.id = :courseId ORDER BY lb.globalRank ASC NULLS LAST, lb.avgScore DESC")
    List<Leaderboard> findByCourse_IdOrderByGlobalRankAsc(@org.springframework.data.repository.query.Param("courseId") Long courseId);

    @org.springframework.data.jpa.repository.Query("SELECT lb FROM Leaderboard lb JOIN FETCH lb.user JOIN FETCH lb.course WHERE lb.course.id = :courseId ORDER BY lb.avgScore DESC")
    List<Leaderboard> findByCourse_IdOrderByAvgScoreDesc(@org.springframework.data.repository.query.Param("courseId") Long courseId);

    @org.springframework.data.jpa.repository.Query("SELECT lb FROM Leaderboard lb JOIN FETCH lb.user JOIN FETCH lb.course WHERE lb.user.userId = :userId")
    List<Leaderboard> findByUser_UserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    Optional<Leaderboard> findByUser_UserIdAndCourse_Id(Long userId, Long courseId);
}
