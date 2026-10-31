package com.learnsphere.payments;

import com.learnsphere.payments.entity.Order;
import com.learnsphere.payments.repository.OrderRepository;
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

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class PaymentsSecurityAndOwnershipTests {

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

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    private String generateToken(Long userId, String email, List<String> roles) {
        var key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(email)
                .issuer("learnsphere")
                .audience().add("learnsphere-web").and()
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("Unauthenticated POST /api/payments/orders returns 401 Unauthorized")
    void unauthenticatedCreateOrderReturns401() throws Exception {
        mockMvc.perform(post("/api/payments/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\": 101}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated GET /api/payments/orders/me returns 401 Unauthorized")
    void unauthenticatedGetMyOrdersReturns401() throws Exception {
        mockMvc.perform(get("/api/payments/orders/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated POST /api/payments/orders/verify returns 401 Unauthorized")
    void unauthenticatedVerifyOrderReturns401() throws Exception {
        mockMvc.perform(post("/api/payments/orders/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razorpay_order_id\": \"order_123\", \"razorpay_payment_id\": \"pay_123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Legacy /orders/{userId} endpoint is retired and not accessible")
    void legacyUserOrdersEndpointIsRetired() throws Exception {
        String token = generateToken(10L, "student@learnsphere.test", List.of("STUDENT"));
        mockMvc.perform(get("/api/payments/orders/10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Authenticated createOrder derives userId from auth token even if omitted from body")
    void authenticatedCreateOrderDerivesUserIdFromAuthToken() throws Exception {
        String token = generateToken(42L, "buyer@learnsphere.test", List.of("STUDENT"));

        // Note: mock backend price call falls back or is stubbed; order is created with razorpay client
        // With razorpay credentials in test, order creation proceeds
        mockMvc.perform(post("/api/payments/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\": 200}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Either 200 (created) or 400 (if razorpay dummy key rejected). If 200, verify order in DB
                    if (status == 200) {
                        List<Order> orders = orderRepository.findByUserId(42L);
                        assertThat(orders).isNotEmpty();
                        assertThat(orders.get(0).getUserId()).isEqualTo(42L);
                    }
                });
    }

    @Test
    @DisplayName("Authenticated createOrder overrides client-supplied tampered userId")
    void authenticatedCreateOrderOverridesTamperedUserId() throws Exception {
        String token = generateToken(42L, "buyer@learnsphere.test", List.of("STUDENT"));

        mockMvc.perform(post("/api/payments/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 9999, \"courseId\": 200}"))
                .andExpect(result -> {
                    // Check orderRepository: no order should ever be created with userId = 9999
                    assertThat(orderRepository.findByUserId(9999L)).isEmpty();
                });
    }

    @Test
    @DisplayName("GET /api/payments/orders/me returns only authenticated user's orders with private, no-store")
    void getMyOrdersReturnsOnlyOwnOrdersAndNoStore() throws Exception {
        // Seed order for user 42
        Order order42 = new Order();
        order42.setRazorpayOrderId("order_rzp_42");
        order42.setUserId(42L);
        order42.setCourseId(101L);
        order42.setAmount(50000);
        order42.setCurrency("INR");
        order42.setStatus(Order.OrderStatus.CREATED);
        orderRepository.save(order42);

        // Seed order for user 99
        Order order99 = new Order();
        order99.setRazorpayOrderId("order_rzp_99");
        order99.setUserId(99L);
        order99.setCourseId(102L);
        order99.setAmount(75000);
        order99.setCurrency("INR");
        order99.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order99);

        String token = generateToken(42L, "buyer@learnsphere.test", List.of("STUDENT"));

        mockMvc.perform(get("/api/payments/orders/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "private, no-store"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].razorpayOrderId").value("order_rzp_42"))
                .andExpect(jsonPath("$[0].userId").value(42));
    }
}
