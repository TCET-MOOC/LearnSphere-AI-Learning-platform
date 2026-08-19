package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {
    List<TestAttempt> findByUserId(Long userId);
    // Note: Test's PK field is named "testId" (not "id"), so the derived-query path must be
    // Test_TestId — findByTest_Id / findByUserIdAndTest_Id (as previously written here) fail to
    // resolve at startup with a PropertyReferenceException. Fixed as part of this workstream
    // since it was breaking the whole application context, not just the new payments/royalty code.
    List<TestAttempt> findByTest_TestId(Long testId);
    List<TestAttempt> findByUserIdAndTest_TestId(Long userId, Long testId);
    List<TestAttempt> findByUserIdAndStatus(Long userId, String status);
}