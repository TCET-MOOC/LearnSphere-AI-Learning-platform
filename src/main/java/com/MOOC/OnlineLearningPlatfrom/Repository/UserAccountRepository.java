package com.MOOC.OnlineLearningPlatfrom.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;

import java.util.List;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    UserAccount findByEmail(String email);

    @Query("SELECT DISTINCT ur.user FROM UserRole ur WHERE ur.role.name = 'STUDENT' " +
            "AND (:collegeId IS NULL OR ur.user.college.id = :collegeId) " +
            "ORDER BY ur.user.leaderboardPoints DESC")
    List<UserAccount> findStudentLeaderboard(@Param("collegeId") Long collegeId);
}
