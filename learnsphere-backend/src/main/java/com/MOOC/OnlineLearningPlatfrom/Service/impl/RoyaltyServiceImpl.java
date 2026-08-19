package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.PayoutResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.RoyaltyBreakdownDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.RoyaltySourceSplitDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.RoyaltySummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.TeacherRoyaltiesResponseDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Royalty;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PaymentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.PayoutRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.RoyaltyRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Security.CustomUserDetails;
import com.MOOC.OnlineLearningPlatfrom.Service.RoyaltyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Service
public class RoyaltyServiceImpl implements RoyaltyService {

    private static final DateTimeFormatter PERIOD_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final RoyaltyRepository royaltyRepository;
    private final PayoutRepository payoutRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final UserAccountRepository userAccountRepository;

    public RoyaltyServiceImpl(RoyaltyRepository royaltyRepository,
                               PayoutRepository payoutRepository,
                               CourseRepository courseRepository,
                               EnrollmentRepository enrollmentRepository,
                               PaymentRepository paymentRepository,
                               UserAccountRepository userAccountRepository) {
        this.royaltyRepository = royaltyRepository;
        this.payoutRepository = payoutRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.paymentRepository = paymentRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional
    public TeacherRoyaltiesResponseDto getRoyalties(CustomUserDetails principal) {
        Long teacherId = principal.getUser().getUserId();
        UserAccount teacher = userAccountRepository.findById(teacherId).orElse(principal.getUser());
        List<Course> courses = courseRepository.findByTeacher_UserId(teacherId);
        if (courses.isEmpty()) {
            String teacherEmail = teacher.getEmail();
            if (teacherEmail != null && !teacherEmail.isBlank()) {
                courses = courseRepository.findAll().stream()
                        .filter(c -> c.getTeacher() != null && teacherEmail.equalsIgnoreCase(c.getTeacher().getEmail()))
                        .toList();
            }
        }
        if (courses.isEmpty() && principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"))) {
            courses = courseRepository.findAll();
        }

        // Auto-reconcile existing enrollments with royalties
        String currentPeriod = LocalDateTime.now().format(PERIOD_FORMAT);
        for (Course course : courses) {
            if (course.getPrice() != null && course.getPrice().compareTo(BigDecimal.ZERO) > 0) {
                long paidEnrollments = enrollmentRepository.countByCourse_Id(course.getId());
                long existingRoyaltiesCount = royaltyRepository.findByTeacher_UserIdAndCourse_Id(teacherId, course.getId()).size();
                long missingCount = paidEnrollments - existingRoyaltiesCount;
                if (missingCount > 0) {
                    BigDecimal royaltyPerEnrollment = course.getPrice().multiply(new BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP);
                    for (int i = 0; i < missingCount; i++) {
                        Royalty r = new Royalty();
                        r.setTeacher(teacher);
                        r.setCourse(course);
                        r.setPeriod(currentPeriod);
                        r.setSource(Royalty.Source.EXTERNAL_SALES);
                        r.setAmount(royaltyPerEnrollment);
                        royaltyRepository.save(r);
                    }
                    BigDecimal currentBalance = teacher.getRoyaltyBalance() != null ? teacher.getRoyaltyBalance() : BigDecimal.ZERO;
                    BigDecimal addedTotal = royaltyPerEnrollment.multiply(BigDecimal.valueOf(missingCount));
                    teacher.setRoyaltyBalance(currentBalance.add(addedTotal));
                    userAccountRepository.save(teacher);
                }
            }
        }

        List<Royalty> royalties = royaltyRepository.findByTeacher_UserIdOrderByCreatedAtDesc(teacherId);

        BigDecimal totalEarned = royalties.stream().map(Royalty::getAmount).filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal thisMonthTotal = royalties.stream()
                .filter(r -> currentPeriod.equals(r.getPeriod()))
                .map(Royalty::getAmount).filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingPayout = teacher.getRoyaltyBalance() != null
                ? teacher.getRoyaltyBalance() : BigDecimal.ZERO;

        long externalEnrollments = courses.stream()
                .mapToLong(c -> {
                    long payCount = paymentRepository.countByCourse_IdAndStatus(c.getId(), Payment.Status.SUCCESS);
                    long enrollCount = enrollmentRepository.countByCourse_Id(c.getId());
                    return Math.max(payCount, enrollCount);
                })
                .sum();

        List<RoyaltyBreakdownDto> byCourse = courses.stream().map(course -> {
            long enrolledCount = enrollmentRepository.countByCourse_Id(course.getId());
            long externalPaidCount = Math.max(paymentRepository.countByCourse_IdAndStatus(course.getId(), Payment.Status.SUCCESS), enrolledCount);
            BigDecimal courseAmount = royaltyRepository.findByTeacher_UserIdAndCourse_Id(teacherId, course.getId())
                    .stream().map(Royalty::getAmount).filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            boolean isDraft = course.getStatus() == Course.Status.DRAFT;
            String status = isDraft ? "Draft — not yet live"
                    : (courseAmount.compareTo(BigDecimal.ZERO) > 0 ? "this month" : "No sales yet");
            return new RoyaltyBreakdownDto(course.getId(), course.getTitle(), enrolledCount, externalPaidCount,
                    courseAmount, status, isDraft);
        }).toList();

        List<RoyaltySourceSplitDto> sourceBreakdown = Arrays.stream(Royalty.Source.values()).map(source -> {
            BigDecimal sourceAmount = royalties.stream()
                    .filter(r -> r.getSource() == source)
                    .map(Royalty::getAmount).filter(a -> a != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            double percentage = totalEarned.compareTo(BigDecimal.ZERO) > 0
                    ? sourceAmount.multiply(BigDecimal.valueOf(100))
                        .divide(totalEarned, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            return new RoyaltySourceSplitDto(source.name(), labelFor(source), sourceAmount, percentage);
        }).toList();

        RoyaltySummaryDto summary = new RoyaltySummaryDto(thisMonthTotal, totalEarned, pendingPayout, externalEnrollments);
        return new TeacherRoyaltiesResponseDto(summary, byCourse, sourceBreakdown);
    }

    private String labelFor(Royalty.Source source) {
        return switch (source) {
            case EXTERNAL_SALES -> "External sales";
            case COLLEGE_SHARE -> "College share";
            case REMEDIAL_CERTS -> "Remedial certs";
        };
    }

    @Override
    public List<PayoutResponseDto> getPayouts(CustomUserDetails principal) {
        return payoutRepository.findByTeacher_UserIdOrderByCreatedAtDesc(principal.getUser().getUserId()).stream()
                .map(PayoutResponseDto::from)
                .toList();
    }
}
