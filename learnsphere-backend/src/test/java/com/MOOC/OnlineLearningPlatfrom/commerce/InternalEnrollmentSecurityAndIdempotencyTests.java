package com.MOOC.OnlineLearningPlatfrom.commerce;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Royalty;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PaymentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoyaltyRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class InternalEnrollmentSecurityAndIdempotencyTests extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private RoyaltyRepository royaltyRepository;

    @Value("${internal.api.key}")
    private String internalApiKey;

    private UserAccount teacher;
    private UserAccount student;
    private Course paidCourse;

    @BeforeEach
    void setUp() {
        teacher = findOrCreateUser("teacher.commerce@learnsphere.test", "Commerce Teacher");
        student = findOrCreateUser("student.commerce@learnsphere.test", "Commerce Student");

        // Clean enrollments for student
        enrollmentRepository.findByUserId(student.getUserId()).forEach(enrollmentRepository::delete);
        paymentRepository.deleteAll();
        royaltyRepository.deleteAll();

        paidCourse = new Course();
        paidCourse.setTitle("Advanced Distributed Systems " + System.currentTimeMillis());
        paidCourse.setStatus(Course.Status.LIVE);
        paidCourse.setIsPublished(true);
        paidCourse.setPrice(new BigDecimal("1000.00"));
        paidCourse.setTeacher(teacher);
        paidCourse = courseRepository.save(paidCourse);
    }

    private UserAccount findOrCreateUser(String email, String fullName) {
        UserAccount existing = userAccountRepository.findByEmail(email);
        if (existing != null) {
            existing.setRoyaltyBalance(BigDecimal.ZERO);
            return userAccountRepository.save(existing);
        }
        UserAccount u = new UserAccount();
        u.setEmail(email);
        u.setFullName(fullName);
        u.setPassword("hash");
        u.setRoyaltyBalance(BigDecimal.ZERO);
        return userAccountRepository.save(u);
    }

    @Test
    @DisplayName("Missing X-Internal-Key or X-Service-Id returns 401 Unauthorized")
    void missingKeyOrServiceIdReturns401() throws Exception {
        // No headers
        mockMvc.perform(post("/internal/enroll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + student.getUserId() + ", \"courseId\": " + paidCourse.getId() + "}"))
                .andExpect(status().isUnauthorized());

        // Key only, missing X-Service-Id
        mockMvc.perform(post("/internal/enroll")
                        .header("X-Internal-Key", internalApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": " + student.getUserId() + ", \"courseId\": " + paidCourse.getId() + "}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Non-live course returns 400 Bad Request")
    void nonLiveCourseReturns400() throws Exception {
        Course draftCourse = new Course();
        draftCourse.setTitle("Draft Cloud");
        draftCourse.setStatus(Course.Status.DRAFT);
        draftCourse.setPrice(new BigDecimal("500.00"));
        draftCourse.setTeacher(teacher);
        courseRepository.save(draftCourse);

        mockMvc.perform(post("/internal/enroll")
                        .header("X-Internal-Key", internalApiKey)
                        .header("X-Service-Id", "payments-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"userId\": " + student.getUserId() + ","
                                + "\"courseId\": " + draftCourse.getId() + ","
                                + "\"orderId\": \"order_draft_1\","
                                + "\"paymentId\": \"pay_draft_1\""
                                + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Missing gateway paymentId returns 400 Bad Request")
    void missingPaymentIdReturns400() throws Exception {
        mockMvc.perform(post("/internal/enroll")
                        .header("X-Internal-Key", internalApiKey)
                        .header("X-Service-Id", "payments-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"userId\": " + student.getUserId() + ","
                                + "\"courseId\": " + paidCourse.getId() + ","
                                + "\"orderId\": \"order_123\""
                                + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Server recomputes course price and ignores tampered amount in request payload")
    void recomputesCoursePriceAndIgnoresTamperedAmount() throws Exception {
        // Course price is 1000.00. Tampered request sends amount = 1.00
        mockMvc.perform(post("/internal/enroll")
                        .header("X-Internal-Key", internalApiKey)
                        .header("X-Service-Id", "payments-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"
                                + "\"userId\": " + student.getUserId() + ","
                                + "\"courseId\": " + paidCourse.getId() + ","
                                + "\"orderId\": \"order_rzp_server_price\","
                                + "\"paymentId\": \"pay_rzp_server_price\","
                                + "\"amount\": 1.00"
                                + "}"))
                .andExpect(status().isOk());

        // Payment amount must be 1000.00 (from DB), NOT 1.00
        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));

        // Royalty must be 700.00 (70% of 1000.00), NOT 0.70
        List<Royalty> royalties = royaltyRepository.findAll();
        assertThat(royalties).hasSize(1);
        assertThat(royalties.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("700.00"));

        // Teacher balance updated to 700.00
        UserAccount updatedTeacher = userAccountRepository.findById(teacher.getUserId()).orElseThrow();
        assertThat(updatedTeacher.getRoyaltyBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
    }

    @Test
    @DisplayName("Replaying /internal/enroll with same paymentId is idempotent and does not mint duplicate payments or royalties")
    void replayingWithSamePaymentIdIsIdempotent() throws Exception {
        String payload = "{"
                + "\"userId\": " + student.getUserId() + ","
                + "\"courseId\": " + paidCourse.getId() + ","
                + "\"orderId\": \"order_rzp_replay\","
                + "\"paymentId\": \"pay_rzp_replay\""
                + "}";

        // First call
        mockMvc.perform(post("/internal/enroll")
                        .header("X-Internal-Key", internalApiKey)
                        .header("X-Service-Id", "payments-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        assertThat(paymentRepository.findByGatewayPaymentId("pay_rzp_replay")).isPresent();
        assertThat(royaltyRepository.findByTeacher_UserIdOrderByCreatedAtDesc(teacher.getUserId())).hasSize(1);
        assertThat(enrollmentRepository.findByUserId(student.getUserId())).hasSize(1);

        // Second call (replay)
        mockMvc.perform(post("/internal/enroll")
                        .header("X-Internal-Key", internalApiKey)
                        .header("X-Service-Id", "payments-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        // Counts must still be exactly 1
        assertThat(paymentRepository.findAll().stream().filter(p -> "pay_rzp_replay".equals(p.getGatewayPaymentId())).count()).isEqualTo(1);
        assertThat(royaltyRepository.findByTeacher_UserIdOrderByCreatedAtDesc(teacher.getUserId())).hasSize(1);
        assertThat(enrollmentRepository.findByUserId(student.getUserId())).hasSize(1);

        UserAccount updatedTeacher = userAccountRepository.findById(teacher.getUserId()).orElseThrow();
        assertThat(updatedTeacher.getRoyaltyBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
    }
}

