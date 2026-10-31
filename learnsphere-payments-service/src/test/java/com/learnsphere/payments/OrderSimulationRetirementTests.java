package com.learnsphere.payments;

import com.learnsphere.payments.controller.OrderController;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public class OrderSimulationRetirementTests {

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

    @Value("${app.jwt.secret}")
    private String jwtSecret;

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

    @Test
    @DisplayName("Production OrderController must not expose simulateSuccess endpoint")
    void orderControllerDoesNotContainSimulateSuccessMethod() {
        Method[] methods = OrderController.class.getDeclaredMethods();
        boolean hasSimulateSuccess = Arrays.stream(methods)
                .anyMatch(m -> m.getName().equals("simulateSuccess"));
        assertThat(hasSimulateSuccess)
                .as("OrderController must not declare simulateSuccess endpoint in production")
                .isFalse();
    }

    @Test
    @DisplayName("simulate-success endpoint is retired or restricted to test fake only")
    void simulateSuccessPathIsProtectedAndNotOpen() throws Exception {
        // Calling unauthenticated simulate-success must never succeed (must be 401 or 404)
        mockMvc.perform(post("/api/payments/orders/simulate-success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"razorpay_order_id\": \"order_123\"}"))
                .andExpect(result -> {
                    int sc = result.getResponse().getStatus();
                    assertThat(sc).isIn(401, 404);
                });
    }
}
