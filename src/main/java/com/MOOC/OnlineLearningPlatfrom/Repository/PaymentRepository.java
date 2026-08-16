package com.MOOC.OnlineLearningPlatfrom.Repository;

import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByUser_UserIdOrderByCreatedAtDesc(Long userId);
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
    Optional<Payment> findByUser_UserIdAndCourse_IdAndStatus(Long userId, Long courseId, Payment.Status status);
    long countByCourse_IdAndStatus(Long courseId, Payment.Status status);
    List<Payment> findByCourse_IdAndStatus(Long courseId, Payment.Status status);
}
