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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Admin-only payout actions. Mounted at /api/admin/payouts so it inherits the ROLE_ADMIN gate
 * already configured in SecurityConfig for /api/admin/**.
 *
 * "Pending payouts" is computed live from UserAccount.royaltyBalance (see PendingPayoutDto) rather
 * than a stored Payout row — a Payout row is only ever created once a payout is actually processed
 * (status PAID), so there is no separate PENDING-Payout queue to keep in sync.
 */
@RestController
@RequestMapping("/api/admin/payouts")
public class PayoutAdminController {

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final UserAccountRepository userAccountRepository;
    private final CourseRepository courseRepository;
    private final PayoutRepository payoutRepository;

    public PayoutAdminController(UserAccountRepository userAccountRepository,
                                  CourseRepository courseRepository,
                                  PayoutRepository payoutRepository) {
        this.userAccountRepository = userAccountRepository;
        this.courseRepository = courseRepository;
        this.payoutRepository = payoutRepository;
    }

    @GetMapping
    public ResponseEntity<List<PendingPayoutDto>> getPendingPayouts() {
        List<PendingPayoutDto> pending = userAccountRepository.findTeachersWithPendingRoyalty().stream()
                .map(teacher -> new PendingPayoutDto(
                        teacher.getUserId(),
                        teacher.getFullName(),
                        teacher.getAvatarUrl(),
                        teacher.getRoyaltyBalance(),
                        courseRepository.countByTeacher_UserId(teacher.getUserId())
                ))
                .toList();
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/process-all")
    public ResponseEntity<Map<String, Object>> processAll() {
        List<UserAccount> teachers = userAccountRepository.findTeachersWithPendingRoyalty();
        int processed = 0;
        for (UserAccount teacher : teachers) {
            payTeacher(teacher);
            processed++;
        }
        return ResponseEntity.ok(Map.of("processed", processed));
    }

    @PostMapping("/{teacherId}/pay")
    public ResponseEntity<PayoutResponseDto> payOne(@PathVariable Long teacherId) {
        UserAccount teacher = userAccountRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with id: " + teacherId));
        if (teacher.getRoyaltyBalance() == null || teacher.getRoyaltyBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("This teacher has no pending royalty balance to pay out.");
        }
        return ResponseEntity.ok(PayoutResponseDto.from(payTeacher(teacher)));
    }

    private Payout payTeacher(UserAccount teacher) {
        BigDecimal amount = teacher.getRoyaltyBalance() != null ? teacher.getRoyaltyBalance() : BigDecimal.ZERO;

        Payout payout = new Payout();
        payout.setTeacher(teacher);
        payout.setPeriod(LocalDateTime.now().format(PERIOD_FORMAT));
        payout.setAmount(amount);
        payout.setStatus(Payout.Status.PAID);
        payout.setTransferredAt(LocalDateTime.now());
        payoutRepository.save(payout);

        teacher.setRoyaltyBalance(BigDecimal.ZERO);
        userAccountRepository.save(teacher);
        return payout;
    }
}
