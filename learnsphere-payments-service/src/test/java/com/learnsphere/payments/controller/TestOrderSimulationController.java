package com.learnsphere.payments.controller;

import com.learnsphere.payments.entity.Order;
import com.learnsphere.payments.entity.Payment;
import com.learnsphere.payments.repository.OrderRepository;
import com.learnsphere.payments.repository.PaymentRepository;
import com.learnsphere.payments.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Hermetic test-profile fake for simulating successful payments.
 * NEVER compiled or bundled in production distributions.
 */
@RestController
@RequestMapping("/api/payments/orders")
@Profile("test")
@ConditionalOnProperty(name = "payments.simulation.enabled", havingValue = "true")
public class TestOrderSimulationController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;

    @Value("${main.backend.url}")
    private String mainBackendUrl;

    @Value("${internal.api.key:}")
    private String internalApiKey;

    public TestOrderSimulationController(OrderRepository orderRepository,
                                         PaymentRepository paymentRepository,
                                         RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/simulate-success")
    public ResponseEntity<?> simulateSuccess(@RequestBody Map<String, String> request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        String rzpOrderId = request.get("razorpay_order_id");
        if (rzpOrderId == null || rzpOrderId.isBlank()) {
            return ResponseEntity.badRequest().body("razorpay_order_id is required");
        }

        var orderOpt = orderRepository.findByRazorpayOrderId(rzpOrderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found: " + rzpOrderId);
        }

        Order order = orderOpt.get();
        if (!order.getUserId().equals(currentUserId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (order.getStatus() == Order.OrderStatus.PAID) {
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", "Payment already captured!"
            ));
        }

        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);

        String paymentId = "pay_sim_" + System.currentTimeMillis();
        Payment payment = new Payment();
        payment.setRazorpayPaymentId(paymentId);
        payment.setOrder(order);
        payment.setMethod("test_simulation");
        payment.setStatus("CAPTURED");
        payment.setCapturedAt(LocalDateTime.now());
        payment.setRawWebhookPayload("{\"simulated\": true}");
        paymentRepository.save(payment);

        try {
            String url = mainBackendUrl + "/internal/enroll";
            HttpHeaders headers = new HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }
            headers.set("X-Service-Id", "payments-service");
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "userId", order.getUserId(),
                    "courseId", order.getCourseId(),
                    "orderId", order.getRazorpayOrderId(),
                    "paymentId", paymentId,
                    "amount", ((double) order.getAmount()) / 100.0
            );

            org.springframework.http.HttpEntity<Map<String, Object>> req = new org.springframework.http.HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, req, String.class);
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Payment captured and student successfully enrolled!",
                "order_id", rzpOrderId,
                "payment_id", paymentId
        ));
    }
}
