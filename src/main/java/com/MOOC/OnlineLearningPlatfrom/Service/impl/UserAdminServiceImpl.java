package com.MOOC.OnlineLearningPlatfrom.Service.impl;

import com.MOOC.OnlineLearningPlatfrom.Dto.TopTeacherDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserActivitySummaryDto;
import com.MOOC.OnlineLearningPlatfrom.Dto.UserAdminDto;
import com.MOOC.OnlineLearningPlatfrom.Entity.Course;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;
import com.MOOC.OnlineLearningPlatfrom.Entity.UserRole;
import com.MOOC.OnlineLearningPlatfrom.Exception.BadRequestException;
import com.MOOC.OnlineLearningPlatfrom.Exception.ResourceNotFoundException;
import com.MOOC.OnlineLearningPlatfrom.Repository.CourseRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.EnrollmentRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserAccountRepository;
import com.MOOC.OnlineLearningPlatfrom.Repository.UserRoleRepository;
import com.MOOC.OnlineLearningPlatfrom.Service.UserAdminService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public UserAdminServiceImpl(UserAccountRepository userAccountRepository,
                                 UserRoleRepository userRoleRepository,
                                 CourseRepository courseRepository,
                                 EnrollmentRepository enrollmentRepository) {
        this.userAccountRepository = userAccountRepository;
        this.userRoleRepository = userRoleRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    private Map<Long, List<String>> rolesByUserId() {
        Map<Long, List<String>> map = new HashMap<>();
        for (UserRole ur : userRoleRepository.findAll()) {
            if (ur.getUser() == null || ur.getRole() == null) continue;
            map.computeIfAbsent(ur.getUser().getUserId(), k -> new java.util.ArrayList<>()).add(ur.getRole().getName());
        }
        return map;
    }

    @Override
    public List<UserAdminDto> getUsers(String role, String status, String search) {
        Map<Long, List<String>> roleMap = rolesByUserId();
        String roleFilter = role != null && !role.isBlank() ? role.trim().toUpperCase() : null;
        String statusFilter = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
        String searchFilter = search != null && !search.isBlank() ? search.trim().toLowerCase() : null;

        return userAccountRepository.findAll().stream()
                .filter(u -> roleFilter == null || roleMap.getOrDefault(u.getUserId(), List.of()).contains(roleFilter))
                .filter(u -> statusFilter == null || (u.getStatus() != null && u.getStatus().name().equals(statusFilter)))
                .filter(u -> searchFilter == null
                        || (u.getFullName() != null && u.getFullName().toLowerCase().contains(searchFilter))
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(searchFilter)))
                .map(u -> UserAdminDto.from(u, roleMap.get(u.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public UserAdminDto updateStatus(Long userId, String status) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        UserAccount.Status newStatus;
        try {
            newStatus = UserAccount.Status.valueOf(status.trim().toUpperCase());
        } catch (Exception e) {
            throw new BadRequestException("status must be one of ACTIVE, FLAGGED, BLACKLISTED");
        }
        user.setStatus(newStatus);
        userAccountRepository.save(user);
        List<String> roles = rolesByUserId().get(user.getUserId());
        return UserAdminDto.from(user, roles);
    }

    @Override
    public UserActivitySummaryDto getActivitySummary() {
        List<UserAccount> all = userAccountRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dayAgo = now.minusHours(24);
        LocalDateTime weekAgo = now.minusDays(7);
        LocalDateTime monthAgo = now.minusDays(30);

        long daily = all.stream().filter(u -> u.getLastActiveAt() != null && u.getLastActiveAt().isAfter(dayAgo)).count();
        long weekly = all.stream().filter(u -> u.getLastActiveAt() != null && u.getLastActiveAt().isAfter(weekAgo)).count();
        long inactive30 = all.stream().filter(u -> u.getLastActiveAt() == null || u.getLastActiveAt().isBefore(monthAgo)).count();

        return new UserActivitySummaryDto(all.size(), daily, weekly, inactive30);
    }

    @Override
    public List<TopTeacherDto> getTopTeachers(int limit) {
        Map<Long, List<String>> roleMap = rolesByUserId();
        List<UserAccount> teachers = userAccountRepository.findAll().stream()
                .filter(u -> roleMap.getOrDefault(u.getUserId(), List.of()).contains("TEACHER"))
                .toList();

        return teachers.stream()
                .map(t -> {
                    List<Course> courses = courseRepository.findByTeacher_UserId(t.getUserId());
                    long students = courses.stream().mapToLong(c -> enrollmentRepository.countByCourse_Id(c.getId())).sum();
                    BigDecimal earnings = t.getRoyaltyBalance() != null ? t.getRoyaltyBalance() : BigDecimal.ZERO;
                    return new TopTeacherDto(t.getUserId(), t.getFullName(), students, earnings);
                })
                .sorted(Comparator.comparingLong(TopTeacherDto::getStudents).reversed()
                        .thenComparing(Comparator.comparing(TopTeacherDto::getEarnings).reversed()))
                .limit(Math.max(limit, 1))
                .collect(Collectors.toList());
    }
}
