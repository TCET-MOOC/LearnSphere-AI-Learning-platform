package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.UserAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class UserResponseDto {
    private Long id;
    private String fullName;
    private String email;
    private String role;
    private Long collegeId;
    private String collegeName;
    private String collegeVerificationStatus;
    private Long departmentId;
    private String departmentName;
    private String avatarUrl;
    private String bio;
    private String status;
    private boolean twoFactorEnabled;
    private boolean bankAccountLinked;
    private BigDecimal royaltyBalance;
    private Integer attendanceScore;
    private Integer leaderboardPoints;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;

    public static UserResponseDto from(UserAccount user, List<String> roles) {
        UserResponseDto dto = new UserResponseDto();
        dto.id = user.getUserId();
        dto.fullName = user.getFullName();
        dto.email = user.getEmail();
        dto.role = roles.isEmpty() ? null : roles.get(0);
        if (user.getCollege() != null) {
            dto.collegeId = user.getCollege().getId();
            dto.collegeName = user.getCollege().getName();
            dto.collegeVerificationStatus = user.getCollege().getVerificationStatus() != null
                    ? user.getCollege().getVerificationStatus().name() : null;
        }
        if (user.getDepartment() != null) {
            dto.departmentId = user.getDepartment().getDepartmentId();
            dto.departmentName = user.getDepartment().getName();
        }
        dto.avatarUrl = user.getAvatarUrl();
        dto.bio = user.getBio();
        dto.status = user.getStatus() != null ? user.getStatus().name() : null;
        dto.twoFactorEnabled = Boolean.TRUE.equals(user.isTwoFactorEnabled());
        dto.bankAccountLinked = Boolean.TRUE.equals(user.isBankAccountLinked());
        dto.royaltyBalance = user.getRoyaltyBalance();
        dto.attendanceScore = user.getAttendanceScore();
        dto.leaderboardPoints = user.getLeaderboardPoints();
        dto.lastActiveAt = user.getLastActiveAt();
        dto.createdAt = user.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Long getCollegeId() { return collegeId; }
    public String getCollegeName() { return collegeName; }
    public String getCollegeVerificationStatus() { return collegeVerificationStatus; }
    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getBio() { return bio; }
    public String getStatus() { return status; }
    public boolean isTwoFactorEnabled() { return twoFactorEnabled; }
    public boolean isBankAccountLinked() { return bankAccountLinked; }
    public BigDecimal getRoyaltyBalance() { return royaltyBalance; }
    public Integer getAttendanceScore() { return attendanceScore; }
    public Integer getLeaderboardPoints() { return leaderboardPoints; }
    public LocalDateTime getLastActiveAt() { return lastActiveAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
