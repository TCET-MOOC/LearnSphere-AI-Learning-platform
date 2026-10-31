package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.CheckoutRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.PaymentOrderDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.PaymentResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.PaymentStatusDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.VerifyPaymentRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.PaymentService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Payment endpoints on the main backend.
 * The simulated checkout and verify rails have been retired (A-06, AUD-06, W6-01).
 * Production course purchases and verification are exclusively handled by the Razorpay flow
 * in the payments service.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final com.MOOC.OnlineLearningPlatfrom.Service.CollegeEntitlementService collegeEntitlementService;

    public PaymentController(PaymentService paymentService,
                             com.MOOC.OnlineLearningPlatfrom.Service.CollegeEntitlementService collegeEntitlementService) {
        this.paymentService = paymentService;
        this.collegeEntitlementService = collegeEntitlementService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<PaymentOrderDto> checkout(@RequestBody(required = false) CheckoutRequestDto request,
                                                      @AuthenticationPrincipal CustomUserDetails principal) {
        if (request != null && request.getCourseId() != null && principal != null && principal.getUser() != null) {
            var entitlementStatus = collegeEntitlementService.checkEntitlement(principal.getUser().getUserId(), request.getCourseId());
            if (entitlementStatus != null && entitlementStatus.isEligible()) {
                throw new BadRequestException("This course is included by your college — use the direct enroll endpoint instead of checkout.");
            }
        }
        throw new BadRequestException("The simulated checkout rail has been retired. Please purchase courses via the Razorpay checkout flow.");
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponseDto> verify(@RequestBody(required = false) VerifyPaymentRequestDto request,
                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        throw new BadRequestException("The simulated checkout verify rail has been retired. Please verify payments via the payments service.");
    }

    @GetMapping("/history")
    public ResponseEntity<List<PaymentResponseDto>> history(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(paymentService.getHistory(principal));
    }

    @GetMapping("/status")
    public ResponseEntity<PaymentStatusDto> status(@RequestParam Long courseId,
                                                     @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(new PaymentStatusDto(paymentService.hasPaid(courseId, principal)));
    }
}
