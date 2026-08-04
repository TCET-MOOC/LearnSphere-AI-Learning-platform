package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.PaymentOrderDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.PaymentResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Enrollment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Royalty;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PaymentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoyaltyRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.PaymentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    /**
     * Teacher's cut of every paid enrollment. The remaining 30% is retained by the
     * platform (hosting, payment processing, discovery/marketing). This is a simplification
     * for this project — a real platform would likely vary the split by contract/college deal.
     */
    private static final BigDecimal TEACHER_ROYALTY_SHARE = new BigDecimal("0.70");
    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final PaymentRepository paymentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final RoyaltyRepository royaltyRepository;
    private final UserAccountRepository userAccountRepository;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                               CourseRepository courseRepository,
                               EnrollmentRepository enrollmentRepository,
                               RoyaltyRepository royaltyRepository,
                               UserAccountRepository userAccountRepository) {
        this.paymentRepository = paymentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.royaltyRepository = royaltyRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional
    public PaymentOrderDto checkout(Long courseId, CustomUserDetails principal) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        Long userId = principal.getUser().getUserId();

        if (course.getPrice() == null || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("This course is free — use the direct enroll endpoint instead of checkout.");
        }
        if (enrollmentRepository.existsByUserIdAndCourse_Id(userId, courseId)) {
            throw new BadRequestException("You are already enrolled in this course.");
        }

        Payment payment = new Payment();
        payment.setUser(principal.getUser());
        payment.setCourse(course);
        payment.setAmount(course.getPrice());
        payment.setCurrency("INR");
        payment.setStatus(Payment.Status.PENDING);
        payment.setGatewayOrderId("order_" + UUID.randomUUID());
        paymentRepository.save(payment);

        return new PaymentOrderDto(payment.getGatewayOrderId(), payment.getAmount(), payment.getCurrency(), courseId);
    }

    @Override
    @Transactional
    public PaymentResponseDto verify(String orderId, CustomUserDetails principal) {
        Long userId = principal.getUser().getUserId();
        Payment payment = paymentRepository.findByGatewayOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("No pending payment order found for: " + orderId));

        boolean belongsToUser = payment.getUser() != null && payment.getUser().getUserId().equals(userId);
        if (!belongsToUser || payment.getStatus() != Payment.Status.PENDING) {
            throw new ResourceNotFoundException("No pending payment order found for: " + orderId);
        }

        payment.setStatus(Payment.Status.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        payment.setGatewayPaymentId("pay_" + UUID.randomUUID());
        paymentRepository.save(payment);

        Course course = payment.getCourse();
        if (course != null && !enrollmentRepository.existsByUserIdAndCourse_Id(userId, course.getId())) {
            Enrollment enrollment = new Enrollment();
            enrollment.setUserId(userId);
            enrollment.setCourse(course);
            enrollment.setRole("STUDENT");
            enrollment.setEnrolledAt(LocalDateTime.now());
            enrollmentRepository.save(enrollment);
        }

        if (course != null && course.getTeacher() != null) {
            creditTeacherRoyalty(course, payment.getAmount());
        }

        return PaymentResponseDto.from(payment);
    }

    private void creditTeacherRoyalty(Course course, BigDecimal paymentAmount) {
        UserAccount teacher = course.getTeacher();
        BigDecimal royaltyAmount = paymentAmount.multiply(TEACHER_ROYALTY_SHARE).setScale(2, RoundingMode.HALF_UP);

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

    @Override
    public List<PaymentResponseDto> getHistory(CustomUserDetails principal) {
        return paymentRepository.findByUser_UserIdOrderByCreatedAtDesc(principal.getUser().getUserId()).stream()
                .map(PaymentResponseDto::from)
                .toList();
    }

    @Override
    public boolean hasPaid(Long courseId, CustomUserDetails principal) {
        return paymentRepository.findByUser_UserIdAndCourse_IdAndStatus(
                principal.getUser().getUserId(), courseId, Payment.Status.SUCCESS).isPresent();
    }
}
