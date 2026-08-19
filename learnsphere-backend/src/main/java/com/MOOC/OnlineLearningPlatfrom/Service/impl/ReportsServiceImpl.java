package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.ReportsSummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.FlaggedContent;
import com.MOOC.OnlineLearningPlatfrom.Entity.Payment;
import com.MOOC.OnlineLearningPlatfrom.Entity.Role;
import com.MOOC.OnlineLearningPlatfrom.Repository.*;
import com.MOOC.OnlineLearningPlatfrom.Service.ReportsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportsServiceImpl implements ReportsService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CourseRepository courseRepository;
    private final PaymentRepository paymentRepository;
    private final CertificateRepository certificateRepository;
    private final FlaggedContentRepository flaggedContentRepository;

    public ReportsServiceImpl(UserAccountRepository userAccountRepository, RoleRepository roleRepository,
                               UserRoleRepository userRoleRepository, CourseRepository courseRepository,
                               PaymentRepository paymentRepository, CertificateRepository certificateRepository,
                               FlaggedContentRepository flaggedContentRepository) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.courseRepository = courseRepository;
        this.paymentRepository = paymentRepository;
        this.certificateRepository = certificateRepository;
        this.flaggedContentRepository = flaggedContentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ReportsSummaryDto getSummary() {
        long totalUsers = userAccountRepository.count();

        Map<String, Long> usersByRole = new LinkedHashMap<>();
        List<Role> roles = roleRepository.findAll();
        for (Role role : roles) {
            usersByRole.put(role.getName(), userRoleRepository.countByRole_Name(role.getName()));
        }

        List<Course> courses = courseRepository.findAll();
        long totalCourses = courses.size();
        Map<String, Long> coursesByStatus = new LinkedHashMap<>();
        for (Course.Status status : Course.Status.values()) {
            long count = courses.stream().filter(c -> c.getStatus() == status).count();
            coursesByStatus.put(status.name(), count);
        }

        // Use a targeted SUM query instead of findAll() to avoid holding table-level locks
        // that conflict with the payments microservice database connection.
        BigDecimal totalRevenue = paymentRepository.sumAmountByStatus(Payment.Status.SUCCESS);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        long totalCertificates = certificateRepository.count();
        long flaggedPending = flaggedContentRepository.countByStatus(FlaggedContent.Status.PENDING);

        return new ReportsSummaryDto(totalUsers, usersByRole, totalCourses, coursesByStatus,
                totalRevenue, totalCertificates, flaggedPending);
    }
}
