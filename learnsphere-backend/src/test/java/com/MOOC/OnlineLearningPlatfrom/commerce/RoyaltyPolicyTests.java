package com.MOOC.OnlineLearningPlatfrom.commerce;

import com.MOOC.OnlineLearningPlatfrom.Dto.RevenueSummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PaymentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.RevenueService;
import com.MOOC.OnlineLearningPlatfrom.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story W6-08: 70/30 policy object
 * Rules: AC-21, audit.md B10, [F-F11], spec §6
 */
@SpringBootTest
@ActiveProfiles("test")
public class RoyaltyPolicyTests extends PostgresIntegrationTest {

    @Autowired
    private RoyaltyPolicy royaltyPolicy;

    @Autowired
    private RevenueService revenueService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    private UserAccount teacher;
    private UserAccount student;
    private Course course;

    @BeforeEach
    void setUp() {
        teacher = findOrCreateUser("policy.teacher@learnsphere.test", "Policy Teacher");
        student = findOrCreateUser("policy.student@learnsphere.test", "Policy Student");

        paymentRepository.findByUser_UserIdOrderByCreatedAtDesc(student.getUserId()).forEach(paymentRepository::delete);

        course = new Course();
        course.setTitle("Policy Test Course " + System.currentTimeMillis());
        course.setStatus(Course.Status.LIVE);
        course.setIsPublished(true);
        course.setPrice(new BigDecimal("1000.00"));
        course.setTeacher(teacher);
        course = courseRepository.save(course);
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
        u.setPassword("hashedpassword");
        u.setRoyaltyBalance(BigDecimal.ZERO);
        u.setStatus(UserAccount.Status.ACTIVE);
        return userAccountRepository.save(u);
    }

    @Test
    @DisplayName("RoyaltyPolicy computes 70% instructor royalty and 30% platform earnings with half-up rounding")
    void royaltyPolicyComputesSeventyThirtySplit() {
        assertThat(royaltyPolicy.getTeacherPercentage()).isEqualTo(70);
        assertThat(royaltyPolicy.getPlatformPercentage()).isEqualTo(30);
        assertThat(royaltyPolicy.getTeacherShareRate()).isEqualByComparingTo(new BigDecimal("0.70"));
        assertThat(royaltyPolicy.getPlatformShareRate()).isEqualByComparingTo(new BigDecimal("0.30"));

        BigDecimal gross = new BigDecimal("1499.00");
        BigDecimal teacherCut = royaltyPolicy.calculateTeacherRoyalty(gross);
        BigDecimal platformCut = royaltyPolicy.calculatePlatformEarnings(gross);

        // 1499 * 0.70 = 1049.30; 1499 - 1049.30 = 449.70
        assertThat(teacherCut).isEqualByComparingTo(new BigDecimal("1049.30"));
        assertThat(platformCut).isEqualByComparingTo(new BigDecimal("449.70"));
        assertThat(teacherCut.add(platformCut)).isEqualByComparingTo(gross);

        // Edge amounts
        assertThat(royaltyPolicy.calculateTeacherRoyalty(BigDecimal.ZERO)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(royaltyPolicy.calculateTeacherRoyalty(null)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(royaltyPolicy.calculatePlatformEarnings(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("RevenueService derives platform and teacher revenue using shared RoyaltyPolicy")
    void revenueServiceUsesRoyaltyPolicy() {
        Payment payment = new Payment();
        payment.setUser(student);
        payment.setCourse(course);
        payment.setAmount(new BigDecimal("1000.00"));
        payment.setStatus(Payment.Status.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setGatewayOrderId("order_pol_1");
        payment.setGatewayPaymentId("pay_pol_1");
        paymentRepository.save(payment);

        RevenueSummaryDto summary = revenueService.getRevenueSummary();

        assertThat(summary.getTeacherRoyaltyPercent()).isEqualTo(royaltyPolicy.getTeacherPercentage());
        assertThat(summary.getPlatformCutPercent()).isEqualTo(royaltyPolicy.getPlatformPercentage());
        assertThat(summary.getTeacherRoyalties()).isEqualByComparingTo(new BigDecimal("700.00"));
        assertThat(summary.getPlatformEarnings()).isEqualByComparingTo(new BigDecimal("300.00"));
    }
}
