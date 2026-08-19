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

    @Query("SELECT u FROM UserAccount u JOIN UserRole ur ON ur.user.userId = u.userId WHERE ur.role.name = 'STUDENT' ORDER BY COALESCE(u.leaderboardPoints, 0) DESC")
    List<UserAccount> findGlobalStudentLeaderboard();

    @Query("SELECT u FROM UserAccount u JOIN UserRole ur ON ur.user.userId = u.userId WHERE ur.role.name = 'STUDENT' AND u.college.id = :collegeId ORDER BY COALESCE(u.leaderboardPoints, 0) DESC")
    List<UserAccount> findCollegeStudentLeaderboard(@Param("collegeId") Long collegeId);

    @Query("SELECT DISTINCT ur.user FROM UserRole ur WHERE ur.role.name = 'TEACHER' " +
            "AND ur.user.royaltyBalance > 0 ORDER BY ur.user.royaltyBalance DESC")
    List<UserAccount> findTeachersWithPendingRoyalty();
}
