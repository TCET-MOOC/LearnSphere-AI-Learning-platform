package com.learnsphere.payments.controller;

import com.learnsphere.payments.entity.Order;
import com.learnsphere.payments.entity.Payment;
import com.learnsphere.payments.repository.OrderRepository;
import com.learnsphere.payments.repository.PaymentRepository;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments/webhook")
public class WebhookController {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Value("${main.backend.url}")
    private String mainBackendUrl;

    public WebhookController(OrderRepository orderRepository, PaymentRepository paymentRepository, RestTemplate restTemplate) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        
        try {
            boolean isSignatureValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
            if (!isSignatureValid) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }

            JSONObject jsonPayload = new JSONObject(payload);
            String event = jsonPayload.getString("event");

            JSONObject paymentEntity = jsonPayload.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
            String rzpPaymentId = paymentEntity.getString("id");
            String rzpOrderId = paymentEntity.getString("order_id");

            if ("payment.captured".equals(event)) {
                // Idempotency check
                if (paymentRepository.existsByRazorpayPaymentId(rzpPaymentId)) {
                    return ResponseEntity.ok("Already processed");
                }

                Optional<Order> orderOpt = orderRepository.findByRazorpayOrderId(rzpOrderId);
                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    
                    Payment payment = new Payment();
                    payment.setRazorpayPaymentId(rzpPaymentId);
                    payment.setOrder(order);
                    payment.setMethod(paymentEntity.optString("method"));
                    payment.setStatus("CAPTURED");
                    payment.setCapturedAt(LocalDateTime.now());
                    payment.setRawWebhookPayload(payload);
                    
                    paymentRepository.save(payment);
                    
                    order.setStatus(Order.OrderStatus.PAID);
                    orderRepository.save(order);

                    // Call Main Backend
                    enrollStudent(order);
                }
            } else if ("payment.failed".equals(event)) {
                Optional<Order> orderOpt = orderRepository.findByRazorpayOrderId(rzpOrderId);
                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    order.setStatus(Order.OrderStatus.FAILED);
                    orderRepository.save(order);
                    
                    Payment payment = new Payment();
                    payment.setRazorpayPaymentId(rzpPaymentId);
                    payment.setOrder(order);
                    payment.setMethod(paymentEntity.optString("method"));
                    payment.setStatus("FAILED");
                    payment.setRawWebhookPayload(payload);
                    paymentRepository.save(payment);
                }
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }

    private void enrollStudent(Order order) {
        try {
            String url = mainBackendUrl + "/internal/enroll";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Key", internalApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = Map.of(
                    "userId", order.getUserId(),
                    "courseId", order.getCourseId(),
                    "orderId", order.getRazorpayOrderId()
            );
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                System.err.println("Failed to enroll student: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("Error calling main backend: " + e.getMessage());
        }
    }
}
