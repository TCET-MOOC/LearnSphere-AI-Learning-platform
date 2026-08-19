package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
    Optional<Payment> findByUser_UserIdAndCourse_IdAndStatus(Long userId, Long courseId, Payment.Status status);
    boolean existsByUser_UserIdAndCourse_Id(Long userId, Long courseId);
    boolean existsByUser_UserIdAndCourse_IdAndStatus(Long userId, Long courseId, Payment.Status status);
    long countByCourse_IdAndStatus(Long courseId, Payment.Status status);
    List<Payment> findByCourse_IdAndStatus(Long courseId, Payment.Status status);

    /** Targeted query: fetch only SUCCESS payments with their course/user eagerly, avoiding full table scan. */
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.course c LEFT JOIN FETCH c.teacher WHERE p.status = :status AND p.amount IS NOT NULL")
    List<Payment> findByStatusWithRelations(Payment.Status status);

    /** Count-only query for total revenue calculation — no data transfer, minimal lock time. */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = :status AND p.amount IS NOT NULL")
    java.math.BigDecimal sumAmountByStatus(Payment.Status status);

    long countByStatus(Payment.Status status);
}

