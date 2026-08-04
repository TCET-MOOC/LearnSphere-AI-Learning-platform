package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    List<TestAttempt> findByUserId(Long userId);
    List<TestAttempt> findByTest_Id(Long testId);
    List<TestAttempt> findByUserIdAndTest_Id(Long userId, Long testId);
    List<TestAttempt> findByUserIdAndStatus(Long userId, String status);
}