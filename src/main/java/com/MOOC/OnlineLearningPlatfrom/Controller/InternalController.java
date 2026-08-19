package com.MOOC.OnlineLearningPlatfrom.Controller;

import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Royalty;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PaymentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoyaltyRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.EnrollmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
public class InternalController {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentService enrollmentService;
    private final PaymentRepository paymentRepository;
    private final RoyaltyRepository royaltyRepository;
    private final UserAccountRepository userAccountRepository;

    private static final BigDecimal TEACHER_ROYALTY_SHARE = new BigDecimal("0.70");
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Value("${internal.api.key}")
    private String expectedApiKey;

    public InternalController(EnrollmentRepository enrollmentRepository,
                              CourseRepository courseRepository,
                              EnrollmentService enrollmentService,
                              PaymentRepository paymentRepository,
                              RoyaltyRepository royaltyRepository,
                              UserAccountRepository userAccountRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentService = enrollmentService;
        this.paymentRepository = paymentRepository;
        this.royaltyRepository = royaltyRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @PostMapping("/enroll")
    @Transactional
    public ResponseEntity<?> enrollStudent(
            @RequestHeader(value = "X-Internal-Key", required = false) String apiKey,
            @RequestBody Map<String, Object> payload) {
        
        if (expectedApiKey == null || !expectedApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        Long userId = null;
        Long courseId = null;

        try {
            userId = Long.valueOf(payload.get("userId").toString());
            courseId = Long.valueOf(payload.get("courseId").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid userId or courseId");
        }

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Course not found");
        }

        UserAccount student = userAccountRepository.findById(userId).orElse(null);

        // 1. Enroll user if not already enrolled
        if (!enrollmentRepository.existsByUserIdAndCourse_Id(userId, courseId)) {
            Enrollment newEnrollment = new Enrollment();
            newEnrollment.setUserId(userId);
            newEnrollment.setCourse(course);
            newEnrollment.setRole("STUDENT");
            enrollmentService.enrollUser(newEnrollment);
        }

        // 2. Parse price/amount paid
        BigDecimal paidAmount = course.getPrice();
        if (payload.get("amount") != null) {
            try {
                paidAmount = new BigDecimal(payload.get("amount").toString());
            } catch (Exception ignored) {}
        }

        // 3. Record Payment transaction in database
        Payment payment = new Payment();
        payment.setUser(student);
        payment.setCourse(course);
        payment.setAmount(paidAmount != null ? paidAmount : BigDecimal.ZERO);
        payment.setCurrency("INR");
        payment.setStatus(Payment.Status.SUCCESS);
        payment.setGatewayOrderId(payload.getOrDefault("orderId", "order_" + UUID.randomUUID()).toString());
        payment.setGatewayPaymentId(payload.getOrDefault("paymentId", "pay_" + UUID.randomUUID()).toString());
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 4. Credit 70% royalty to Course Teacher
        if (course.getTeacher() != null && paidAmount != null && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            UserAccount teacher = course.getTeacher();
            BigDecimal royaltyAmount = paidAmount.multiply(TEACHER_ROYALTY_SHARE).setScale(2, RoundingMode.HALF_UP);

            Royalty royalty = new Royalty();
            royalty.setTeacher(teacher);
            royalty.setCourse(course);
            royalty.setPeriod(LocalDateTime.now().format(PERIOD_FORMAT));
            royalty.setSource(Royalty.Source.EXTERNAL_SALES);
            royalty.setAmount(royaltyAmount);
            royaltyRepository.save(royalty);

            BigDecimal currentBalance = teacher.getRoyaltyBalance() != null ? teacher.getRoyaltyBalance() : BigDecimal.ZERO;
            teacher.setRoyaltyBalance(currentBalance.add(royaltyAmount));
            userAccountRepository.save(teacher);
        }

        return ResponseEntity.ok(Map.of("message", "User enrolled and teacher royalties credited successfully"));
    }
}
