package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.PayoutResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.PendingPayoutDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Payout;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PayoutRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.AuditService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Story W6-09: Payout threshold/schedule/lock/statement
 * Rules: A-22, BR-20, [F-F18], spec §4 SCR-55
 *
 * Admin-only payout actions. Enforces:
 * - ₹500 minimum payout threshold
 * - Pessimistic DB write lock on UserAccount to prevent double-pay races
 * - Idempotency-Key support
 * - Audit logging on disbursement
 * - Cache-Control: private, no-store
 */
@RestController
@RequestMapping("/api/admin/payouts")
public class PayoutAdminController {

    public static final BigDecimal MINIMUM_PAYOUT_THRESHOLD = new BigDecimal("500.00");
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserAccountRepository userAccountRepository;
    private final CourseRepository courseRepository;
    private final PayoutRepository payoutRepository;
    private final AuditService auditService;

    public PayoutAdminController(UserAccountRepository userAccountRepository,
                                  CourseRepository courseRepository,
                                  PayoutRepository payoutRepository,
                                  AuditService auditService) {
        this.userAccountRepository = userAccountRepository;
        this.courseRepository = courseRepository;
        this.payoutRepository = payoutRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<PendingPayoutDto>> getPendingPayouts() {
        List<PendingPayoutDto> pending = userAccountRepository.findTeachersWithPendingRoyalty().stream()
                .map(teacher -> {
                    BigDecimal balance = teacher.getRoyaltyBalance() != null ? teacher.getRoyaltyBalance() : BigDecimal.ZERO;
                    boolean eligible = balance.compareTo(MINIMUM_PAYOUT_THRESHOLD) >= 0;
                    return new PendingPayoutDto(
                            teacher.getUserId(),
                            teacher.getFullName(),
                            teacher.getAvatarUrl(),
                            balance,
                            courseRepository.countByTeacher_UserId(teacher.getUserId()),
                            eligible,
                            MINIMUM_PAYOUT_THRESHOLD
                    );
                })
                .toList();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(pending);
    }

    @PostMapping("/process-all")
    @Transactional
    public ResponseEntity<Map<String, Object>> processAll() {
        List<UserAccount> teachers = userAccountRepository.findTeachersWithPendingRoyalty();
        int processed = 0;
        int skipped = 0;

        for (UserAccount candidate : teachers) {
            Optional<UserAccount> lockedOpt = userAccountRepository.findByIdWithLock(candidate.getUserId());
            if (lockedOpt.isEmpty()) {
                continue;
            }
            UserAccount lockedTeacher = lockedOpt.get();
            BigDecimal balance = lockedTeacher.getRoyaltyBalance() != null ? lockedTeacher.getRoyaltyBalance() : BigDecimal.ZERO;

            if (balance.compareTo(MINIMUM_PAYOUT_THRESHOLD) < 0) {
                skipped++;
                continue;
            }

            executePayout(lockedTeacher, balance, null);
            processed++;
        }

        return ResponseEntity.ok(Map.of("processed", processed, "skipped", skipped));
    }

    @PostMapping("/{teacherId}/pay")
    @Transactional
    public ResponseEntity<PayoutResponseDto> payOne(
            @PathVariable Long teacherId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<Payout> existing = payoutRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                return ResponseEntity.ok(PayoutResponseDto.from(existing.get()));
            }
        }

        UserAccount teacher = userAccountRepository.findByIdWithLock(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));

        BigDecimal balance = teacher.getRoyaltyBalance() != null ? teacher.getRoyaltyBalance() : BigDecimal.ZERO;
        if (balance.compareTo(MINIMUM_PAYOUT_THRESHOLD) < 0) {
            throw new BadRequestException("Teacher royalty balance (₹" + balance + ") is below the minimum payout threshold of ₹500.00.");
        }

        Payout payout = executePayout(teacher, balance, idempotencyKey);
        return ResponseEntity.ok(PayoutResponseDto.from(payout));
    }

    private Payout executePayout(UserAccount teacher, BigDecimal amount, String idempotencyKey) {
        Payout payout = new Payout();
        payout.setTeacher(teacher);
        payout.setPeriod(LocalDateTime.now().format(PERIOD_FORMAT));
        payout.setAmount(amount);
        payout.setStatus(Payout.Status.PAID);
        payout.setTransferredAt(LocalDateTime.now());
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            payout.setIdempotencyKey(idempotencyKey.trim());
        }
        payout = payoutRepository.save(payout);

        teacher.setRoyaltyBalance(BigDecimal.ZERO);
        userAccountRepository.save(teacher);

        auditService.record(
                AuditService.PAYOUT_DISBURSED,
                AuditService.RESOURCE_PAYOUT,
                payout.getId(),
                Map.of("teacherId", teacher.getUserId(), "royaltyBalance", amount),
                Map.of("payoutId", payout.getId(), "amount", payout.getAmount(), "period", payout.getPeriod())
        );

        return payout;
    }
}
