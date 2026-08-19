package com.learnsphere.payments.repository;

import com.learnsphere.payments.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
    boolean existsByRazorpayPaymentId(String razorpayPaymentId);
}
