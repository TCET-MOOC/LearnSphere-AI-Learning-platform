package com.learnsphere.payments;

import com.learnsphere.payments.entity.Order;
import com.learnsphere.payments.entity.Payment;
import com.learnsphere.payments.repository.OrderRepository;
import com.learnsphere.payments.repository.PaymentRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Formatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class FailClosedVerificationAndIdempotencyTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("learnsphere_payments")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private String generateToken(Long userId) {
        var key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("user" + userId + "@learnsphere.test")
                .issuer("learnsphere")
                .audience().add("learnsphere-web").and()
                .claim("userId", userId)
                .claim("roles", List.of("STUDENT"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(key)
                .compact();
    }

    private String calculateSignature(String orderId, String paymentId, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] hash = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }

    @Test
    @DisplayName("createOrder fails closed without defaulting to 1000 INR when price cannot be fetched")
    void createOrderDoesNotDefaultTo1000INR() throws Exception {
        String token = generateToken(50L);

        // course 99999 has no backend running on localhost:8080 during this test
        mockMvc.perform(post("/api/payments/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\": 99999}"))
                .andExpect(status().is4xxClientError());

        // Ensure no order was saved with 100000 paise
        assertThat(orderRepository.findByUserId(50L)).isEmpty();
    }

    @Test
    @DisplayName("verifyPayment with invalid signature returns 400 and fails closed without marking order PAID")
    void verifyPaymentInvalidSignatureFailsClosed() throws Exception {
        Order order = new Order();
        order.setRazorpayOrderId("order_rzp_test_fail");
        order.setUserId(51L);
        order.setCourseId(101L);
        order.setAmount(49900);
        order.setCurrency("INR");
        order.setStatus(Order.OrderStatus.CREATED);
        orderRepository.save(order);

        String token = generateToken(51L);

        mockMvc.perform(post("/api/payments/orders/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"razorpay_order_id\": \"order_rzp_test_fail\","
                                + "\"razorpay_payment_id\": \"pay_forged_123\","
                                + "\"razorpay_signature\": \"forged_invalid_signature_hex\""
                                + "}"))
                .andExpect(status().isBadRequest());

        // Verify order remained CREATED and no payment row was stored
        Order persisted = orderRepository.findByRazorpayOrderId("order_rzp_test_fail").orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(Order.OrderStatus.CREATED);
        assertThat(paymentRepository.findByRazorpayPaymentId("pay_forged_123")).isEmpty();
    }

    @Test
    @DisplayName("verifyPayment with missing signature returns 400 Bad Request")
    void verifyPaymentMissingSignatureReturns400() throws Exception {
        Order order = new Order();
        order.setRazorpayOrderId("order_rzp_missing_sig");
        order.setUserId(52L);
        order.setCourseId(101L);
        order.setAmount(49900);
        order.setCurrency("INR");
        order.setStatus(Order.OrderStatus.CREATED);
        orderRepository.save(order);

        String token = generateToken(52L);

        mockMvc.perform(post("/api/payments/orders/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"razorpay_order_id\": \"order_rzp_missing_sig\","
                                + "\"razorpay_payment_id\": \"pay_test_missing_sig\""
                                + "}"))
                .andExpect(status().isBadRequest());

        Order persisted = orderRepository.findByRazorpayOrderId("order_rzp_missing_sig").orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(Order.OrderStatus.CREATED);
    }

    @Test
    @DisplayName("verifyPayment with valid signature succeeds and subsequent replay is idempotent")
    void verifyPaymentValidSignatureSucceedsAndReplayIsIdempotent() throws Exception {
        Order order = new Order();
        order.setRazorpayOrderId("order_rzp_valid_1");
        order.setUserId(53L);
        order.setCourseId(101L);
        order.setAmount(49900);
        order.setCurrency("INR");
        order.setStatus(Order.OrderStatus.CREATED);
        orderRepository.save(order);

        String paymentId = "pay_valid_123456";
        String validSignature = calculateSignature("order_rzp_valid_1", paymentId, razorpaySecret);
        String token = generateToken(53L);

        // First verification: success
        mockMvc.perform(post("/api/payments/orders/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"razorpay_order_id\": \"order_rzp_valid_1\","
                                + "\"razorpay_payment_id\": \"" + paymentId + "\","
                                + "\"razorpay_signature\": \"" + validSignature + "\""
                                + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        Order persisted = orderRepository.findByRazorpayOrderId("order_rzp_valid_1").orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(Order.OrderStatus.PAID);
        assertThat(paymentRepository.count()).isEqualTo(1);

        // Replay of same verify: idempotent 200, does not create duplicate payment
        mockMvc.perform(post("/api/payments/orders/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"razorpay_order_id\": \"order_rzp_valid_1\","
                                + "\"razorpay_payment_id\": \"" + paymentId + "\","
                                + "\"razorpay_signature\": \"" + validSignature + "\""
                                + "}"))
                .andExpect(status().isOk());

        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("verifyPayment rejects second capture on already-paid order with a different paymentId")
    void verifyPaymentRejectsDoubleCaptureWithDifferentPaymentId() throws Exception {
        Order order = new Order();
        order.setRazorpayOrderId("order_rzp_double_cap");
        order.setUserId(54L);
        order.setCourseId(101L);
        order.setAmount(49900);
        order.setCurrency("INR");
        order.setStatus(Order.OrderStatus.CREATED);
        orderRepository.save(order);

        String paymentId1 = "pay_first_111";
        String sig1 = calculateSignature("order_rzp_double_cap", paymentId1, razorpaySecret);
        String token = generateToken(54L);

        // First capture
        mockMvc.perform(post("/api/payments/orders/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"razorpay_order_id\": \"order_rzp_double_cap\","
                                + "\"razorpay_payment_id\": \"" + paymentId1 + "\","
                                + "\"razorpay_signature\": \"" + sig1 + "\""
                                + "}"))
                .andExpect(status().isOk());

        // Second capture with DIFFERENT payment ID on same order must be rejected (409 Conflict)
        String paymentId2 = "pay_second_222";
        String sig2 = calculateSignature("order_rzp_double_cap", paymentId2, razorpaySecret);

        mockMvc.perform(post("/api/payments/orders/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"razorpay_order_id\": \"order_rzp_double_cap\","
                                + "\"razorpay_payment_id\": \"" + paymentId2 + "\","
                                + "\"razorpay_signature\": \"" + sig2 + "\""
                                + "}"))
                .andExpect(status().isConflict());

        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("verifyPayment for foreign user's order returns 404 (IDOR defense)")
    void verifyPaymentForForeignOrderReturns404() throws Exception {
        Order order = new Order();
        order.setRazorpayOrderId("order_rzp_foreign");
        order.setUserId(999L);
        order.setCourseId(101L);
        order.setAmount(49900);
        order.setCurrency("INR");
        order.setStatus(Order.OrderStatus.CREATED);
        orderRepository.save(order);

        String token = generateToken(55L); // caller is user 55, order belongs to 999
        String paymentId = "pay_foreign_999";
        String sig = calculateSignature("order_rzp_foreign", paymentId, razorpaySecret);

        mockMvc.perform(post("/api/payments/orders/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"razorpay_order_id\": \"order_rzp_foreign\","
                                + "\"razorpay_payment_id\": \"" + paymentId + "\","
                                + "\"razorpay_signature\": \"" + sig + "\""
                                + "}"))
                .andExpect(status().isNotFound());
    }
}
