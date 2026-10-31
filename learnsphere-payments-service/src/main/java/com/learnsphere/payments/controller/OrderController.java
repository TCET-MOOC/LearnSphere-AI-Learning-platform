package com.learnsphere.payments.controller;

import com.learnsphere.payments.entity.Order;
import com.learnsphere.payments.repository.OrderRepository;
import com.learnsphere.payments.repository.PaymentRepository;
import com.learnsphere.payments.security.SecurityUtils;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final RazorpayClient razorpayClient;
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;

    @Value("${main.backend.url}")
    private String mainBackendUrl;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    private final PaymentRepository paymentRepository;

    @Value("${internal.api.key:}")
    private String internalApiKey;

    public OrderController(RazorpayClient razorpayClient, OrderRepository orderRepository, PaymentRepository paymentRepository, RestTemplate restTemplate) {
        this.razorpayClient = razorpayClient;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.restTemplate = restTemplate;
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> request) {
        try {
            Long userId = SecurityUtils.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            Object courseIdObj = request.get("courseId");
            if (courseIdObj == null) {
                return ResponseEntity.badRequest().body("courseId is required");
            }
            Long courseId = Long.parseLong(courseIdObj.toString());

            // Fetch course price from main backend
            String courseUrl = mainBackendUrl + "/api/courses/" + courseId;
            HttpHeaders headers = new HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            Integer amountInPaise = null;
            try {
                ResponseEntity<Map> response = restTemplate.exchange(courseUrl, org.springframework.http.HttpMethod.GET, entity, Map.class);
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    Object priceObj = response.getBody().get("price");
                    if (priceObj != null) {
                        double priceInr = Double.parseDouble(priceObj.toString());
                        amountInPaise = (int) (priceInr * 100);
                    }
                }
            } catch (Exception fetchEx) {
                log.warn("Course price fetch via internal call failed: {}", fetchEx.getMessage());
            }

            // No price fallback: if course price cannot be resolved, fail closed
            if (amountInPaise == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "status", "ERROR",
                        "message", "Unable to determine course price. Please try again."
                ));
            }

            if (amountInPaise <= 0) {
                return ResponseEntity.badRequest().body("Cannot purchase free courses via payment gateway");
            }

            String rzpOrderId;
            try {
                // Create Real Razorpay Order via SDK on api.razorpay.com
                JSONObject orderRequest = new JSONObject();
                orderRequest.put("amount", amountInPaise);
                orderRequest.put("currency", "INR");
                orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

                com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
                rzpOrderId = razorpayOrder.get("id");
            } catch (Exception rzpEx) {
                log.error("Razorpay Orders API error: {}", rzpEx.getMessage());
                return ResponseEntity.status(400).body(Map.of(
                        "status", "ERROR",
                        "message", "Razorpay Error: " + rzpEx.getMessage() + ". Please verify RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET in .env"
                ));
            }

            // Save to DB
            Order order = new Order();
            order.setRazorpayOrderId(rzpOrderId);
            order.setUserId(userId);
            order.setCourseId(courseId);
            order.setAmount(amountInPaise);
            order.setCurrency("INR");
            order.setStatus(Order.OrderStatus.CREATED);
            orderRepository.save(order);

            // Return details to frontend
            return ResponseEntity.ok(Map.of(
                    "razorpay_order_id", rzpOrderId,
                    "amount", amountInPaise,
                    "currency", "INR",
                    "key_id", razorpayKeyId
            ));
        } catch (Exception e) {
            log.error("Error creating order", e);
            return ResponseEntity.internalServerError().body("Error creating order: " + e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        String rzpOrderId = request.get("razorpay_order_id");
        String rzpPaymentId = request.get("razorpay_payment_id");
        String rzpSignature = request.get("razorpay_signature");

        if (rzpOrderId == null || rzpPaymentId == null || rzpSignature == null || rzpSignature.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "razorpay_order_id, razorpay_payment_id, and razorpay_signature are required"
            ));
        }

        var orderOpt = orderRepository.findByRazorpayOrderId(rzpOrderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order not found: " + rzpOrderId);
        }

        Order order = orderOpt.get();

        // IDOR defense: caller must own this order
        if (!order.getUserId().equals(currentUserId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Idempotency check: if order is already PAID
        if (order.getStatus() == Order.OrderStatus.PAID) {
            if (paymentRepository.existsByRazorpayPaymentId(rzpPaymentId)) {
                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "message", "Payment already verified and enrollment confirmed!",
                        "order_id", rzpOrderId,
                        "payment_id", rzpPaymentId
                ));
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "status", "CONFLICT",
                        "message", "Order has already been paid and captured."
                ));
            }
        }

        // Fail-closed signature verification
        if (razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            log.error("Razorpay key secret not configured; failing closed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Payment gateway verification unavailable");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", rzpOrderId);
            options.put("razorpay_payment_id", rzpPaymentId);
            options.put("razorpay_signature", rzpSignature);
            boolean verified = com.razorpay.Utils.verifyPaymentSignature(options, razorpayKeySecret);
            if (!verified) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "ERROR",
                        "message", "Invalid signature"
                ));
            }
        } catch (Exception sigEx) {
            log.warn("Signature verification failed: {}", sigEx.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "ERROR",
                    "message", "Invalid signature: " + sigEx.getMessage()
            ));
        }

        if (paymentRepository.existsByRazorpayPaymentId(rzpPaymentId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "status", "CONFLICT",
                    "message", "Payment has already been processed."
            ));
        }

        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);

        com.learnsphere.payments.entity.Payment payment = new com.learnsphere.payments.entity.Payment();
        payment.setRazorpayPaymentId(rzpPaymentId);
        payment.setOrder(order);
        payment.setMethod("razorpay_gateway");
        payment.setStatus("CAPTURED");
        payment.setCapturedAt(java.time.LocalDateTime.now());
        payment.setRawWebhookPayload("{\"verified\": true, \"payment_id\": \"" + rzpPaymentId + "\"}");
        paymentRepository.save(payment);

        // Enroll Student in Main Backend
        try {
            String url = mainBackendUrl + "/internal/enroll";
            HttpHeaders headers = new HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "userId", order.getUserId(),
                    "courseId", order.getCourseId(),
                    "orderId", order.getRazorpayOrderId(),
                    "paymentId", rzpPaymentId,
                    "amount", ((double) order.getAmount()) / 100.0
            );

            org.springframework.http.HttpEntity<Map<String, Object>> req = new org.springframework.http.HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, req, String.class);
        } catch (Exception e) {
            log.warn("Enrollment trigger failed: {}", e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Payment verified and enrollment confirmed!",
                "order_id", rzpOrderId,
                "payment_id", rzpPaymentId
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<List<Order>> getMyOrders() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(orderRepository.findByUserId(currentUserId));
    }
}
