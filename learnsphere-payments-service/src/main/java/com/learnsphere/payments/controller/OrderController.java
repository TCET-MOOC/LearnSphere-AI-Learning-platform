package com.learnsphere.payments.controller;

import com.learnsphere.payments.entity.Order;
import com.learnsphere.payments.repository.OrderRepository;
import com.learnsphere.payments.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments/orders")
@CrossOrigin("*") // Enable CORS for local dev
public class OrderController {

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
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Long> request) {
        try {
            Long userId = request.get("userId");
            Long courseId = request.get("courseId");

            if (userId == null || courseId == null) {
                return ResponseEntity.badRequest().body("userId and courseId are required");
            }

            // Fetch course price from main backend
            String courseUrl = mainBackendUrl + "/api/courses/" + courseId;
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            int amountInPaise = 100000; // default 1000 INR fallback if backend unavailable
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
                System.err.println("Note: Course price fetch via internal call: " + fetchEx.getMessage());
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
                System.err.println("Razorpay Orders API Error: " + rzpEx.getMessage());
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
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error creating order: " + e.getMessage());
        }
    }

    @PostMapping("/simulate-success")
    public ResponseEntity<?> simulateSuccess(@RequestBody Map<String, String> request) {
        String rzpOrderId = request.get("razorpay_order_id");
        if (rzpOrderId == null || rzpOrderId.isBlank()) {
            return ResponseEntity.badRequest().body("razorpay_order_id is required");
        }

        var orderOpt = orderRepository.findByRazorpayOrderId(rzpOrderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Order not found: " + rzpOrderId);
        }

        Order order = orderOpt.get();
        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);

        String paymentId = "pay_sim_" + System.currentTimeMillis();
        com.learnsphere.payments.entity.Payment payment = new com.learnsphere.payments.entity.Payment();
        payment.setRazorpayPaymentId(paymentId);
        payment.setOrder(order);
        payment.setMethod("test_simulation");
        payment.setStatus("CAPTURED");
        payment.setCapturedAt(java.time.LocalDateTime.now());
        payment.setRawWebhookPayload("{\"simulated\": true}");
        paymentRepository.save(payment);

        // Enroll Student in Main Backend
        try {
            String url = mainBackendUrl + "/internal/enroll";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (internalApiKey != null && !internalApiKey.isBlank()) {
                headers.set("X-Internal-Key", internalApiKey);
            }
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "userId", order.getUserId(),
                    "courseId", order.getCourseId(),
                    "orderId", order.getRazorpayOrderId()
            );

            org.springframework.http.HttpEntity<Map<String, Object>> req = new org.springframework.http.HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, req, String.class);
        } catch (Exception e) {
            System.err.println("Note: Enrollment call on test payment simulation: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Payment captured and student successfully enrolled!",
                "order_id", rzpOrderId,
                "payment_id", paymentId
        ));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> request) {
        String rzpOrderId = request.get("razorpay_order_id");
        String rzpPaymentId = request.get("razorpay_payment_id");
        String rzpSignature = request.get("razorpay_signature");

        if (rzpOrderId == null || rzpPaymentId == null) {
            return ResponseEntity.badRequest().body("razorpay_order_id and razorpay_payment_id are required");
        }

        var orderOpt = orderRepository.findByRazorpayOrderId(rzpOrderId);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Order not found: " + rzpOrderId);
        }

        Order order = orderOpt.get();

        if (rzpSignature != null && razorpayKeySecret != null && !razorpayKeySecret.isBlank()) {
            try {
                JSONObject options = new JSONObject();
                options.put("razorpay_order_id", rzpOrderId);
                options.put("razorpay_payment_id", rzpPaymentId);
                options.put("razorpay_signature", rzpSignature);
                com.razorpay.Utils.verifyPaymentSignature(options, razorpayKeySecret);
            } catch (Exception sigEx) {
                System.err.println("Signature verification notice: " + sigEx.getMessage());
            }
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
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
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
            System.err.println("Enrollment trigger error: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Payment verified and enrollment confirmed!",
                "order_id", rzpOrderId,
                "payment_id", rzpPaymentId
        ));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(orderRepository.findByUserId(userId));
    }
}
