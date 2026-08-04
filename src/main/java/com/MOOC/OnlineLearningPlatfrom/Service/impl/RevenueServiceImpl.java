package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.CourseRevenueDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.MonthlyRevenueDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.RevenueSummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;
import com.MOOC.OnlineLearningPlatfrom.Repository.PaymentRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.RevenueService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Real revenue breakdown computed from SUCCESS Payment rows.
 * Platform/teacher split mirrors the real 70% teacher royalty share used at checkout time
 * (see PaymentServiceImpl.TEACHER_ROYALTY_SHARE).
 */
@Service
public class RevenueServiceImpl implements RevenueService {

    private static final BigDecimal TEACHER_ROYALTY_SHARE = new BigDecimal("0.70");
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy");

    private final PaymentRepository paymentRepository;

    public RevenueServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public RevenueSummaryDto getRevenueSummary() {
        List<Payment> successfulPayments = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == Payment.Status.SUCCESS && p.getAmount() != null)
                .toList();

        BigDecimal totalRevenue = successfulPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal teacherRoyalties = totalRevenue.multiply(TEACHER_ROYALTY_SHARE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformEarnings = totalRevenue.subtract(teacherRoyalties).setScale(2, RoundingMode.HALF_UP);

        List<CourseRevenueDto> byCourse = buildByCourse(successfulPayments);
        List<MonthlyRevenueDto> byMonth = buildByMonth(successfulPayments);

        return new RevenueSummaryDto(totalRevenue, platformEarnings, teacherRoyalties, 30, 70, byCourse, byMonth);
    }

    private List<CourseRevenueDto> buildByCourse(List<Payment> payments) {
        Map<Long, List<Payment>> byCourse = payments.stream()
                .filter(p -> p.getCourse() != null)
                .collect(Collectors.groupingBy(p -> p.getCourse().getId()));

        List<CourseRevenueDto> result = new ArrayList<>();
        for (List<Payment> coursePayments : byCourse.values()) {
            Course course = coursePayments.get(0).getCourse();
            BigDecimal amount = coursePayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            String teacherName = course.getTeacher() != null ? course.getTeacher().getFullName() : "Unknown";
            result.add(new CourseRevenueDto(course.getId(), course.getTitle(), teacherName, amount, coursePayments.size()));
        }

        result.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return result.size() > 10 ? result.subList(0, 10) : result;
    }

    private List<MonthlyRevenueDto> buildByMonth(List<Payment> payments) {
        Map<String, BigDecimal> byMonth = new TreeMap<>();
        Map<String, String> labels = new LinkedHashMap<>();
        for (Payment payment : payments) {
            if (payment.getPaidAt() == null) continue;
            String key = payment.getPaidAt().format(MONTH_KEY);
            byMonth.merge(key, payment.getAmount(), BigDecimal::add);
            labels.putIfAbsent(key, payment.getPaidAt().format(MONTH_LABEL));
        }

        List<MonthlyRevenueDto> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : byMonth.entrySet()) {
            result.add(new MonthlyRevenueDto(labels.get(entry.getKey()), entry.getValue()));
        }
        return result;
    }
}
