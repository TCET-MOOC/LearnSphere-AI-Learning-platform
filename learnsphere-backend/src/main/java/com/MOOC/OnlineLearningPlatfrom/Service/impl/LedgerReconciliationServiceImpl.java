package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.LedgerReconciliationDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PaymentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoyaltyRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.LedgerReconciliationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class LedgerReconciliationServiceImpl implements LedgerReconciliationService {

    private final PaymentRepository paymentRepository;
    private final RoyaltyRepository royaltyRepository;
    private final EnrollmentRepository enrollmentRepository;

    public LedgerReconciliationServiceImpl(PaymentRepository paymentRepository,
                                           RoyaltyRepository royaltyRepository,
                                           EnrollmentRepository enrollmentRepository) {
        this.paymentRepository = paymentRepository;
        this.royaltyRepository = royaltyRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerReconciliationDto reconcileLedger() {
        long totalPayments = paymentRepository.count();

        long paidCount = paymentRepository.countByStatus(Payment.Status.SUCCESS);
        BigDecimal paidAmount = paymentRepository.sumAmountByStatus(Payment.Status.SUCCESS);
        if (paidAmount == null) paidAmount = BigDecimal.ZERO;

        long pendingCount = paymentRepository.countByStatus(Payment.Status.PENDING);
        BigDecimal pendingAmount = paymentRepository.sumAmountByStatus(Payment.Status.PENDING);
        if (pendingAmount == null) pendingAmount = BigDecimal.ZERO;

        long failedCount = paymentRepository.countByStatus(Payment.Status.FAILED);
        BigDecimal failedAmount = paymentRepository.sumAmountByStatus(Payment.Status.FAILED);
        if (failedAmount == null) failedAmount = BigDecimal.ZERO;

        long royaltiesCount = royaltyRepository.count();
        BigDecimal royaltiesAmount = royaltyRepository.sumTotalAmount();
        if (royaltiesAmount == null) royaltiesAmount = BigDecimal.ZERO;

        long paidEnrollmentsCount = enrollmentRepository.countPaidEnrollments();

        // Discrepancy metric: difference between successful paid enrollments and paid payments count
        long discrepancies = Math.abs(paidEnrollmentsCount - paidCount);

        return LedgerReconciliationDto.builder()
                .totalPayments(totalPayments)
                .paidPaymentsCount(paidCount)
                .paidPaymentsAmount(paidAmount)
                .pendingPaymentsCount(pendingCount)
                .pendingPaymentsAmount(pendingAmount)
                .failedPaymentsCount(failedCount)
                .failedPaymentsAmount(failedAmount)
                .totalRoyaltiesCount(royaltiesCount)
                .totalRoyaltiesAmount(royaltiesAmount)
                .paidEnrollmentsCount(paidEnrollmentsCount)
                .discrepanciesCount(discrepancies)
                .isReconciled(discrepancies == 0)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}

