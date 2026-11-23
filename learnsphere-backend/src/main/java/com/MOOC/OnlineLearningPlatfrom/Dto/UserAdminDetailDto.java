package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.AuditLog;
import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Admin Inspect view of one account (W7-12, audit D5): the account row, its role
 * assignments, its most recent ledger payments and its most recent audit entries.
 * Built from explicit fields only, so no password, entity graph or client IP leaks.
 */
public record UserAdminDetailDto(
        UserAdminDto user,
        List<UserRoleDto> roles,
        List<PaymentEntry> payments,
        List<AuditEntry> audit) {

    public record PaymentEntry(
            Long id,
            Long courseId,
            String courseTitle,
            BigDecimal amount,
            String currency,
            String status,
            String gatewayPaymentId,
            LocalDateTime createdAt,
            LocalDateTime paidAt) {

        public static PaymentEntry from(Payment p) {
            return new PaymentEntry(
                    p.getId(),
                    p.getCourse() != null ? p.getCourse().getId() : null,
                    p.getCourse() != null ? p.getCourse().getTitle() : null,
                    p.getAmount(),
                    p.getCurrency(),
                    p.getStatus() != null ? p.getStatus().name() : null,
                    p.getGatewayPaymentId(),
                    p.getCreatedAt(),
                    p.getPaidAt());
        }
    }

    public record AuditEntry(
            Long id,
            Long actorId,
            String action,
            Map<String, Object> before,
            Map<String, Object> after,
            LocalDateTime createdAt) {

        public static AuditEntry from(AuditLog log) {
            return new AuditEntry(log.getId(), log.getActorId(), log.getAction(),
                    log.getBeforeJson(), log.getAfterJson(), log.getCreatedAt());
        }
    }
}
