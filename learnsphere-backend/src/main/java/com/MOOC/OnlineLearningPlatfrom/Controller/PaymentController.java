package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Dto.CheckoutRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.PaymentOrderDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.PaymentResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.PaymentStatusDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.VerifyPaymentRequestDto;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Student-facing (any authenticated role) checkout flow for paid courses. There is no real
 * payment gateway wired up (no Razorpay/Stripe keys available in this environment) — checkout
 * creates a simulated gateway order, and verify simulates the gateway's success callback.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<PaymentOrderDto> checkout(@RequestBody CheckoutRequestDto request,
                                                      @AuthenticationPrincipal CustomUserDetails principal) {
        if (request.getCourseId() == null) {
            throw new BadRequestException("courseId is required.");
        }
        return ResponseEntity.ok(paymentService.checkout(request.getCourseId(), principal));
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponseDto> verify(@RequestBody VerifyPaymentRequestDto request,
                                                       @AuthenticationPrincipal CustomUserDetails principal) {
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new BadRequestException("orderId is required.");
        }
        return ResponseEntity.ok(paymentService.verify(request.getOrderId(), principal));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PaymentResponseDto>> history(@AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(paymentService.getHistory(principal));
    }

    @GetMapping("/status")
    public ResponseEntity<PaymentStatusDto> status(@RequestParam Long courseId,
                                                     @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(new PaymentStatusDto(paymentService.hasPaid(courseId, principal)));
    }
}
