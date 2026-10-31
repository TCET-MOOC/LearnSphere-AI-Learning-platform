package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Royalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface RoyaltyRepository extends JpaRepository<Royalty, Long> {
    List<Royalty> findByTeacher_UserIdOrderByCreatedAtDesc(Long teacherId);
    List<Royalty> findByTeacher_UserIdAndCourse_Id(Long teacherId, Long courseId);
    List<Royalty> findByTeacher_UserIdAndPeriod(Long teacherId, String period);

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Royalty r WHERE r.amount IS NOT NULL")
    BigDecimal sumTotalAmount();
}
