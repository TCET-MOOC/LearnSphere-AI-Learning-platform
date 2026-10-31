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
import com.MOOC.OnlineLearningPlatfrom.commerce.RoyaltyPolicy;
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

@RestController
@RequestMapping("/internal")
public class InternalController {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentService enrollmentService;
    private final PaymentRepository paymentRepository;
    private final RoyaltyRepository royaltyRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoyaltyPolicy royaltyPolicy;

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Value("${internal.api.key}")
    private String expectedApiKey;

    public InternalController(EnrollmentRepository enrollmentRepository,
                              CourseRepository courseRepository,
                              EnrollmentService enrollmentService,
                              PaymentRepository paymentRepository,
                              RoyaltyRepository royaltyRepository,
                              UserAccountRepository userAccountRepository,
                              RoyaltyPolicy royaltyPolicy) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentService = enrollmentService;
        this.paymentRepository = paymentRepository;
        this.royaltyRepository = royaltyRepository;
        this.userAccountRepository = userAccountRepository;
        this.royaltyPolicy = royaltyPolicy;
    }

    @PostMapping("/enroll")
    @Transactional
    public ResponseEntity<?> enrollStudent(
            @RequestHeader(value = "X-Internal-Key", required = false) String apiKey,
            @RequestHeader(value = "X-Service-Id", required = false) String serviceId,
            @RequestBody(required = false) Map<String, Object> payload) {

        if (expectedApiKey == null || !expectedApiKey.equals(apiKey) || !"payments-service".equals(serviceId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized", "message", "Missing or invalid internal credentials"));
        }

        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", "Payload is required"));
        }

        Long userId;
        Long courseId;
        try {
            if (payload.get("userId") == null || payload.get("courseId") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", "userId and courseId are required"));
            }
            userId = Long.valueOf(payload.get("userId").toString());
            courseId = Long.valueOf(payload.get("courseId").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", "Invalid userId or courseId"));
        }

        Object paymentIdObj = payload.get("paymentId");
        if (paymentIdObj == null || paymentIdObj.toString().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", "paymentId is required and cannot be empty"));
        }
        String paymentId = paymentIdObj.toString().trim();

        Object orderIdObj = payload.get("orderId");
        if (orderIdObj == null || orderIdObj.toString().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", "orderId is required and cannot be empty"));
        }
        String orderId = orderIdObj.toString().trim();

        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not Found", "message", "Course not found"));
        }

        if (course.getStatus() != Course.Status.LIVE) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", "Course is not LIVE"));
        }

        if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Bad Request", "message", "Internal enrollment only supports paid courses"));
        }

        UserAccount student = userAccountRepository.findById(userId).orElse(null);

        // 1. Idempotency: check if paymentId already processed
        if (paymentRepository.findByGatewayPaymentId(paymentId).isPresent()) {
            if (!enrollmentRepository.existsByUserIdAndCourse_Id(userId, courseId)) {
                Enrollment newEnrollment = new Enrollment();
                newEnrollment.setUserId(userId);
                newEnrollment.setCourse(course);
                newEnrollment.setRole("STUDENT");
                enrollmentService.enrollUser(newEnrollment);
            }
            return ResponseEntity.ok(Map.of(
                    "status", "ALREADY_PROCESSED",
                    "message", "Payment already recorded and processed idempotently"
            ));
        }

        // 2. Compute price strictly from server-side course price (ignore body amount)
        BigDecimal paidAmount = course.getPrice();

        // 3. Record Payment transaction in database FIRST so that enrollment service recognizes successful payment
        Payment payment = new Payment();
        payment.setUser(student);
        payment.setCourse(course);
        payment.setAmount(paidAmount);
        payment.setCurrency("INR");
        payment.setStatus(Payment.Status.SUCCESS);
        payment.setGatewayOrderId(orderId);
        payment.setGatewayPaymentId(paymentId);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // 4. Enroll user if not already enrolled
        if (!enrollmentRepository.existsByUserIdAndCourse_Id(userId, courseId)) {
            Enrollment newEnrollment = new Enrollment();
            newEnrollment.setUserId(userId);
            newEnrollment.setCourse(course);
            newEnrollment.setRole("STUDENT");
            enrollmentService.enrollUser(newEnrollment);
        }

        // 5. Credit 70% royalty to Course Teacher
        if (course.getTeacher() != null && paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            UserAccount teacher = course.getTeacher();
            BigDecimal royaltyAmount = royaltyPolicy.calculateTeacherRoyalty(paidAmount);

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
