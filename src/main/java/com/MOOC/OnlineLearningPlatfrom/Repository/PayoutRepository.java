package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Payout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {
    List<Payout> findByTeacher_UserIdOrderByCreatedAtDesc(Long teacherId);
    List<Payout> findByStatusOrderByCreatedAtDesc(Payout.Status status);
}
